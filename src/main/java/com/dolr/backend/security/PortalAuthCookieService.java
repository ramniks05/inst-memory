package com.dolr.backend.security;

import com.dolr.backend.config.PortalPathSupport;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Signed HTTP-only cookie so navigation still works when the servlet {@code JSESSIONID}
 * cookie is not sent on the next request (common behind some proxies).
 */
@Service
public class PortalAuthCookieService {

	public static final String COOKIE_NAME = "PORTAL_UID";

	private final PortalPathSupport portalPathSupport;

	@Value("${jwt.secret:dolrSuperSecretKeyDolrSuperSecretKey123456}")
	private String secret;

	public PortalAuthCookieService(PortalPathSupport portalPathSupport) {
		this.portalPathSupport = portalPathSupport;
	}

	public void writeCookie(HttpServletRequest request, HttpServletResponse response, long userId) {
		Cookie cookie = new Cookie(COOKIE_NAME, sign(userId));
		cookie.setHttpOnly(true);
		cookie.setSecure(request.isSecure());
		cookie.setPath(cookiePath(request));
		cookie.setMaxAge(12 * 60 * 60);
		response.addCookie(cookie);
	}

	public void clearCookie(HttpServletRequest request, HttpServletResponse response) {
		Cookie cookie = new Cookie(COOKIE_NAME, "");
		cookie.setHttpOnly(true);
		cookie.setSecure(request.isSecure());
		cookie.setPath(cookiePath(request));
		cookie.setMaxAge(0);
		response.addCookie(cookie);
	}

	public Optional<Long> readUserId(HttpServletRequest request) {
		if (request.getCookies() == null) {
			return Optional.empty();
		}
		for (Cookie c : request.getCookies()) {
			if (COOKIE_NAME.equals(c.getName())) {
				return verifyAndExtractUserId(c.getValue());
			}
		}
		return Optional.empty();
	}

	private String cookiePath(HttpServletRequest request) {
		String prefix = portalPathSupport.linkPrefix(request);
		return prefix.isEmpty() ? "/" : prefix;
	}

	private String sign(long userId) {
		return userId + "." + hmac(String.valueOf(userId));
	}

	private Optional<Long> verifyAndExtractUserId(String raw) {
		if (raw == null || raw.isBlank()) {
			return Optional.empty();
		}
		int dot = raw.lastIndexOf('.');
		if (dot <= 0) {
			return Optional.empty();
		}
		String idPart = raw.substring(0, dot);
		String sigPart = raw.substring(dot + 1);
		try {
			long userId = Long.parseLong(idPart);
			String expected = hmac(idPart);
			if (!MessageDigest.isEqual(
					expected.getBytes(StandardCharsets.UTF_8),
					sigPart.getBytes(StandardCharsets.UTF_8))) {
				return Optional.empty();
			}
			return Optional.of(userId);
		} catch (NumberFormatException e) {
			return Optional.empty();
		}
	}

	private String hmac(String payload) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
			return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
		} catch (Exception e) {
			throw new IllegalStateException("Could not sign portal auth cookie", e);
		}
	}
}
