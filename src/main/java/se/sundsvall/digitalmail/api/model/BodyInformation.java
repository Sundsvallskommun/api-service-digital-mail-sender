package se.sundsvall.digitalmail.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import se.sundsvall.dept44.common.validators.annotation.OneOf;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;
import static lombok.AccessLevel.PROTECTED;
import static org.springframework.http.MediaType.TEXT_HTML_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

@Schema(description = "The body of the message")
@Builder(setterPrefix = "with")
@NoArgsConstructor
@AllArgsConstructor(access = PROTECTED)
public class BodyInformation {

	@OneOf({
		TEXT_PLAIN_VALUE, TEXT_HTML_VALUE
	})
	@Schema(description = "The content type for the message, text/plain for only text, text/html for HTML messages.", examples = TEXT_HTML_VALUE, requiredMode = REQUIRED)
	private String contentType;

	@Schema(description = "Plain-text body")
	private String body;

	public String getContentType() {
		return contentType;
	}

	public void setContentType(final String contentType) {
		this.contentType = contentType;
	}

	public String getBody() {
		return body;
	}

	public void setBody(final String body) {
		this.body = body;
	}
}
