package se.sundsvall.digitalmail.api;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_HTML_VALUE;
import static org.springframework.http.ResponseEntity.notFound;
import static org.springframework.http.ResponseEntity.ok;
import static org.zalando.problem.Status.BAD_REQUEST;

import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zalando.problem.Problem;
import org.zalando.problem.violations.ConstraintViolationProblem;

import se.sundsvall.dept44.common.validators.annotation.ValidUuid;
import se.sundsvall.digitalmail.api.model.DigitalInvoiceRequest;
import se.sundsvall.digitalmail.api.model.DigitalInvoiceResponse;
import se.sundsvall.digitalmail.api.model.DigitalMailRequest;
import se.sundsvall.digitalmail.api.model.DigitalMailResponse;
import se.sundsvall.digitalmail.api.model.validation.HtmlValidator;
import se.sundsvall.digitalmail.integration.kivra.InvoiceDto;
import se.sundsvall.digitalmail.integration.skatteverket.DigitalMailDto;
import se.sundsvall.digitalmail.service.DigitalMailService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@Validated
@RequestMapping(value = "/")
@Tag(name = "Digital Mail")
class DigitalMailResource {
    
    private final DigitalMailService digitalMailService;
    private final HtmlValidator htmlValidator;

    DigitalMailResource(final DigitalMailService digitalMailService, final HtmlValidator htmlValidator) {
        this.digitalMailService = digitalMailService;
        this.htmlValidator = htmlValidator;
    }

    @Operation(
        summary = "Send a digital mail",
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(schema = @Schema(oneOf = { Problem.class, ConstraintViolationProblem.class }))),
            @ApiResponse(responseCode = "404", description = "Not Found", content = @Content(schema = @Schema(implementation = Problem.class))),
            @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content(schema = @Schema(implementation = Problem.class)))
        }
    )
    @PostMapping(
        value = "/send-digital-mail",
        consumes = APPLICATION_JSON_VALUE,
        produces = { APPLICATION_JSON_VALUE, APPLICATION_PROBLEM_JSON_VALUE }
    )
    ResponseEntity<DigitalMailResponse> sendDigitalMail(@Valid @RequestBody final DigitalMailRequest request) {

        Optional.ofNullable(request.getBodyInformation())
                .filter(bodyInfo -> TEXT_HTML_VALUE.equals(request.getBodyInformation().getContentType()) && !htmlValidator.validate(request.getBodyInformation().getBody()))
                .ifPresent(bodyInfo -> {
                    throw Problem.builder()
                            .withTitle("Body HTML is invalid")
                            .withStatus(BAD_REQUEST)
                            .withDetail("Use https://validator.w3.org/ to make sure your HTML validates")
                            .build();
                });

        var response = digitalMailService.sendDigitalMail(new DigitalMailDto(request));
        return ok(response);
    }

    @Operation(
        summary = "Send a digital invoice",
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(schema = @Schema(oneOf = { Problem.class, ConstraintViolationProblem.class }))),
            @ApiResponse(responseCode = "404", description = "Not Found", content = @Content(schema = @Schema(implementation = Problem.class))),
            @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content(schema = @Schema(implementation = Problem.class)))
        }
    )
    @PostMapping(
        value = "/send-digital-invoice",
        consumes = APPLICATION_JSON_VALUE,
        produces = { APPLICATION_JSON_VALUE, APPLICATION_PROBLEM_JSON_VALUE }
    )
    ResponseEntity<DigitalInvoiceResponse> sendDigitalInvoice(
            @Valid @RequestBody final DigitalInvoiceRequest request) {
        return ok(digitalMailService.sendDigitalInvoice(new InvoiceDto(request)));
    }

    @Operation(
        summary = "Check if a party has a digital mailbox and if said party has chosen to receive digital mail",
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful Operation (available mailbox(es) found)"),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(schema = @Schema(implementation = Problem.class))),
            @ApiResponse(responseCode = "404", description = "Not Found (no available mailbox(es) found)", content = @Content(schema = @Schema(implementation = Problem.class))),
            @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content(schema = @Schema(implementation = Problem.class)))
    })
    @PostMapping(value = "/has-available-mailbox/{partyId}")
    ResponseEntity<Void> hasAvailableMailbox(@PathVariable("partyId") @ValidUuid final String partyId) {
        if (digitalMailService.verifyRecipientHasSomeAvailableMailbox(partyId)) {
            return ok().build();
        }
        return notFound().build();
    }
}
