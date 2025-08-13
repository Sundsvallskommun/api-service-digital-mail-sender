package se.sundsvall.digitalmail.api.model;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "Contains contact information and where the recipient may turn to for questions.", requiredMode = REQUIRED)
@Builder(setterPrefix = "with")
@Getter
@Setter
public class SupportInfo {

	@NotBlank
	@Schema(description = "Information text describing the different ways the recipient may contact the sender.", example = "Kontakta oss via epost eller telefon.", requiredMode = REQUIRED)
	private String supportText;

	@Schema(description = "Url where the recipient may find more information.", example = "https://sundsvall.se/")
	private String contactInformationUrl;

	@Schema(description = "Phone number the recipient may call to get in contact with the sender.", example = "4660191000")
	private String contactInformationPhoneNumber;

	@Schema(description = "Email address the recipient may use to get in contact with the sender.", example = "sundsvalls.kommun@sundsvall.se")
	private String contactInformationEmail;
}
