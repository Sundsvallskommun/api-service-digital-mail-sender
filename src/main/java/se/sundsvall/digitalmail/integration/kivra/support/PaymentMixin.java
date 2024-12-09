package se.sundsvall.digitalmail.integration.kivra.support;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.io.IOException;

public interface PaymentMixin {

	@JsonSerialize(using = FloatToStringSerializer.class)
	Float getTotalOwed();

	class FloatToStringSerializer extends JsonSerializer<Float> {

		@Override
		public void serialize(final Float value, final JsonGenerator jsonGenerator, final SerializerProvider serializerProvider) throws IOException {
			jsonGenerator.writeString(value.toString());
		}
	}
}
