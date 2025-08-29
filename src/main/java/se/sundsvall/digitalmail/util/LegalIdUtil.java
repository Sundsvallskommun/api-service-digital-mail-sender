package se.sundsvall.digitalmail.util;

import static java.util.Optional.ofNullable;

public final class LegalIdUtil {

	private LegalIdUtil() {}

	/**
	 * Prefixes the given orgNumber with "16" if it is exactly 10 characters long.
	 * Otherwise, returns the orgNumber unchanged.
	 * 
	 * @param  orgNumber the organization number to potentially prefix
	 * @return           the prefixed organization number or the original if no prefixing was done
	 */
	public static String prefixOrgNumber(String orgNumber) {
		return ofNullable(orgNumber)
			.filter(string -> string.length() == 10)
			.map(string -> "16" + string)
			.orElse(orgNumber);
	}
}
