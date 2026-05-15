package com.dolr.backend.security;

import jakarta.servlet.http.HttpServletRequest;

/** Servlet path helpers (context-path aware). */
public final class WebPaths {

	private WebPaths() {
	}

	public static String servletPath(HttpServletRequest request) {
		String path = request.getServletPath();
		if (path != null && !path.isEmpty()) {
			return path;
		}
		String uri = request.getRequestURI();
		String ctx = request.getContextPath();
		if (ctx != null && !ctx.isEmpty() && !"/".equals(ctx) && uri.startsWith(ctx)) {
			return uri.substring(ctx.length());
		}
		return uri;
	}
}
