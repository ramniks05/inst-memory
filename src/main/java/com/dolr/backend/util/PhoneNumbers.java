package com.dolr.backend.util;

/**
 * Normalises phone-style input to digits only for validation and storage.
 */
public final class PhoneNumbers {

	private PhoneNumbers() {
	}

	public static String digitsOnly(String raw) {
		if (raw == null) {
			return "";
		}
		return raw.replaceAll("\\D", "");
	}
}
