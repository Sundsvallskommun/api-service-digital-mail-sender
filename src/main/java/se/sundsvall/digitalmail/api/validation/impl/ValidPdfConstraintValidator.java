package se.sundsvall.digitalmail.api.validation.impl;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.io.IOException;
import java.util.Base64;
import org.openpdf.text.pdf.PdfReader;
import se.sundsvall.digitalmail.api.validation.ValidPdf;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class ValidPdfConstraintValidator implements ConstraintValidator<ValidPdf, String> {

	private static final Base64.Decoder BASE64_DECODER = Base64.getDecoder();
	// Acrobat accepts the %%EOF marker anywhere within the last 1024 bytes of the file
	private static final int EOF_SEARCH_WINDOW = 1024;
	private static final String EOF_MARKER = "%%EOF";

	@Override
	public boolean isValid(final String value, final ConstraintValidatorContext context) {
		if (isBlank(value)) {
			return true;
		}

		final byte[] bytes;
		try {
			bytes = BASE64_DECODER.decode(value);
		} catch (final IllegalArgumentException _) {
			return true;
		}

		if (!hasEofMarker(bytes)) {
			return false;
		}

		try (final var reader = new PdfReader(bytes)) {
			return reader.getNumberOfPages() > 0;
		} catch (final IOException | RuntimeException _) {
			return false;
		}
	}

	private static boolean hasEofMarker(final byte[] bytes) {
		final var from = Math.max(0, bytes.length - EOF_SEARCH_WINDOW);
		return new String(bytes, from, bytes.length - from, ISO_8859_1).contains(EOF_MARKER);
	}
}
