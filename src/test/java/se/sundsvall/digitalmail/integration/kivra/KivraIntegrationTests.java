package se.sundsvall.digitalmail.integration.kivra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.ResponseEntity.ok;
import static se.sundsvall.digitalmail.TestObjectFactory.generateInvoiceRequest;

import generated.com.kivra.ContentUserV2;
import generated.com.kivra.UserMatchV2SSN;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KivraIntegrationTests {

	private final InvoiceDto invoiceDto = new InvoiceDto(generateInvoiceRequest());
	@Mock
	private KivraClient mockClient;

	@InjectMocks
	private KivraIntegration kivraIntegration;

	@Test
	void sendInvoice() {
		when(mockClient.postContent(any(ContentUserV2.class))).thenReturn(ok(new ContentUserV2()));

		final var result = kivraIntegration.sendInvoice(invoiceDto);

		assertThat(result).isTrue();

		verify(mockClient, times(1)).postContent(any(ContentUserV2.class));
	}

	/**
	 * Test that the method returns true when the legalId is returned from the Kivra client.
	 */
	@Test
	void verifyValidRecipient_1() {
		final var legalId = "19990101-1234";
		when(mockClient.postUserMatchSSN(any(UserMatchV2SSN.class))).thenReturn(ok(new UserMatchV2SSN().addListItem(legalId)));

		final var result = kivraIntegration.verifyValidRecipient(legalId);

		assertThat(result).isTrue();
		verify(mockClient).postUserMatchSSN(any(UserMatchV2SSN.class));
	}

	/**
	 * Test that the method returns false when the legalId is not returned from the Kivra client.
	 */
	@Test
	void verifyValidRecipient_2() {
		final var legalId = "19990101-1234";
		when(mockClient.postUserMatchSSN(any(UserMatchV2SSN.class))).thenReturn(ok(new UserMatchV2SSN()));

		final var result = kivraIntegration.verifyValidRecipient(legalId);

		assertThat(result).isFalse();
		verify(mockClient).postUserMatchSSN(any(UserMatchV2SSN.class));
	}
}
