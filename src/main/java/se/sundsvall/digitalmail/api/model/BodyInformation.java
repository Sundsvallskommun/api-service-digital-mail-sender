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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@Schema(
    description = "The body of the message",
    requiredMode = REQUIRED,
    oneOf = { BodyInformation.PlainText.class, BodyInformation.Html.class },
    discriminatorProperty = "contentType"
)
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
    @NotBlank(message = "contentType must not be blank")
    @Schema(description = "The content type for the message, text/plain for only text, text/html for html messages.", example = TEXT_HTML_VALUE, requiredMode = REQUIRED)
    private String contentType;

    public abstract String getBody();

    @Getter
    @Setter
    @NoArgsConstructor
    @SuperBuilder(setterPrefix = "with")
    @Schema(description = "Plain-text body", discriminatorProperty = "contentType")
    public static final class PlainText extends BodyInformation {

        private final String contentType = TEXT_PLAIN_VALUE;

        @NotBlank
        @Schema(description = "Plain text body", requiredMode = REQUIRED)
        private String body;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @SuperBuilder(setterPrefix = "with")
    @Schema(description = "HTML body", discriminatorProperty = "contentType")
    public static final class Html extends BodyInformation {

        private final String contentType = TEXT_HTML_VALUE;

        @ValidHtml
        @Schema(description = "BASE64-encoded HTML body", example = "PCFET0NUWVBFIGh0bWw+PGh0bWwgbGFuZz0iZW4iPjxoZWFkPjxtZXRhIGNoYXJzZXQ9InV0Zi04Ij48dGl0bGU+VGVzdDwvdGl0bGU+PC9oZWFkPjxib2R5PjxwPkhlbGxvPC9wPjwvYm9keT48L2h0bWw+", requiredMode = REQUIRED)
        private String body;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @SuperBuilder(setterPrefix = "with")
    public static final class Unknown extends BodyInformation {

        private String body;
    }
}
