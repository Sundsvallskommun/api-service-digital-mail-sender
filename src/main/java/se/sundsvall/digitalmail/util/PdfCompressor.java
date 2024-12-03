package se.sundsvall.digitalmail.util;

import com.lowagie.text.Document;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfSmartCopy;
import com.lowagie.text.pdf.PdfStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sundsvall.digitalmail.api.model.File;

public final class PdfCompressor {

	private PdfCompressor() {}

	private static final Logger LOGGER = LoggerFactory.getLogger(PdfCompressor.class);

	public static void compress(final List<File> files) {
		files.stream().filter(file -> StringUtils.isNotEmpty(file.getBody())).forEach(file -> {
			LOGGER.info("Trying to compress pdf: {}", file.getFilename());

			var sizeBeforeCompression = file.getBody().length();
			file.setBody(compress(file.getBody()));

			logCompressionResult(sizeBeforeCompression, file.getBody().length());
		});
	}

	private static void logCompressionResult(int sizeBeforeCompression, int sizeAfterCompression) {
		var compressedPercentage = String.format("%.0f", (double) sizeAfterCompression / sizeBeforeCompression * 100);

		if (!"100".equals(compressedPercentage)) {
			LOGGER.info("Pdf is now {}% of the original size.", compressedPercentage);
		} else {
			LOGGER.info("Couldn't compress pdf.");
		}
	}

	public static String compress(String pdfContent) {
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
			LOGGER.warn("A problem occured during compression of PDF: {}", e.getMessage());
		}

		// If compression fails, return the original content
		return pdfContent;
	}
}
