package se.sundsvall.digitalmail.integration.kivra.support;

import generated.com.kivra.Payment;
import org.springframework.boot.jackson.JacksonMixin;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.annotation.JsonSerialize;

@JacksonMixin(Payment.class)
public interface PaymentMixin {

	@JsonSerialize(using = FloatToStringSerializer.class)
	Float getTotalOwed();

	class FloatToStringSerializer extends ValueSerializer<Float> {

		@Override
		public void serialize(final Float value, final JsonGenerator jsonGenerator, final SerializationContext serializationContext) {
			jsonGenerator.writeString(value.toString());
		}
	}
}
