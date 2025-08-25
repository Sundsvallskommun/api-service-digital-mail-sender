package se.sundsvall.digitalmail.integration.skatteverket;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

class MailboxDtoTest {

	@Test
	void testBean() {
		MatcherAssert.assertThat(MailboxDto.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanToString(),
			hasValidBeanEquals(),
			hasValidBeanHashCode()));
	}

	@Test
	void testBuilderMethods() {
		final var recipientId = "12345";
		final var serviceAddress = "kivra.com";
		final var serviceName = "Kivra";
		final var validMailbox = true;

		final var mailbox = MailboxDto.builder()
			.withRecipientId(recipientId)
			.withServiceAddress(serviceAddress)
			.withServiceName(serviceName)
			.withValidMailbox(validMailbox)
			.build();

		assertThat(mailbox).isNotNull().hasNoNullFieldsOrProperties();
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(new MailboxDto()).isNotNull().hasAllNullFieldsOrPropertiesExcept("validMailbox");
		assertThat(MailboxDto.builder().build()).hasAllNullFieldsOrPropertiesExcept("validMailbox");
	}
}
