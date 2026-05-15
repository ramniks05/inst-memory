package com.dolr.backend.security;

import com.dolr.backend.config.PortalPathSupport;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Redirect targets that include {@code /institutionalmemory} when required on the server. */
@Component
@RequiredArgsConstructor
public class PortalUrlBuilder {

	private final PortalPathSupport portalPathSupport;

	public String redirectTo(HttpServletRequest request, String servletPath) {
		return portalPathSupport.path(request, servletPath);
	}
}
