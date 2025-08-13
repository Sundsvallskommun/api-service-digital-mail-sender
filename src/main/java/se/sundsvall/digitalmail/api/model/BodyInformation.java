package se.sundsvall.digitalmail.api.model;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;
import static lombok.AccessLevel.PROTECTED;
import static org.springframework.http.MediaType.TEXT_HTML_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import se.sundsvall.dept44.common.validators.annotation.OneOf;

@Schema(description = "The body of the message")
@Builder(setterPrefix = "with")
@NoArgsConstructor
@AllArgsConstructor(access = PROTECTED)
@Getter
@Setter
public class BodyInformation {

	@OneOf({
		TEXT_PLAIN_VALUE, TEXT_HTML_VALUE
	})
	@Schema(description = "The content type for the message, text/plain for only text, text/html for HTML messages.", example = TEXT_HTML_VALUE, requiredMode = REQUIRED)
	private String contentType;

	@Schema(description = "Plain-text body")
	private String body;

}
