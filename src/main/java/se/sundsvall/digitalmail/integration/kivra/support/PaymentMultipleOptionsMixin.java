package se.sundsvall.digitalmail.integration.kivra.support;

import com.fasterxml.jackson.annotation.JsonIgnore;
import generated.com.kivra.PaymentMultipleOptions;
import org.springframework.boot.jackson.JacksonMixin;

@JacksonMixin(PaymentMultipleOptions.class)
public interface PaymentMultipleOptionsMixin {

	@JsonIgnore
	PaymentMultipleOptions.StatusEnum getStatus();
}
