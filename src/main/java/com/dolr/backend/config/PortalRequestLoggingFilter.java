package com.dolr.backend.config;

import com.dolr.backend.security.PortalAuthCookieService;
import com.dolr.backend.security.WebSessionKeys;
import jakarta.servlet.FilterChain;
import lombok.RequiredArgsConstructor;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Logs portal page requests so server/nginx misrouting is visible in the log
 * (if menu clicks produce no line here, the browser never hit this application).
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@RequiredArgsConstructor
public class PortalRequestLoggingFilter extends OncePerRequestFilter {

	private final PortalPathSupport portalPathSupport;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String uri = request.getRequestURI();
		if (uri != null && (uri.contains("/home") || uri.contains("/admin") || uri.contains("/login"))) {
			HttpSession session = request.getSession(false);
			Object userId = session != null ? session.getAttribute(WebSessionKeys.USER_ID) : null;
			boolean hasPortalCookie = request.getCookies() != null
					&& java.util.Arrays.stream(request.getCookies())
					.anyMatch(c -> PortalAuthCookieService.COOKIE_NAME.equals(c.getName()));
			log.info("PORTAL {} {}{} servletCtx={} sessionId={} portalUserId={} portalCookie={} cookieHdr={}",
					request.getMethod(),
					uri,
					request.getQueryString() != null ? "?" + request.getQueryString() : "",
					request.getContextPath(),
					session != null ? session.getId() : "-",
					userId != null ? userId : "-",
					hasPortalCookie,
					request.getHeader("Cookie") != null ? "yes" : "no");
		}
		chain.doFilter(request, response);
	}
}
