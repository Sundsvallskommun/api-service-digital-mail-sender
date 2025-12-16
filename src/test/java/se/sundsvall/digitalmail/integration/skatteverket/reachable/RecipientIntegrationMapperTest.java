package se.sundsvall.digitalmail.integration.skatteverket.reachable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.digitalmail.TestObjectFactory.ORGANIZATION_NUMBER;
import static se.sundsvall.digitalmail.TestObjectFactory.PREFIXED_ORGANIZATION_NUMBER;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.gov.minameddelanden.schema.recipient.AccountStatus;
import se.gov.minameddelanden.schema.recipient.ReachabilityStatus;
import se.gov.minameddelanden.schema.recipient.ServiceSupplier;
import se.gov.minameddelanden.schema.recipient.v3.IsReachableResponse;
import se.sundsvall.digitalmail.integration.skatteverket.SkatteverketProperties;

@ExtendWith(MockitoExtension.class)
class RecipientIntegrationMapperTest {

	@Mock
	private SkatteverketProperties mockSkatteverketProperties;

	@InjectMocks
	private RecipientIntegrationMapper mapper;

	@AfterEach
	void tearDown() {
		verifyNoMoreInteractions(mockSkatteverketProperties);
	}

	@Test
	void testCreateIsReachableRequest() {
		final var legalIds = List.of("legalId", "anotherLegalId");

		final var isReachableRequest = mapper.createIsReachableRequest(legalIds, ORGANIZATION_NUMBER);

		assertThat(isReachableRequest.getSenderOrgNr()).isEqualTo(PREFIXED_ORGANIZATION_NUMBER);
		assertThat(isReachableRequest.getRecipientIds()).containsExactlyInAnyOrderElementsOf(legalIds);

		verifyNoInteractions(mockSkatteverketProperties);
	}

	@Test
	void testToMailboxDtos() {
		when(mockSkatteverketProperties.supportedSuppliers()).thenReturn(List.of("Kivra"));

		final var response = createIsReachableResponse("2120002441", false, true, "Kivra", "https://somewhere.com");

		final var mailboxSettings = mapper.toMailboxDtos(response);

		assertThat(mailboxSettings).hasSize(1);
		assertThat(mailboxSettings.getFirst().getServiceAddress()).isEqualTo("https://somewhere.com");
		assertThat(mailboxSettings.getFirst().getRecipientId()).isEqualTo("2120002441");
		assertThat(mailboxSettings.getFirst().getServiceName()).isEqualTo("Kivra");
		assertThat(mailboxSettings.getFirst().isValidMailbox()).isTrue();
		assertThat(mailboxSettings.getFirst().getReason()).isNull();

		verify(mockSkatteverketProperties).supportedSuppliers();
	}

	@Test
	void testFindMatchingSupplier() {
		when(mockSkatteverketProperties.supportedSuppliers()).thenReturn(List.of("supplier1", "supplier2"));

		assertThat(mapper.isSupportedSupplier("supplier1")).isTrue();
		assertThat(mapper.isSupportedSupplier("supplier2")).isTrue();
		assertThat(mapper.isSupportedSupplier("unknownSupplier")).isFalse();

		verify(mockSkatteverketProperties, times(3)).supportedSuppliers();
	}

	@Test
	void testFindShortSupplierNameShouldMapAndReturnShortName() {
		when(mockSkatteverketProperties.supportedSuppliers()).thenReturn(List.of("Supplier1", "SuPPliEr2"));

		assertThat(mapper.getShortSupplierName("Supplier1")).isEqualTo("supplier1");
		assertThat(mapper.getShortSupplierName("SuPPliEr2")).isEqualTo("supplier2");

		verify(mockSkatteverketProperties, times(2)).supportedSuppliers();
	}

	@Test
	void testToMailboxDtosWhenEmptyResponse() {
		final var response = new IsReachableResponse();
		final var mailboxSettings = mapper.toMailboxDtos(response);

		assertThat(mailboxSettings).isEmpty();

		verifyNoInteractions(mockSkatteverketProperties);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("invalidMailboxProvider")
	void testInvalidateMailboxScenarios(final String testName, final IsReachableResponse response,
		final String expectedReason, final boolean usesMock) {

		if (usesMock) {
			when(mockSkatteverketProperties.supportedSuppliers()).thenReturn(List.of("Kivra"));
		}

		final var mailboxSettings = mapper.toMailboxDtos(response);

		assertThat(mailboxSettings).hasSize(1);
		assertThat(mailboxSettings.getFirst())
			.satisfies(mailbox -> {
				assertThat(mailbox.isValidMailbox()).isFalse();
				assertThat(mailbox.getReason()).isEqualTo(expectedReason);
			});

		if (usesMock) {
			verify(mockSkatteverketProperties).supportedSuppliers();
		} else {
			verifyNoInteractions(mockSkatteverketProperties);
		}
	}

	private static Stream<Arguments> invalidMailboxProvider() {
		return Stream.of(
			Arguments.of(
				"Sender not accepted",
				createIsReachableResponse("2120002441", false, false, "Kivra", "https://somewhere.com"),
				"Sender not accepted by recipient",
				false),
			Arguments.of(
				"Mailbox pending",
				createIsReachableResponse("2120002441", true, true, "Kivra", "https://somewhere.com"),
				"Mailbox is pending activation",
				false),
			Arguments.of(
				"No service supplier",
				createIsReachableResponse("2120002441", false, true, null, null),
				"No service supplier available",
				false),
			Arguments.of(
				"Recipient not an adult",
				createIsReachableResponse(generateMinorPersonnummer(), false, true, "Kivra", "https://somewhere.com"),
				"Recipient is not an adult",
				false),
			Arguments.of(
				"Unsupported supplier",
				createIsReachableResponse("2120002441", false, true, "UnsupportedSupplier", "https://somewhere.com"),
				"Unsupported service supplier",
				true),
			Arguments.of(
				"Blank service address",
				createIsReachableResponse("2120002441", false, true, "Kivra", ""),
				"Service address is blank",
				true));
	}

	private static String generateMinorPersonnummer() {
		final var birthDate = LocalDate.now().minusYears(13);
		final var formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
		return birthDate.format(formatter) + "1234";  // YYYYMMDD + serial number
	}

	private static IsReachableResponse createIsReachableResponse(final String recipientId, final boolean isPending, final boolean isSenderAccepted,
		final String supplierName, final String serviceAddress) {

		final var accountStatus = new AccountStatus();
		accountStatus.setRecipientId(recipientId);
		accountStatus.setPending(isPending);

		if (supplierName != null) {
			final var serviceSupplier = new ServiceSupplier();
			serviceSupplier.setId("165568402266");
			serviceSupplier.setName(supplierName);
			serviceSupplier.setServiceAdress(serviceAddress);

			accountStatus.setServiceSupplier(serviceSupplier);
		}

		final var reachabilityStatus = new ReachabilityStatus();
		reachabilityStatus.setSenderAccepted(isSenderAccepted);
		reachabilityStatus.setAccountStatus(accountStatus);

		final var response = new IsReachableResponse();
		response.getReturns().add(reachabilityStatus);

		return response;
	}
}
