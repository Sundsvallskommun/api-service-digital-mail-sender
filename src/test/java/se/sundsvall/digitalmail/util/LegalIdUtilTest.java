package se.sundsvall.digitalmail.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
