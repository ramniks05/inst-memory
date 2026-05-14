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

	/**
	 * Portal shell (master data, admin navigation): {@code ADMIN} role, explicitly not on the officer roster,
	 * and no departmental profile links. Departmental rows that wrongly kept {@code ADMIN} are excluded.
	 */
	public static boolean isPortalAdministrator(User user) {
		if (user == null) {
			return false;
		}
		if (isOfficerRole(user.getRole())) {
			return false;
		}
		if (Boolean.TRUE.equals(user.getIsOfficer())) {
			return false;
		}
		if (!Boolean.FALSE.equals(user.getIsOfficer())) {
			return false;
		}
		if (user.getDesignationRef() != null || user.getDivisionRef() != null) {
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
		if (!Boolean.FALSE.equals(departmentalOfficer)) {
			return false;
		}
		return isAdmin(role);
	}
}
