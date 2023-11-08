package se.sundsvall.digitalmail.api.model;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import se.sundsvall.dept44.common.validators.annotation.ValidMunicipalityId;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;
import se.sundsvall.digitalmail.api.model.validation.ValidBodyInformation;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
    
    @ValidBodyInformation(nullable = true)
    private BodyInformation bodyInformation;
}
