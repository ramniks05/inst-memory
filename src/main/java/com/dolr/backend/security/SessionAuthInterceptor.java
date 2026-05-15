package com.dolr.backend.security;

import com.dolr.backend.config.PortalPathSupport;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Ensures signed-in portal pages always redirect to login with the correct context path
 * (important when the app is deployed under {@code /institutionalmemory} behind a proxy).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SessionAuthInterceptor implements HandlerInterceptor {

	private final AdminAuthHelper adminAuthHelper;
	private final PortalPathSupport portalPathSupport;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		adminAuthHelper.restoreSessionIfNeeded(request);
		if (!requiresAuth(request)) {
			return true;
		}
		if (adminAuthHelper.userFromRequest(request).isPresent()) {
			return true;
		}
		String loginUrl = portalPathSupport.path(request, "/login");
		log.warn("PORTAL auth required: uri={} cookies={} servletCtx={} -> {}",
				request.getRequestURI(), request.getHeader("Cookie"), request.getContextPath(), loginUrl);
		response.sendRedirect(loginUrl);
		return false;
	}

	private static boolean requiresAuth(HttpServletRequest request) {
		String path = request.getServletPath();
		if (path == null || path.isEmpty()) {
			path = request.getRequestURI();
			String ctx = request.getContextPath();
			if (ctx != null && !ctx.isEmpty() && path.startsWith(ctx)) {
				path = path.substring(ctx.length());
			}
		}
		return path.startsWith("/home") || path.startsWith("/admin");
	}
}
