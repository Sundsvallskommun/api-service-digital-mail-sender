package se.sundsvall.digitalmail.integration.kivra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.ResponseEntity.ok;
import static se.sundsvall.digitalmail.TestObjectFactory.generateInvoiceRequest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import generated.com.kivra.ContentUserV2;

@ExtendWith(MockitoExtension.class)
class KivraIntegrationTests {

	@Mock
	private KivraClient mockClient;

	@InjectMocks
	private KivraIntegration kivraIntegration;

	private final InvoiceDto invoiceDto = new InvoiceDto(generateInvoiceRequest());

	@Test
	void sendContent() {
		when(mockClient.postContent(any(ContentUserV2.class))).thenReturn(ok(new ContentUserV2()));

		final var result = kivraIntegration.sendInvoice(invoiceDto);

		assertThat(result).isTrue();

		verify(mockClient, times(1)).postContent(any(ContentUserV2.class));
	}
}
