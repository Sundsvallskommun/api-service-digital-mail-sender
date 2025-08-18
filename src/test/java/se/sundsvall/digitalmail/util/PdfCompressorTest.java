package se.sundsvall.digitalmail.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openpdf.text.pdf.PdfReader;
import se.sundsvall.digitalmail.TestObjectFactory;
import se.sundsvall.digitalmail.api.model.File;

class PdfCompressorTest {

	private static final String CONTENT_TYPE = "contentType";
	private static final String VALID_PDF_FILE_NAME = "test.pdf";
	private static final String INVALID_PDF_FILE_NAME = "invalid.pdf";

	@Test
	void testPdfFileShouldBeCompressed() throws IOException {
		var file = TestObjectFactory.getSamplePdfBase64();
		var files = List.of(new File(CONTENT_TYPE, file, VALID_PDF_FILE_NAME));

		PdfCompressor.compress(files);

		assertThat(files.getFirst().getBody().length()).isLessThan(file.length());
		assertThat(files.getFirst().getBody()).isNotEqualTo(file);
		assertThat(validatePdf(files.getFirst().getBody())).isTrue();
	}

	@Test
	void testFaultyPdfShouldNotBeCompressed() {
		var file = "This is not a pdf";
		var files = List.of(new File(CONTENT_TYPE, file, INVALID_PDF_FILE_NAME));

		PdfCompressor.compress(files);

		assertThat(files.getFirst().getBody()).hasSameSizeAs(file);
		assertThat(files.getFirst().getBody()).isEqualTo(file);
		assertThat(validatePdf(files.getFirst().getBody())).isFalse();
	}

	@Test
	void testCompressTwoFiles_oneFails_shouldCompressTheOther() throws IOException {
		var validPdf = TestObjectFactory.getSamplePdfBase64();
		var invalidPdf = "This is not a pdf";
		var validFile = new File(CONTENT_TYPE, validPdf, VALID_PDF_FILE_NAME);
		var invalidFile = new File(CONTENT_TYPE, invalidPdf, INVALID_PDF_FILE_NAME);
		List<File> files = List.of(validFile, invalidFile);

		PdfCompressor.compress(files);

		// Check that one file is compressed while the failed one is not.
		files.forEach(file -> {
			if (file.getFilename().equals(VALID_PDF_FILE_NAME)) {
				assertThat(file.getBody().length()).isLessThan(validPdf.length());
				assertThat(file.getBody()).isNotEqualTo(validPdf);
				assertThat(validatePdf(file.getBody())).isTrue();
			} else {
				assertThat(file.getBody()).hasSameSizeAs(invalidPdf);
				assertThat(file.getBody()).isEqualTo(invalidPdf);
				assertThat(validatePdf(file.getBody())).isFalse();
			}
		});
	}

	// Used to validate that the returned base64 contains a valid pdf.
	private boolean validatePdf(String base64PdfContent) {
		try (var pdfReader = new PdfReader(Base64.getDecoder().decode(base64PdfContent.getBytes(StandardCharsets.UTF_8)))) {
			// If we could read the pdf, it's valid
			return true;
		} catch (Exception ignored) {
			// If not, it's invalid
			return false;
		}
	}
}
