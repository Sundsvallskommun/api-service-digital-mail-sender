package se.sundsvall.digitalmail.integration.kivra;

import static java.util.Collections.emptySet;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Request;
import generated.com.kivra.ContentUserContextInvoice;
import generated.com.kivra.Payment;
import org.springframework.cloud.openfeign.FeignBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import se.sundsvall.dept44.configuration.feign.FeignConfiguration;
import se.sundsvall.dept44.configuration.feign.decoder.JsonPathErrorDecoder;
import se.sundsvall.dept44.configuration.feign.interceptor.OAuth2RequestInterceptor;
import se.sundsvall.dept44.configuration.feign.retryer.ActionRetryer;
import se.sundsvall.digitalmail.integration.kivra.support.ContentUserContextInvoiceMixin;
import se.sundsvall.digitalmail.integration.kivra.support.PaymentMixin;

@Import(FeignConfiguration.class)
class KivraConfig {

	static final String INTEGRATION_NAME = "Kivra";

	private final KivraProperties properties;

	KivraConfig(final KivraProperties properties,
		final ObjectMapper objectMapper) {
		this.properties = properties;

		objectMapper.addMixIn(ContentUserContextInvoice.class, ContentUserContextInvoiceMixin.class);
		objectMapper.addMixIn(Payment.class, PaymentMixin.class);
	}

	@Bean
	FeignBuilderCustomizer feignBuilderCustomizer() {
		return builder -> {
			final var clientRegistration = ClientRegistration.withRegistrationId(INTEGRATION_NAME)
				.tokenUri(properties.oauth2().tokenUrl())
				.clientId(properties.oauth2().clientId())
				.clientSecret(properties.oauth2().clientSecret())
				.authorizationGrantType(new AuthorizationGrantType(properties.oauth2().authorizationGrantType()))
				.build();
			final var oAuth2RequestInterceptor = new OAuth2RequestInterceptor(clientRegistration, emptySet());

			builder.requestInterceptor(oAuth2RequestInterceptor)
				.retryer(new ActionRetryer(oAuth2RequestInterceptor::removeToken, 1))
				.errorDecoder(new JsonPathErrorDecoder(INTEGRATION_NAME, new JsonPathErrorDecoder.JsonPathSetup("$.long_message")))
				.options(feignOptions());
		};
	}

	private Request.Options feignOptions() {
		return new Request.Options(
			properties.connectTimeout().toMillis(), MILLISECONDS,
			properties.readTimeout().toMillis(), MILLISECONDS,
			true);
	}
}
