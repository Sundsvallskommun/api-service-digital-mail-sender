package se.sundsvall.digitalmail.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import se.sundsvall.dept44.common.validators.annotation.ValidMunicipalityId;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(setterPrefix = "with")
@Schema(description = "The body of the digital mail request")
public class DigitalMailRequest {

	@ValidUuid
	@Schema(description = "partyId for the person or organization the digital mail should be sent to", examples = "6a5c3d04-412d-11ec-973a-0242ac130003", requiredMode = REQUIRED)
	private String partyId;

	@ValidMunicipalityId
	@Schema(description = "MunicipalityId", examples = "2281", requiredMode = REQUIRED)
	private String municipalityId;

	@NotBlank
	@Schema(description = "The subject of the digital mail.", examples = "Viktig information från Sundsvalls kommun", requiredMode = REQUIRED)
	private String headerSubject;

	@NotNull
	@Valid
	private SupportInfo supportInfo;

	@Builder.Default
	private List<File> attachments = new ArrayList<>();

	@Valid
	private BodyInformation bodyInformation;
}
