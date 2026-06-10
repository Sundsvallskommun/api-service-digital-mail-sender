package se.sundsvall.digitalmail.util;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.openpdf.text.Document;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.PdfSmartCopy;
import org.openpdf.text.pdf.PdfStream;
import se.sundsvall.digitalmail.api.model.File;

import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;

@Slf4j
public final class PdfCompressor {

	private PdfCompressor() {}

	public static void compress(final List<File> files) {
		if (files == null) {
			return;
		}
		files.stream().filter(file -> StringUtils.isNotEmpty(file.getBody())).forEach(file -> {
			log.info("Trying to compress pdf: {}", sanitizeForLogging(file.getFilename()));

			final var sizeBeforeCompression = file.getBody().length();
			file.setBody(compress(file.getBody()));

			logCompressionResult(sizeBeforeCompression, file.getBody().length());
		});
	}

	private static void logCompressionResult(final int sizeBeforeCompression, final int sizeAfterCompression) {
		final var compressedPercentage = String.format("%.0f", (double) sizeAfterCompression / sizeBeforeCompression * 100);

		if (!"100".equals(compressedPercentage)) {
			log.info("Pdf is now {}% of the original size.", compressedPercentage);
		} else {
			log.info("Couldn't compress pdf.");
		}
	}

	public static String compress(final String pdfContent) {
		// decode the base64-content
		try (final var pdfReader = new PdfReader(Base64.getDecoder().decode(pdfContent.getBytes(StandardCharsets.UTF_8)));
			final var document = new Document();
			final var result = new ByteArrayOutputStream()) {

			final var pdfSmartCopy = new PdfSmartCopy(document, result);
			pdfSmartCopy.setFullCompression();
			pdfSmartCopy.setCompressionLevel(PdfStream.BEST_COMPRESSION);
			document.open();

			for (int pageNumber = 1; pageNumber <= pdfReader.getNumberOfPages(); pageNumber++) {
				final var page = pdfSmartCopy.getImportedPage(pdfReader, pageNumber);
				pdfSmartCopy.addPage(page);
			}
			pdfSmartCopy.close();
			// encode the compressed content back to base64
			return Base64.getEncoder().encodeToString(result.toByteArray());

		} catch (final Exception e) {
			log.warn("A problem occurred during compression of PDF: {}", e.getMessage());
		}

		// If compression fails, return the original content
		return pdfContent;
	}
}
