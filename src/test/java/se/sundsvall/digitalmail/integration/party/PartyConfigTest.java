package se.sundsvall.digitalmail.integration.party;

import feign.Request;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.openfeign.FeignBuilderCustomizer;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import se.sundsvall.dept44.configuration.feign.FeignMultiCustomizer;
import se.sundsvall.dept44.configuration.feign.decoder.ProblemErrorDecoder;
import se.sundsvall.digitalmail.integration.OAuth2;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static se.sundsvall.digitalmail.integration.party.PartyConfig.INTEGRATION_NAME;

@ExtendWith(MockitoExtension.class)
class PartyConfigTest {

	@Mock
	private ClientRegistrationRepository clientRegistrationRepositoryMock;

	@Spy
	private FeignMultiCustomizer feignMultiCustomizerSpy;

	@Mock
	private FeignBuilderCustomizer feignBuilderCustomizerMock;

	@Mock
	private PartyProperties propertiesMock;

	@Mock
	private OAuth2 oAuth2Mock;

	@Captor
	private ArgumentCaptor<ClientRegistration> clientRegistrationCaptor;

	@Captor
	private ArgumentCaptor<Request.Options> requestOptionsCaptor;

	@Captor
	private ArgumentCaptor<ProblemErrorDecoder> problemErrorDecoderCaptor;

	@Test
	void testFeignBuilderCustomizer() {

		// Arrange
		final var configuration = new PartyConfig(propertiesMock);
		when(propertiesMock.connectTimeout()).thenReturn(Duration.ofSeconds(3));
		when(propertiesMock.readTimeout()).thenReturn(Duration.ofSeconds(4));
		when(propertiesMock.oauth2()).thenReturn(oAuth2Mock);
		when(oAuth2Mock.authorizationGrantType()).thenReturn("client_credentials");
		when(oAuth2Mock.clientId()).thenReturn("clientId");
		when(oAuth2Mock.clientSecret()).thenReturn("clientSecret");
		when(oAuth2Mock.tokenUrl()).thenReturn("tokenUrl");
		when(feignMultiCustomizerSpy.composeCustomizersToOne()).thenReturn(feignBuilderCustomizerMock);

		try (final MockedStatic<FeignMultiCustomizer> feignMultiCustomizerMock = Mockito.mockStatic(FeignMultiCustomizer.class)) {
			feignMultiCustomizerMock.when(FeignMultiCustomizer::create).thenReturn(feignMultiCustomizerSpy);

			// Act
			final var customizer = configuration.feignBuilderCustomizer();

			// Assert and verify
			verify(feignMultiCustomizerSpy).withErrorDecoder(problemErrorDecoderCaptor.capture());
			verify(feignMultiCustomizerSpy).withRetryableOAuth2InterceptorForClientRegistration(clientRegistrationCaptor.capture());
			verify(propertiesMock).connectTimeout();
			verify(propertiesMock).readTimeout();
			verify(feignMultiCustomizerSpy).withRequestOptions(requestOptionsCaptor.capture());
			verify(feignMultiCustomizerSpy).composeCustomizersToOne();

			assertThat(clientRegistrationCaptor.getValue().getAuthorizationGrantType().getValue()).isEqualTo("client_credentials");
			assertThat(clientRegistrationCaptor.getValue().getClientId()).isEqualTo("clientId");
			assertThat(clientRegistrationCaptor.getValue().getClientSecret()).isEqualTo("clientSecret");
			assertThat(clientRegistrationCaptor.getValue().getProviderDetails().getTokenUri()).isEqualTo("tokenUrl");
			assertThat(problemErrorDecoderCaptor.getValue()).hasFieldOrPropertyWithValue("integrationName", INTEGRATION_NAME);
			assertThat(requestOptionsCaptor.getValue().connectTimeout()).isEqualTo(3000);
			assertThat(requestOptionsCaptor.getValue().connectTimeoutUnit()).isEqualTo(MILLISECONDS);
			assertThat(requestOptionsCaptor.getValue().readTimeout()).isEqualTo(4000);
			assertThat(requestOptionsCaptor.getValue().readTimeoutUnit()).isEqualTo(MILLISECONDS);
			assertThat(customizer).isSameAs(feignBuilderCustomizerMock);
		}
	}

}
