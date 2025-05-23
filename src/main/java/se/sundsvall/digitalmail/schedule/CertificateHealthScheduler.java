package se.sundsvall.digitalmail.schedule;

import static java.lang.Boolean.TRUE;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.util.Collections.emptyList;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static se.sundsvall.digitalmail.integration.messaging.MessagingMapper.toEmailRequest;
import static se.sundsvall.digitalmail.integration.messaging.MessagingMapper.toSlackRequest;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import se.sundsvall.dept44.scheduling.Dept44Scheduled;
import se.sundsvall.dept44.scheduling.health.Dept44HealthUtility;
import se.sundsvall.digitalmail.integration.kivra.KivraIntegration;
import se.sundsvall.digitalmail.integration.messaging.MessagingIntegration;

@Component
public class CertificateHealthScheduler {
	private static final Logger LOGGER = LoggerFactory.getLogger(CertificateHealthScheduler.class);
	private static final String EMPTY_STRING = "";
	private static final String SUNDSVALL_MUNICIPALITY_ID = "2281";
	private static final String HEALTH_MESSAGE = "A potential certificate issue has been detected and needs to be investigated";
	private static final String MAIL_BODY = "Ett potentiellt problem med tjänstens aktiva certifikat mot Kivra har upptäckts %s och behöver utredas och ev åtgärdas ASAP.%n%nMed vänlig hälsning%nDigitalMailSender";
	private static final DateTimeFormatter DATE_AND_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

	private final KivraIntegration kivraIntegration;
	private final MessagingIntegration messagingIntegation;
	private final NotificationProperties notificationProperties;
	private final Consumer<Boolean> certificateHealthConsumer;
	private final AtomicBoolean sendNotification = new AtomicBoolean(true);

	@Value("${scheduler.certificate-health.name}")
	private String schedulerName;

	public CertificateHealthScheduler(
		final KivraIntegration kivraIntegration,
		final MessagingIntegration messagingIntegration,
		final NotificationProperties notificationProperties,
		final Dept44HealthUtility dept44HealthUtility) {

		this.kivraIntegration = kivraIntegration;
		this.messagingIntegation = messagingIntegration;
		this.notificationProperties = notificationProperties;

		this.certificateHealthConsumer = certificateHealthy -> {
			if (TRUE.equals(certificateHealthy)) {
				dept44HealthUtility.setHealthIndicatorHealthy(schedulerName);
				sendNotification.set(true); // Reset notification signal if indicator is considered to be healthy
			} else {
				LOGGER.warn(HEALTH_MESSAGE);
				dept44HealthUtility.setHealthIndicatorUnhealthy(schedulerName, HEALTH_MESSAGE);
			}
		};
	}

	@Dept44Scheduled(
		name = "${scheduler.certificate-health.name}",
		cron = "${scheduler.certificate-health.cron:-}",
		lockAtMostFor = "${scheduler.certificate-health.lock-at-most-for}",
		maximumExecutionTime = "${scheduler.certificate-health.maximum-execution-time}")
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

				if (sendNotification.get()) { // Send slack and mail notification the first time the problem is discovered
					sendNotifications();

					sendNotification.set(false);
				}
			}
		}
	}

	private void sendNotifications() {
		if (nonNull(notificationProperties.mail())) {
			ofNullable(notificationProperties.mail().recipients()).orElse(emptyList())
				.stream()
				.forEach(this::sendMail);
		}

		if (nonNull(notificationProperties.slack())) {
			sendSlack();
		}
	}

	private void sendMail(final String recipient) {
		final var time = LocalDateTime.now().truncatedTo(MINUTES);

		ofNullable(toEmailRequest(
			recipient,
			notificationProperties.mail().subject(),
			MAIL_BODY.formatted(time.format(DATE_AND_TIME_FORMAT)),
			notificationProperties.mail().sender().emailAddress(),
			notificationProperties.mail().sender().name()))
			.ifPresent(request -> messagingIntegation.sendEmail(SUNDSVALL_MUNICIPALITY_ID, request));
	}

	private void sendSlack() {
		ofNullable(
			toSlackRequest(
				notificationProperties.slack().channel(),
				notificationProperties.slack().message(),
				notificationProperties.slack().token()))
			.ifPresent(request -> messagingIntegation.sendSlack(SUNDSVALL_MUNICIPALITY_ID, request));
	}
}
