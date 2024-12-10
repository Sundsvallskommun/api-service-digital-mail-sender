package se.sundsvall.digitalmail.api.model;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import se.sundsvall.dept44.common.validators.annotation.ValidMunicipalityId;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(setterPrefix = "with")
@Schema(description = "The body of the digital mail request")
public class DigitalMailRequest {

	@ValidUuid
	@Schema(description = "partyId for the person or organization the digital mail should be sent to", example = "6a5c3d04-412d-11ec-973a-0242ac130003", requiredMode = REQUIRED)
	private String partyId;

	@ValidMunicipalityId
	@Schema(description = "MunicipalityId", example = "2281", requiredMode = REQUIRED)
	private String municipalityId;

	@NotBlank
	@Schema(description = "The subject of the digital mail.", example = "Viktig information från Sundsvalls kommun", requiredMode = REQUIRED)
	private String headerSubject;

	@NotNull
	@Valid
	private SupportInfo supportInfo;

	@Builder.Default
	private List<File> attachments = new ArrayList<>();

	@Valid
	private BodyInformation bodyInformation;

	@Override
	public boolean equals(final Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		final DigitalMailRequest that = (DigitalMailRequest) o;
		return Objects.equals(partyId, that.partyId) && Objects.equals(municipalityId, that.municipalityId) && Objects.equals(headerSubject, that.headerSubject) && Objects.equals(supportInfo, that.supportInfo) && Objects.equals(attachments,
			that.attachments) && Objects.equals(bodyInformation, that.bodyInformation);
	}

	@Override
	public int hashCode() {
		return Objects.hash(partyId, municipalityId, headerSubject, supportInfo, attachments, bodyInformation);
	}

	@Override
	public String toString() {
		return "DigitalMailRequest{" +
			"partyId='" + partyId + '\'' +
			", municipalityId='" + municipalityId + '\'' +
			", headerSubject='" + headerSubject + '\'' +
			", supportInfo=" + supportInfo +
			", attachments=" + attachments +
			", bodyInformation=" + bodyInformation +
			'}';
	}

}
