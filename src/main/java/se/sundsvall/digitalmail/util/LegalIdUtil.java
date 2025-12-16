package se.sundsvall.digitalmail.util;

import static java.util.Optional.ofNullable;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;

public final class LegalIdUtil {

	// Using strict resolver style to ensure dates like 20230230 are considered invalid
	private static final DateTimeFormatter BIRTH_DATE_FORMATTER = DateTimeFormatter.ofPattern("uuuuMMdd")
		.withResolverStyle(ResolverStyle.STRICT);
	private static final int ORG_NUMBER_LENGTH = 10;
	private static final int ORG_NUMBER_PREFIXED_WITH_16_LENGTH = 12;
	private static final int PERSON_ID_LENGTH = 12;
	private static final int ADULT_AGE = 18;

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
			.filter(string -> string.length() == ORG_NUMBER_LENGTH)
			.map(string -> "16" + string)
			.orElse(orgNumber);
	}

	/**
	 * Removes the "16" prefix from the given orgNumber if it is exactly 12 characters long.
	 *
	 * @param  orgNumber the organization number to potentially remove the prefix from
	 * @return           the organization number without the "16" prefix or the original if no prefix was present
	 */
	private static String removeOrgNumberPrefix(final String orgNumber) {
		if (orgNumber == null) {
			return null;
		}
		return orgNumber.length() == ORG_NUMBER_PREFIXED_WITH_16_LENGTH ? orgNumber.substring(2) : orgNumber;
	}

	/**
	 * Checks if the provided organization number is a valid organization number.
	 *
	 * @param  orgNumber the organization number to validate
	 * @return           true if the orgNumber is valid, false otherwise
	 */
	public static boolean isOrgNumber(final String orgNumber) {
		if (orgNumber == null) {
			return false;
		}

		// Make sure it's either 10 or 12 characters long
		final var length = orgNumber.length();
		if (length != ORG_NUMBER_LENGTH && length != ORG_NUMBER_PREFIXED_WITH_16_LENGTH) {
			return false;
		}

		// Remove prefix (16) if present to make it easier to validate together with non-prefixed org numbers
		final var trimmedOrgNumber = removeOrgNumberPrefix(orgNumber);

		// The third number (index 2) in a Swedish organization number is always >= 2 to not be confused with a personal number
		// that may be 0 or 1 in that position.
		try {
			// Extract the digit with index 2 and make sure it's >= 2
			final var monthPart = Integer.parseInt(trimmedOrgNumber.substring(2, 3));
			return monthPart >= 2;
		} catch (final NumberFormatException _) {
			return false;
		}
	}

	/**
	 * Check that the person is an adult (18 years or older).
	 * This method validates Swedish personal identity numbers (personnummer) only.
	 *
	 * @param  personId the person's legal Id (12 digits: YYYYMMDDXXXX)
	 * @return          true if the person is an adult (18 years or older), false otherwise
	 */
	public static boolean isAnAdult(final String personId) {
		// Sanity check if we for some reason would receive a null or malformed personId
		if (personId == null || personId.length() != PERSON_ID_LENGTH) {
			return false;
		}

		try {
			// Extract the year, month and date portion and parse it to a LocalDate
			var birthDate = LocalDate.parse(personId.substring(0, 8), BIRTH_DATE_FORMATTER);

			// Ensure the birthdate is not in the future
			if (birthDate.isAfter(LocalDate.now())) {
				return false;
			}

			var age = Period.between(birthDate, LocalDate.now()).getYears();

			return age >= ADULT_AGE;
		} catch (final Exception _) {
			// If parsing fails or any other exception occurs, treat as invalid
			return false;
		}
	}
}
