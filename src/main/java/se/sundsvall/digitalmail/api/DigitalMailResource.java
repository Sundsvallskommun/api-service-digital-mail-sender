package se.sundsvall.digitalmail.api;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_HTML_VALUE;
import static org.springframework.http.ResponseEntity.ok;
import static org.zalando.problem.Status.BAD_REQUEST;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Optional;
import org.hibernate.validator.constraints.UniqueElements;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zalando.problem.Problem;
import org.zalando.problem.violations.ConstraintViolationProblem;
import se.sundsvall.dept44.common.validators.annotation.ValidMunicipalityId;
import se.sundsvall.dept44.common.validators.annotation.ValidOrganizationNumber;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;
import se.sundsvall.digitalmail.api.model.DigitalInvoiceRequest;
import se.sundsvall.digitalmail.api.model.DigitalInvoiceResponse;
import se.sundsvall.digitalmail.api.model.DigitalMailRequest;
import se.sundsvall.digitalmail.api.model.DigitalMailResponse;
import se.sundsvall.digitalmail.api.model.Mailbox;
import se.sundsvall.digitalmail.api.model.validation.HtmlValidator;
import se.sundsvall.digitalmail.integration.kivra.InvoiceDto;
import se.sundsvall.digitalmail.integration.skatteverket.DigitalMailDto;
import se.sundsvall.digitalmail.service.DigitalMailService;

@RestController
@Validated
@RequestMapping(value = "/{municipalityId}")
@Tag(name = "Digital Mail")
@ApiResponse(responseCode = "200",
	description = "Successful Operation",
	useReturnTypeSchema = true)
@ApiResponse(responseCode = "400",
	description = "Bad Request",
	content = @Content(
		mediaType = APPLICATION_PROBLEM_JSON_VALUE,
		schema = @Schema(oneOf = {
			Problem.class, ConstraintViolationProblem.class
		})))
@ApiResponse(responseCode = "404",
	description = "Not Found",
	content = @Content(
		mediaType = APPLICATION_PROBLEM_JSON_VALUE,
		schema = @Schema(implementation = Problem.class)))
@ApiResponse(responseCode = "500",
	description = "Internal Server Error",
	content = @Content(
		mediaType = APPLICATION_PROBLEM_JSON_VALUE,
		schema = @Schema(implementation = Problem.class)))
class DigitalMailResource {

	private final DigitalMailService digitalMailService;

	private final HtmlValidator htmlValidator;

	DigitalMailResource(final DigitalMailService digitalMailService, final HtmlValidator htmlValidator) {
		this.digitalMailService = digitalMailService;
		this.htmlValidator = htmlValidator;
	}

	@Operation(summary = "Send a digital mail")
	@PostMapping(
		value = "/{organizationNumber}/send-digital-mail",
		consumes = APPLICATION_JSON_VALUE,
		produces = APPLICATION_JSON_VALUE)
	ResponseEntity<DigitalMailResponse> sendDigitalMail(
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "organizationNumber", description = "The organization number of the sending organization", example = "5561234567") @ValidOrganizationNumber @PathVariable final String organizationNumber,
		@Valid @RequestBody final DigitalMailRequest request) {

		// Validate body as HTML if content type is text/html
		Optional.ofNullable(request.getBodyInformation())
			.filter(bodyInfo -> TEXT_HTML_VALUE.equals(bodyInfo.getContentType()) && !htmlValidator.validate(bodyInfo.getBody()))
			.ifPresent(bodyInfo -> {
				throw Problem.builder()
					.withTitle("Body HTML is invalid")
					.withStatus(BAD_REQUEST)
					.withDetail("Use https://validator.w3.org/ to make sure your HTML validates")
					.build();
			});

		return ok(digitalMailService.sendDigitalMail(new DigitalMailDto(request, organizationNumber), municipalityId));
	}

	@Operation(summary = "Send a digital invoice")
	@PostMapping(
		value = "/send-digital-invoice",
		consumes = APPLICATION_JSON_VALUE,
		produces = APPLICATION_JSON_VALUE)
	ResponseEntity<DigitalInvoiceResponse> sendDigitalInvoice(
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Valid @RequestBody final DigitalInvoiceRequest request) {
		return ok(digitalMailService.sendDigitalInvoice(new InvoiceDto(request), municipalityId));
	}

	// A Post instead of GET since we may have a long list of partyIds, and GET has a limit on the URL length.
	@Operation(summary = "Retrieve a list of mailboxes. Contains partyId, supplier and if the mailbox is reachable for the given organization.")
	@PostMapping(
		value = "/{organizationNumber}/mailboxes",
		consumes = APPLICATION_JSON_VALUE,
		produces = APPLICATION_JSON_VALUE)
	ResponseEntity<List<Mailbox>> hasAvailableMailboxes(
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "organizationNumber", description = "The organization number of the intended sending organization", example = "5561234567") @ValidOrganizationNumber @PathVariable final String organizationNumber,
		@RequestBody @UniqueElements @NotEmpty final List<@ValidUuid String> partyIds) {
		return ok(digitalMailService.getMailboxes(partyIds, municipalityId, organizationNumber));
	}
}
