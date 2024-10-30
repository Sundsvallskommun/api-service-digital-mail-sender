package se.sundsvall.digitalmail.integration.skatteverket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

import jakarta.xml.soap.MessageFactory;
import jakarta.xml.soap.SOAPException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ws.context.MessageContext;
import org.springframework.ws.soap.SoapMessage;
import org.springframework.ws.soap.saaj.SaajSoapMessage;
import org.zalando.problem.ThrowableProblem;

class SkatteverketClientConfigTests {

	@Nested
	@ExtendWith(MockitoExtension.class)
	class SoapMessageSizeInterceptorTest {

		@Mock
		private MessageContext mockContext;

		private SoapMessage soapMessage;

		private SkatteverketClientConfig.SoapMessageSizeInterceptor interceptor;

		@BeforeEach
		public void setup() throws SOAPException {
			// Create a simple SOAP message
			soapMessage = new SaajSoapMessage(MessageFactory.newInstance().createMessage());
		}

		@Test
		void testCorrectSize_shouldNotThrowException() {
			interceptor = new SkatteverketClientConfig.SoapMessageSizeInterceptor(200000L);

			when(mockContext.getRequest()).thenReturn(soapMessage);

			final var result = interceptor.handleRequest(mockContext);

			assertThat(result).isTrue();
		}

		@Test
		void testTooBigMessage_shouldThrowException() {
			interceptor = new SkatteverketClientConfig.SoapMessageSizeInterceptor(0L);

			when(mockContext.getRequest()).thenReturn(soapMessage);

			assertThatExceptionOfType(ThrowableProblem.class)
				.isThrownBy(() -> interceptor.handleRequest(mockContext))
				.withMessage("Message is too big to be sent as a digital mail.: Size is: 132 bytes. Max allowed is: 0 bytes.");
		}
	}
}
