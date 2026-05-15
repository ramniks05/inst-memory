package com.dolr.backend.config;

import com.dolr.backend.security.AdminAuthHelper;
import com.dolr.backend.security.WebPaths;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAdvice {

	private final AdminAuthHelper adminAuthHelper;
	private final PortalPathSupport portalPathSupport;

	@ModelAttribute("portalAdministrator")
	public boolean portalAdministrator(HttpServletRequest request) {
		return adminAuthHelper.isPortalAdministrator(request);
	}

	@ModelAttribute("signedIn")
	public boolean signedIn(HttpServletRequest request) {
		return adminAuthHelper.userFromRequest(request).isPresent();
	}

	/** Servlet context path, e.g. {@code /institutionalmemory} — use for every navigation link on server. */
	@ModelAttribute("ctx")
	public String contextPath(HttpServletRequest request) {
		return portalPathSupport.linkPrefix(request);
	}

	/** Highlights the correct sidebar item from the current URL (works with context path on server). */
	@ModelAttribute("activeMenu")
	public String activeMenu(HttpServletRequest request) {
		String path = WebPaths.servletPath(request);
		if (path.startsWith("/admin/document-types")) {
			return "admin-document-types";
		}
		if (path.startsWith("/admin/designations")) {
			return "admin-designations";
		}
		if (path.startsWith("/admin/divisions")) {
			return "admin-divisions";
		}
		if (path.startsWith("/admin/officers") || path.contains("/admin/officers/")) {
			return "admin-officers";
		}
		if (path.startsWith("/admin/documents")) {
			return "admin-documents";
		}
		if ("/home".equals(path)) {
			return "dashboard";
		}
		if (path.startsWith("/home/documents")) {
			return "documents";
		}
		return "";
	}
}
