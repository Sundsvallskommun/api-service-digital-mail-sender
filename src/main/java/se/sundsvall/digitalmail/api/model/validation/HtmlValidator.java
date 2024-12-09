package se.sundsvall.digitalmail.api.model.validation;

import static org.apache.commons.lang3.StringUtils.isBlank;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jakarta.validation.ValidationException;
import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.List;
import nu.validator.client.EmbeddedValidator;
import org.springframework.stereotype.Component;

@Component
public class HtmlValidator {

	private static final EmbeddedValidator HTML_VALIDATOR = new EmbeddedValidator();
	private static final Gson GSON = new GsonBuilder().create();
	private static final Base64.Decoder BASE64_DECODER = Base64.getDecoder();

	public boolean validate(final String value) {
		// Treat empty HTML as invalid
		if (isBlank(value)) {
			return false;
		}

		try {
			// Decode the BASE64-encoded HTML
			final var html = new String(BASE64_DECODER.decode(value.trim()));
			// Validate
			final var validationResultAsString = HTML_VALIDATOR.validate(new ByteArrayInputStream(html.getBytes()));
			// Check
			final var validationResult = GSON.fromJson(validationResultAsString, ValidationResult.class);

			return validationResult.isValid();
		} catch (final Exception e) {
			throw new ValidationException("Unable to validate HTML", e);
		}
	}

	/*
	 * Simple validation result record. Just counts the number of validation errors, ignoring what
	 * the errors actually are.
	 */
	record ValidationResult(List<Message> messages) {

		List<String> errors() {
			return messages.stream()
				.filter(message -> "error".equalsIgnoreCase(message.type()))
				.map(Message::message)
				.toList();
		}

		boolean isValid() {
			return errors().isEmpty();
		}

		record Message(String type, String message) {
		}
	}
}
