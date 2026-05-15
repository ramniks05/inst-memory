package com.dolr.backend.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Builds link prefixes for Thymeleaf when the app is reached at
 * {@code https://host/institutionalmemory/...} but the servlet context path may be empty on the
 * server (nginx strips the prefix before proxy_pass). Locally, {@link HttpServletRequest#getContextPath()}
 * is usually enough.
 */
@Component
public class PortalPathSupport {

	@Value("${app.portal.public-path-prefix:/institutionalmemory}")
	private String configuredPublicPrefix;

	/** Prefix for {@code th:href="${ctx + '/admin/...'}"} — never null. */
	public String linkPrefix(HttpServletRequest request) {
		String servletContext = normalize(request.getContextPath());
		if (!servletContext.isEmpty()) {
			return servletContext;
		}
		String forwarded = forwardedPrefix(request);
		if (!forwarded.isEmpty()) {
			return forwarded;
		}
		return normalize(configuredPublicPrefix);
	}

	public String path(HttpServletRequest request, String servletRelativePath) {
		String prefix = linkPrefix(request);
		String path = servletRelativePath.startsWith("/") ? servletRelativePath : "/" + servletRelativePath;
		if (prefix.isEmpty()) {
			return path;
		}
		return prefix + path;
	}

	private static String forwardedPrefix(HttpServletRequest request) {
		String raw = request.getHeader("X-Forwarded-Prefix");
		if (raw == null || raw.isBlank()) {
			return "";
		}
		String first = raw.split(",")[0].trim();
		return normalize(first);
	}

	private static String normalize(String value) {
		if (value == null) {
			return "";
		}
		String t = value.trim();
		if (t.isEmpty() || "/".equals(t)) {
			return "";
		}
		return t.startsWith("/") ? t : "/" + t;
	}
}
