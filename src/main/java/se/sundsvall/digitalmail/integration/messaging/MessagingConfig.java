package se.sundsvall.digitalmail.integration.messaging;

import feign.Request;
import org.springframework.cloud.openfeign.FeignBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import se.sundsvall.dept44.configuration.feign.FeignConfiguration;
import se.sundsvall.dept44.configuration.feign.FeignMultiCustomizer;
import se.sundsvall.dept44.configuration.feign.decoder.ProblemErrorDecoder;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

@Import(FeignConfiguration.class)
public class MessagingConfig {

	static final String INTEGRATION_NAME = "messagingClient";

	private final MessagingProperties properties;

	MessagingConfig(MessagingProperties properties) {
		this.properties = properties;
	}

	@Bean
	FeignBuilderCustomizer feignBuilderCustomizer() {
		return FeignMultiCustomizer.create()
			.withErrorDecoder(new ProblemErrorDecoder(INTEGRATION_NAME))
			.withRequestOptions(feignOptions())
			.withRetryableOAuth2InterceptorForClientRegistration(clientRegistration())
			.composeCustomizersToOne();
	}

	private ClientRegistration clientRegistration() {
		return ClientRegistration.withRegistrationId(INTEGRATION_NAME)
			.tokenUri(properties.oauth2().tokenUrl())
			.clientId(properties.oauth2().clientId())
			.clientSecret(properties.oauth2().clientSecret())
			.authorizationGrantType(new AuthorizationGrantType(properties.oauth2().authorizationGrantType()))
			.build();
	}

	private Request.Options feignOptions() {
		return new Request.Options(
			properties.connectTimeout().toMillis(), MILLISECONDS,
			properties.readTimeout().toMillis(), MILLISECONDS,
			true);
	}
}
