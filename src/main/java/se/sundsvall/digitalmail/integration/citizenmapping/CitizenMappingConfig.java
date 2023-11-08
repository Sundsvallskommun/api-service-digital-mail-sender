package se.sundsvall.digitalmail.integration.citizenmapping;

import java.util.concurrent.TimeUnit;

import org.springframework.cloud.openfeign.FeignBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

import se.sundsvall.dept44.configuration.feign.FeignConfiguration;
import se.sundsvall.dept44.configuration.feign.FeignMultiCustomizer;
import se.sundsvall.dept44.configuration.feign.decoder.ProblemErrorDecoder;

import feign.Request;
import feign.codec.ErrorDecoder;

@Import(FeignConfiguration.class)
class CitizenMappingConfig {
    
    private final CitizenMappingProperties properties;
    
    CitizenMappingConfig(final CitizenMappingProperties properties) {
        this.properties = properties;
    }
    
    @Bean
    FeignBuilderCustomizer feignBuilderCustomizer() {
        return FeignMultiCustomizer.create()
            .withErrorDecoder(errorDecoder())
            .withRequestOptions(feignOptions())
            .withRetryableOAuth2InterceptorForClientRegistration(clientRegistration())
            .composeCustomizersToOne();
    }

    private ClientRegistration clientRegistration() {
        return ClientRegistration.withRegistrationId("citizenmapping")
            .tokenUri(properties.oauth2().tokenUrl())
            .clientId(properties.oauth2().clientId())
            .clientSecret(properties.oauth2().clientSecret())
            .authorizationGrantType(new AuthorizationGrantType(properties.oauth2().authorizationGrantType()))
            .build();
    }

    private Request.Options feignOptions() {
        return new Request.Options(
            properties.connectTimeout().toMillis(), TimeUnit.MILLISECONDS,
            properties.readTimeout().toMillis(), TimeUnit.MILLISECONDS,
            true);
    }

    private ErrorDecoder errorDecoder() {
        return new ProblemErrorDecoder("CitizenMappingClient");
    }
}
