package se.sundsvall.digitalmail.api.model;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;
import static lombok.AccessLevel.PROTECTED;
import static org.springframework.http.MediaType.TEXT_HTML_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

import jakarta.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import se.sundsvall.dept44.common.validators.annotation.OneOf;
import se.sundsvall.digitalmail.api.model.validation.ValidHtml;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Schema(description = "The body of the message")
@SuperBuilder(setterPrefix = "with")
@AllArgsConstructor(access = PROTECTED)
@NoArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "contentType", visible = true, defaultImpl = BodyInformation.Unknown.class)
@JsonSubTypes({
    @JsonSubTypes.Type(value = BodyInformation.PlainText.class, name = TEXT_PLAIN_VALUE),
    @JsonSubTypes.Type(value = BodyInformation.Html.class, name = TEXT_HTML_VALUE),
})
public abstract sealed class BodyInformation permits BodyInformation.Unknown, BodyInformation.PlainText, BodyInformation.Html {

    @OneOf({ TEXT_PLAIN_VALUE, TEXT_HTML_VALUE})
    @Schema(description = "The content type for the message, text/plain for only text, text/html for html messages.", example = TEXT_HTML_VALUE, requiredMode = REQUIRED)
    private String contentType;

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

    @NoArgsConstructor
    @SuperBuilder(setterPrefix = "with")
    @Schema(description = "Plain-text body", allOf = BodyInformation.class)
    public static final class PlainText extends BodyInformation {

        @Override
        public String getContentType() {
            return TEXT_PLAIN_VALUE;
        }

        @NotBlank
        @Schema(description = "Plain-text body", requiredMode = REQUIRED)
        @Override
        public String getBody() {
            return super.getBody();
        }
    }

    @NoArgsConstructor
    @SuperBuilder(setterPrefix = "with")
    @Schema(description = "HTML body", allOf = BodyInformation.class)
    public static final class Html extends BodyInformation {

        @Override
        public String getContentType() {
            return TEXT_HTML_VALUE;
        }

        @ValidHtml
        @Schema(description = "BASE64-encoded HTML body", requiredMode = REQUIRED)
        @Override
        public String getBody() {
            return super.getBody();
        }
    }

    @NoArgsConstructor
    @SuperBuilder(setterPrefix = "with")
    public static final class Unknown extends BodyInformation {

    }
}
