package se.sundsvall.digitalmail.integration.minameddelanden.configuration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SoapMessageSizeInterceptorTest {

	@Test
	void checkSizeOfMessageTest() {
		var soapMessageSizeInterceptor = new MinaMeddelandenClientFactory.SoapMessageSizeInterceptor(10);
		var outputStreamMock = Mockito.mock(ByteArrayOutputStream.class);

		when(outputStreamMock.toString(StandardCharsets.UTF_8)).thenReturn("12345678901");

		assertThatThrownBy(() -> soapMessageSizeInterceptor.checkSizeOfMessage(outputStreamMock))
			.hasMessage("Message is too big to be sent as a digital mail.: Size is: 11 bytes. Max allowed is: 10 bytes.");
	}

}
