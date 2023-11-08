package se.sundsvall.digitalmail.integration.kivra.support;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

class PaymentMixinTests {

    @Nested
    @ExtendWith(MockitoExtension.class)
    class FloatToStringSerializerTests {

        @Mock
        private JsonGenerator mockJsonGenerator;

        private final PaymentMixin.FloatToStringSerializer serializer = new PaymentMixin.FloatToStringSerializer();

        @Test
        void serialize() throws IOException {
            serializer.serialize(2.34f, mockJsonGenerator, null);

            verify(mockJsonGenerator, times(1)).writeString(any(String.class));
        }
    }
}
