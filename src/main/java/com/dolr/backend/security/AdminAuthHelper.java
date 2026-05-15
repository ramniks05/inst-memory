package com.dolr.backend.security;

import com.dolr.backend.entity.User;
import com.dolr.backend.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AdminAuthHelper {

	private final JwtService jwtService;
	private final UserRepository userRepository;
	private final PortalAuthCookieService portalAuthCookieService;

	/**
	 * Resolves the signed-in user. Restores the HTTP session from the signed {@code PORTAL_UID}
	 * cookie when {@code JSESSIONID} was not sent (fixes menu navigation on some servers).
	 */
	public Optional<User> userFromRequest(HttpServletRequest request) {
		if (request == null) {
			return Optional.empty();
		}
		restoreSessionIfNeeded(request);
		return userFromSession(request.getSession(false));
	}

	public void restoreSessionIfNeeded(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if (userFromSession(session).isPresent()) {
			return;
		}
		portalAuthCookieService.readUserId(request)
				.flatMap(userRepository::findByIdWithProfile)
				.ifPresent(user -> bindUserToSession(request.getSession(true), user));
	}

	public Optional<User> userFromSession(HttpSession session) {
		if (session == null) {
			return Optional.empty();
		}
		Object userIdAttr = session.getAttribute(WebSessionKeys.USER_ID);
		if (userIdAttr instanceof Long userId) {
			return userRepository.findByIdWithProfile(userId);
		}
		if (userIdAttr instanceof Number n) {
			return userRepository.findByIdWithProfile(n.longValue());
		}
		Object token = session.getAttribute("jwt");
		if (!(token instanceof String) || ((String) token).isBlank()) {
			return Optional.empty();
		}
		try {
			String email = jwtService.extractEmail(((String) token).trim());
			return userRepository.findByEmailIgnoreCaseWithProfile(email.trim().toLowerCase());
		} catch (Exception e) {
			return Optional.empty();
		}
	}

	public void bindUserToSession(HttpSession session, User user) {
		if (session == null || user == null || user.getId() == null) {
			return;
		}
		session.setAttribute(WebSessionKeys.USER_ID, user.getId());
	}

	public void bindUserAfterLogin(HttpServletRequest request, HttpServletResponse response, User user) {
		bindUserToSession(request.getSession(true), user);
		portalAuthCookieService.writeCookie(request, response, user.getId());
	}

	public void clearWebSession(HttpServletRequest request, HttpServletResponse response, HttpSession session) {
		if (response != null && request != null) {
			portalAuthCookieService.clearCookie(request, response);
		}
		if (session == null) {
			return;
		}
		session.removeAttribute(WebSessionKeys.USER_ID);
		session.removeAttribute("jwt");
	}

	public boolean isPortalAdministrator(HttpServletRequest request) {
		return userFromRequest(request).filter(RoleCodes::isPortalAdministrator).isPresent();
	}

	public boolean isPortalAdministrator(HttpSession session) {
		return userFromSession(session).filter(RoleCodes::isPortalAdministrator).isPresent();
	}

	public boolean isAdmin(HttpSession session) {
		return isPortalAdministrator(session);
	}

	public Optional<String> denyUnlessAdmin(String authHeader) {
		Optional<User> actor = resolveActor(authHeader);
		if (actor.isEmpty()) {
			return Optional.of("Authentication required. Send Authorization: Bearer <token>.");
		}
		if (!RoleCodes.isPortalAdministrator(actor.get())) {
			return Optional.of("Only administrators can perform this action.");
		}
		return Optional.empty();
	}

	public Optional<User> resolveActor(String authHeader) {
		if (authHeader == null || !authHeader.startsWith("Bearer ")) {
			return Optional.empty();
		}
		try {
			String token = authHeader.substring(7).trim();
			if (token.isEmpty()) {
				return Optional.empty();
			}
			String email = jwtService.extractEmail(token);
			return userRepository.findByEmailIgnoreCaseWithProfile(email.trim().toLowerCase());
		} catch (Exception e) {
			return Optional.empty();
		}
	}
}
