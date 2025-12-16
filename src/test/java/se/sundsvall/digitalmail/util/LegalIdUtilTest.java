package se.sundsvall.digitalmail.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class LegalIdUtilTest {

	@ParameterizedTest
	@MethodSource("orgNumbersProvider")
	void prefixOrgNumber(String orgNumber, String expected) {
		assertEquals(expected, LegalIdUtil.prefixOrgNumber(orgNumber));
	}

	private static Stream<Arguments> orgNumbersProvider() {
		return Stream.of(
			Arguments.of("1234567890", "161234567890"),
			Arguments.of("161234567890", "161234567890"),
			Arguments.of(null, null),
			Arguments.of("", ""),
			Arguments.of(" ", " "));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("isAnAdultPersonProvider")
	void testIsAnAdult(String testName, String personId, boolean expectedResult) {
		assertThat(LegalIdUtil.isAnAdult(personId)).isEqualTo(expectedResult);
	}

	private static Stream<Arguments> isAnAdultPersonProvider() {
		final var today = LocalDate.now();
		final var formatter = DateTimeFormatter.ofPattern("uuuuMMdd");  // Using strict mode in the actual implementation

		// Dynamic dates
		final var exactly18Years = today.minusYears(18);
		final var turned18Yesterday = exactly18Years.minusDays(1);
		final var turns18Tomorrow = exactly18Years.plusDays(1);
		final var over18Years = today.minusYears(20);
		final var under18Years = today.minusYears(15);
		final var notYetBorn = today.plusYears(5);

		return Stream.of(
			// Valid adults (18 years or older)
			Arguments.of("Adult - 20 years old", over18Years.format(formatter) + "0000", true),
			Arguments.of("Person exactly 18 years old today", exactly18Years.format(formatter) + "0000", true),
			Arguments.of("Person turned 18 yesterday", turned18Yesterday.format(formatter) + "0000", true),

			// Valid minors (under 18)
			Arguments.of("Person turns 18 tomorrow (17 years old)", turns18Tomorrow.format(formatter) + "0000", false),
			Arguments.of("Minor - 15 years old", under18Years.format(formatter) + "0000", false),

			// Edge cases
			Arguments.of("Null person ID", null, false),
			Arguments.of("Empty string", "", false),
			Arguments.of("Too short", "19856", false),
			Arguments.of("Too long", "1986010100001", false),

			// Invalid dates that will throw DateTimeParseException
			Arguments.of("Invalid day (32nd)", "200713320000", false),
			Arguments.of("Invalid month (00)", "200700010000", false),
			Arguments.of("Invalid date (Feb 30)", "200702300000", false),
			Arguments.of("Non-numeric characters", "ABCD12340000", false),

			// Future dates
			Arguments.of("Future date (+5 years)", notYetBorn.format(formatter) + "0000", false));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("organizationNumberProvider")
	void testIsAnOrganization(String testName, String organizationNumber, boolean expectedResult) {
		assertThat(LegalIdUtil.isOrgNumber(organizationNumber)).isEqualTo(expectedResult);
	}

	private static Stream<Arguments> organizationNumberProvider() {
		return Stream.of(
			// Organization numbers (month >= 20, always return true)
			Arguments.of("Long organization number", "162120002441", true),
			Arguments.of("Regular organization number", "2120002441", true),
			Arguments.of("Too long organization number", "1621200024411", false),
			Arguments.of("Too short organization number", "212000244", false),

			// And a few non-organization numbers for control
			Arguments.of("Person ID - month 01", "198601010000", false),
			Arguments.of("Person ID - month 12", "199212310000", false));
	}
}
