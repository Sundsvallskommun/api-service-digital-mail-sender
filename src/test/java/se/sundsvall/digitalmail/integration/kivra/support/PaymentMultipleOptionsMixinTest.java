package se.sundsvall.digitalmail.integration.kivra.support;

import generated.com.kivra.PaymentMultipleOptions;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentMultipleOptionsMixinTest {

	@Test
	void statusIsNotSerialized() {
		final var jsonMapper = JsonMapper.builder()
			.addMixIn(PaymentMultipleOptions.class, PaymentMultipleOptionsMixin.class)
			.build();

		final Map<String, Object> result = jsonMapper.convertValue(new PaymentMultipleOptions().payable(true), new TypeReference<>() {});

		assertThat(result)
			.doesNotContainKey("status")
			.containsEntry("payable", true);
	}

	@Test
	void generatedModelHasStatusByDefault() {
		assertThat(new PaymentMultipleOptions().getStatus()).isEqualTo(PaymentMultipleOptions.StatusEnum.UNPAID);
	}
}
