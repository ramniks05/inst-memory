package com.dolr.backend.security;

import com.dolr.backend.entity.User;

/**
 * Canonical role strings stored on {@link com.dolr.backend.entity.User#getRole()}.
 */
public final class RoleCodes {

	public static final String ADMIN = "ADMIN";
	public static final String OFFICER = "OFFICER";

	private RoleCodes() {
	}

	private static String trimRole(String role) {
		if (role == null) {
			return null;
		}
		String t = role.trim();
		return t.isEmpty() ? null : t;
	}

	public static boolean isAdmin(String role) {
		String t = trimRole(role);
		return t != null && ADMIN.equalsIgnoreCase(t);
	}

	public static boolean isOfficerRole(String role) {
		String t = trimRole(role);
		return t != null && OFFICER.equalsIgnoreCase(t);
	}

	/** True when this row is a departmental roster user (not the portal shell admin). */
	public static boolean isDepartmentalUser(User user) {
		if (user == null) {
			return false;
		}
		if (isOfficerRole(user.getRole())) {
			return true;
		}
		if (Boolean.TRUE.equals(user.getIsOfficer())) {
			return true;
		}
		return user.getDesignationRef() != null || user.getDivisionRef() != null;
	}

	/**
	 * Portal shell (master data, admin navigation): {@code ADMIN} role and not a departmental user.
	 * Treats {@code is_officer} null as false so legacy admin rows still get the admin menu.
	 */
	public static boolean isPortalAdministrator(User user) {
		if (user == null) {
			return false;
		}
		if (isDepartmentalUser(user)) {
			return false;
		}
		return isAdmin(user.getRole());
	}

	/**
	 * Fallback when only login payload fields exist (no FK graph). Prefer {@link #isPortalAdministrator(User)}.
	 */
	public static boolean isPortalAdministrator(String role, Boolean departmentalOfficer) {
		if (isOfficerRole(role)) {
			return false;
		}
		if (Boolean.TRUE.equals(departmentalOfficer)) {
			return false;
		}
		return isAdmin(role);
	}
}
