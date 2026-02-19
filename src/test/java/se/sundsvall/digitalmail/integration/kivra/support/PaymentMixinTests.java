package se.sundsvall.digitalmail.integration.kivra.support;

import com.fasterxml.jackson.core.JsonGenerator;
import java.io.IOException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;

class PaymentMixinTests {

	@Nested
	@ExtendWith(MockitoExtension.class)
	class FloatToStringSerializerTests {

		private final PaymentMixin.FloatToStringSerializer serializer = new PaymentMixin.FloatToStringSerializer();
		@Mock
		private JsonGenerator mockJsonGenerator;

		@Test
		void serialize() throws IOException {
			serializer.serialize(2.34f, mockJsonGenerator, null);

			verify(mockJsonGenerator).writeString(anyString());
		}
	}
}
