package com.dolr.backend.config;

import com.dolr.backend.security.AdminAuthHelper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAdvice {

	private final AdminAuthHelper adminAuthHelper;

	@ModelAttribute("portalAdministrator")
	public boolean portalAdministrator(HttpSession session) {
		return adminAuthHelper.isPortalAdministrator(session);
	}

	@ModelAttribute("signedIn")
	public boolean signedIn(HttpSession session) {
		return adminAuthHelper.userFromSession(session).isPresent();
	}

	/**
	 * When an administrator uses officer-layout URLs ({@code /home/...}), the admin sidebar
	 * still needs the correct {@code active} key for highlighting.
	 */
	@ModelAttribute("adminSidebarMenu")
	public String adminSidebarMenu(HttpSession session, HttpServletRequest request) {
		if (!adminAuthHelper.isPortalAdministrator(session)) {
			return "";
		}
		String uri = request.getRequestURI();
		String cp = request.getContextPath();
		if (cp != null && !cp.isEmpty() && !"/".equals(cp) && uri.startsWith(cp)) {
			uri = uri.substring(cp.length());
		}
		if (uri.startsWith("/home/documents")) {
			return "admin-documents";
		}
		if (uri.startsWith("/home/officers")) {
			return "admin-officers";
		}
		if ("/home".equals(uri)) {
			return "";
		}
		return "admin-documents";
	}
}
