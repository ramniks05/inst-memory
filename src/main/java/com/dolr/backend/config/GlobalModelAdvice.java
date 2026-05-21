package com.dolr.backend.config;

import com.dolr.backend.security.AdminAuthHelper;
import com.dolr.backend.security.RoleCodes;
import com.dolr.backend.security.WebPaths;
import jakarta.servlet.http.HttpServletRequest;
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

	@ModelAttribute("navUserName")
	public String navUserName(HttpServletRequest request) {
		return adminAuthHelper.userFromRequest(request)
				.map(u -> u.getFullName() != null && !u.getFullName().isBlank() ? u.getFullName().trim() : u.getEmail())
				.orElse("");
	}

	@ModelAttribute("navUserDesignation")
	public String navUserDesignation(HttpServletRequest request) {
		return adminAuthHelper.userFromRequest(request).map(u -> {
			if (u.getDesignation() != null && !u.getDesignation().isBlank()) return u.getDesignation().trim();
			return RoleCodes.isPortalAdministrator(u) ? "Administrator" : "—";
		}).orElse("");
	}

	@ModelAttribute("navUserInitials")
	public String navUserInitials(HttpServletRequest request) {
		return adminAuthHelper.userFromRequest(request)
				.map(u -> {
					String name = u.getFullName() != null && !u.getFullName().isBlank() ? u.getFullName().trim() : u.getEmail();
					String[] parts = name.trim().split("\\s+");
					if (parts.length >= 2) {
						return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
					}
					return parts[0].length() >= 2 ? parts[0].substring(0, 2).toUpperCase() : parts[0].substring(0, 1).toUpperCase();
				}).orElse("?");
	}

	@ModelAttribute("navUserRole")
	public String navUserRole(HttpServletRequest request) {
		return adminAuthHelper.userFromRequest(request)
				.map(u -> RoleCodes.isPortalAdministrator(u) ? "Administrator" : "Officer")
				.orElse("");
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
		if (path.startsWith("/home/mpr")) {
			return "mpr";
		}
		if (path.startsWith("/home/change-password")) {
			return "change-password";
		}
		if (path.startsWith("/home/my-uploads")) {
			return "my-uploads";
		}
		if (path.startsWith("/home/documents")) {
			return "documents";
		}
		return "";
	}
}
