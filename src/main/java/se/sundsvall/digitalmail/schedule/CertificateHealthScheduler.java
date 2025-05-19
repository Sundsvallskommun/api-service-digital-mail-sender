package se.sundsvall.digitalmail.schedule;

import static java.lang.Boolean.TRUE;
import static java.util.Optional.ofNullable;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import se.sundsvall.dept44.scheduling.Dept44Scheduled;
import se.sundsvall.dept44.scheduling.health.Dept44HealthUtility;
import se.sundsvall.digitalmail.integration.kivra.KivraIntegration;

@Component
public class CertificateHealthScheduler {
	private static final String EMPTY_STRING = "";
	private static final String HEALTH_MESSAGE = "A potential certificate issue has been detected and needs to be investigated";
	private static final Logger LOGGER = LoggerFactory.getLogger(CertificateHealthScheduler.class);
	private static final AtomicBoolean SEND_NOTIFICATION = new AtomicBoolean(true);

	private final KivraIntegration kivraIntegration;
	private final Consumer<Boolean> certificateHealthConsumer;

	@Value("${scheduler.certificateHealth.name}")
	private String schedulerName;

	public CertificateHealthScheduler(final KivraIntegration kivraIntegration, final Dept44HealthUtility dept44HealthUtility) {
		this.kivraIntegration = kivraIntegration;
		this.certificateHealthConsumer = certificateHealthy -> {
			if (TRUE.equals(certificateHealthy)) {
				dept44HealthUtility.setHealthIndicatorHealthy(schedulerName);
				SEND_NOTIFICATION.set(true); // Reset notification signal if indicator is considered to be healthy
			} else {
				LOGGER.warn(HEALTH_MESSAGE);
				dept44HealthUtility.setHealthIndicatorUnhealthy(schedulerName, HEALTH_MESSAGE);
			}
		};
	}

	@Dept44Scheduled(
		name = "${scheduler.certificateHealth.name}",
		cron = "${scheduler.certificateHealth.cron:-}",
		lockAtMostFor = "${scheduler.certificateHealth.lockAtMostFor}",
		maximumExecutionTime = "${scheduler.certificateHealth.maximumExecutionTime}")
	public void execute() {
		try {
			// Make a call to verify that certificate is valid
			kivraIntegration.healthCheck();

			// Set certificate health indicator to healthy as no exception has occured
			certificateHealthConsumer.accept(true);

		} catch (final Exception e) {
			// Set health indicator to unhealthy if exception that indicates certificate problem is thrown when using Kivra enpoint
			if (ofNullable(e.getMessage()).orElse(EMPTY_STRING).contains("[invalid_token_response]")) {
				certificateHealthConsumer.accept(false);

				if (SEND_NOTIFICATION.get()) { // Send slack and mail notification the first time the problem is discovered

					// TODO: Implement logic for sending slack message and email to people that need to know that certificate is invalid

					SEND_NOTIFICATION.set(false);
				}
			}
		}
	}

}
