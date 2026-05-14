package com.dolr.backend.security;

import com.dolr.backend.entity.User;
import com.dolr.backend.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AdminAuthHelper {

	private final JwtService jwtService;
	private final UserRepository userRepository;

	public Optional<User> userFromSession(HttpSession session) {
		if (session == null) {
			return Optional.empty();
		}
		Object token = session.getAttribute("jwt");
		if (!(token instanceof String) || ((String) token).isBlank()) {
			return Optional.empty();
		}
		try {
			String email = jwtService.extractEmail(((String) token).trim());
			return userRepository.findByEmailIgnoreCase(email.trim().toLowerCase());
		} catch (Exception e) {
			return Optional.empty();
		}
	}

	public boolean isPortalAdministrator(HttpSession session) {
		return userFromSession(session).filter(RoleCodes::isPortalAdministrator).isPresent();
	}

	/**
	 * Same as {@link #isPortalAdministrator(HttpSession)}; kept for call sites that mean "portal admin".
	 */
	public boolean isAdmin(HttpSession session) {
		return isPortalAdministrator(session);
	}

	/** Empty if caller is an authenticated administrator; otherwise an error message for the client. */
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
			return userRepository.findByEmailIgnoreCase(email.trim().toLowerCase());
		} catch (Exception e) {
			return Optional.empty();
		}
	}
}
