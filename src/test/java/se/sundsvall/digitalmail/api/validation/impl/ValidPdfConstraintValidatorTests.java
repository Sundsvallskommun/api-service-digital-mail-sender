package se.sundsvall.digitalmail.api.validation.impl;

import java.util.Arrays;
import java.util.Base64;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.digitalmail.TestObjectFactory.getSamplePdfBase64;

class ValidPdfConstraintValidatorTests {
	private static final byte[] PDF_CONTENT = Base64.getDecoder().decode(getSamplePdfBase64());

	private final ValidPdfConstraintValidator validator = new ValidPdfConstraintValidator();

	@Test
	void validPdf() {
		assertThat(validator.isValid(getSamplePdfBase64(), null)).isTrue();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("invalidPdfs")
	void invalidPdf(final String scenario, final byte[] content) {
		assertThat(validator.isValid(Base64.getEncoder().encodeToString(content), null)).isFalse();
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {
		" ", "not base64 %%%"
	})
	void blankOrInvalidBase64IsLeftToOtherAnnotations(final String value) {
		assertThat(validator.isValid(value, null)).isTrue();
	}

	private static Stream<Arguments> invalidPdfs() {
		return Stream.of(
			Arguments.of("text", "some pdf data".getBytes(US_ASCII)),
			Arguments.of("png image instead of pdf", Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==")),
			Arguments.of("header and eof marker only", "%PDF-1.7\n%%EOF".getBytes(US_ASCII)),
			Arguments.of("truncated pdf", Arrays.copyOf(PDF_CONTENT, PDF_CONTENT.length / 2)));
	}
}
