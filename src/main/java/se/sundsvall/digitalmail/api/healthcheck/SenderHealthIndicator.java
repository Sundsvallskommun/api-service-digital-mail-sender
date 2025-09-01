package se.sundsvall.digitalmail.api.healthcheck;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Simple health indicator to show if there are valid senders configured.
 */
@Component("senderConfiguration")
public class SenderHealthIndicator implements HealthIndicator {

	private Health health;

	private static final String NAME = "Supported sender configuration";

	public SenderHealthIndicator() {
		this.health = Health.unknown().withDetail(NAME, "never checked").build();
	}

	@Override
	public Health health() {
		return health;
	}

	public void setHealthy() {
		this.health = Health.up()
			.withDetail(NAME, "Configuration present")
			.build();
	}

	public void setUnhealthy() {
		this.health = Health.down()
			.withDetail(NAME, "No configuration present")
			.build();
	}
}
