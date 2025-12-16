package se.sundsvall.digitalmail.api.model;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

class MailboxTest {

	@Test
	void testBean() {
		MatcherAssert.assertThat(Mailbox.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanToString(),
			hasValidBeanEquals(),
			hasValidBeanHashCode()));
	}

	@Test
	void testBuilderMethods() {
		final var partyId = "12345";
		final var supplier = "Kivra";
		final var reachable = true;
		final var reason = "Some reason";

		final var mailbox = Mailbox.builder()
			.withPartyId(partyId)
			.withSupplier(supplier)
			.withReachable(reachable)
			.withReason(reason)
			.build();

		assertThat(mailbox).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(mailbox.getPartyId()).isEqualTo(partyId);
		assertThat(mailbox.getSupplier()).isEqualTo(supplier);
		assertThat(mailbox.isReachable()).isEqualTo(reachable);
		assertThat(mailbox.getReason()).isEqualTo(reason);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(new Mailbox()).isNotNull().hasAllNullFieldsOrPropertiesExcept("reachable");
		assertThat(Mailbox.builder().build()).hasAllNullFieldsOrPropertiesExcept("reachable");
	}
}
