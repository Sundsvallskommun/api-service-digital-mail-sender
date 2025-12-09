package se.sundsvall.digitalmail.api.model;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;
import static org.springframework.http.MediaType.APPLICATION_PDF_VALUE;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import se.sundsvall.dept44.common.validators.annotation.OneOf;
import se.sundsvall.dept44.common.validators.annotation.ValidBase64;

@Setter
@Getter
@Builder(setterPrefix = "with")
@AllArgsConstructor
@NoArgsConstructor
// Schema name is set as attachment to align with previous versions of the API, when the actual
// class has been renamed from "Attachment" to "File" to confirm with the Kivra invoice API and to
// avoid clashes with Skatteverket generated code
@Schema(description = "A PDF file/attachment", name = "Attachment")
public class File {

	@OneOf(APPLICATION_PDF_VALUE)
	@Schema(description = "Allowed type is: application/pdf", examples = APPLICATION_PDF_VALUE, requiredMode = REQUIRED)
	private String contentType;

	@NotBlank
	@ValidBase64
	@Schema(description = "BASE64-encoded body", requiredMode = REQUIRED)
	private String body;

	@NotBlank
	@Schema(description = "The name of the file", examples = "sample.pdf", requiredMode = REQUIRED)
	private String filename;
}
