package se.sundsvall.digitalmail.api.model.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import se.sundsvall.dept44.test.annotation.resource.Load;
import se.sundsvall.dept44.test.extension.ResourceLoaderExtension;

import jakarta.validation.ValidationException;

@ExtendWith(ResourceLoaderExtension.class)
class HtmlValidatorTests {

	private final HtmlValidator validator = new HtmlValidator();

	@Test
	void validValue(@Load("valid-html.base64") final String html) {
		assertThat(validator.validate(html)).isTrue();
	}

	@Test
	void invalidValue(@Load("invalid-html.base64") final String html) {
		assertThat(validator.validate(html)).isFalse();
	}

	@ParameterizedTest(name = "[{index}] value=\"{0}\"")
	@ValueSource(strings = {"", "   "})
	void emptyHtml(final String value) {
		assertThat(validator.validate(value)).isFalse();
	}

	@Test
	void nullValue() {
		assertThat(validator.validate(null)).isFalse();
	}

	@Test
	void throwsException() {
		final var e = assertThrows(ValidationException.class, () -> validator.validate("x"));
		assertThat(e.getMessage()).isEqualTo("Unable to validate HTML");
	}
}
