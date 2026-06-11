package se.sundsvall.digitalmail.integration.kivra;

import feign.Feign;
import feign.Request;
import feign.RequestInterceptor;
import feign.Retryer;
import feign.codec.ErrorDecoder;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.dept44.configuration.feign.decoder.JsonPathErrorDecoder;
import se.sundsvall.dept44.configuration.feign.interceptor.OAuth2RequestInterceptor;
import se.sundsvall.dept44.configuration.feign.retryer.ActionRetryer;
import se.sundsvall.digitalmail.integration.OAuth2;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static se.sundsvall.digitalmail.integration.kivra.KivraConfig.INTEGRATION_NAME;

@ExtendWith(MockitoExtension.class)
class KivraConfigTest {

	@Mock
	private KivraProperties propertiesMock;

	@Mock
	private OAuth2 oAuth2Mock;

	@Mock
	private Feign.Builder builderMock;

	@Captor
	private ArgumentCaptor<RequestInterceptor> requestInterceptorCaptor;

	@Captor
	private ArgumentCaptor<Retryer> retryerCaptor;

	@Captor
	private ArgumentCaptor<ErrorDecoder> errorDecoderCaptor;

	@Captor
	private ArgumentCaptor<Request.Options> requestOptionsCaptor;

	@Test
	void testFeignBuilderCustomizer() {

		// Arrange
		final var configuration = new KivraConfig(propertiesMock);
		when(propertiesMock.oauth2()).thenReturn(oAuth2Mock);
		when(oAuth2Mock.tokenUrl()).thenReturn("tokenUrl");
		when(oAuth2Mock.clientId()).thenReturn("clientId");
		when(oAuth2Mock.clientSecret()).thenReturn("clientSecret");
		when(oAuth2Mock.authorizationGrantType()).thenReturn("client_credentials");
		when(propertiesMock.connectTimeout()).thenReturn(Duration.ofSeconds(1));
		when(propertiesMock.readTimeout()).thenReturn(Duration.ofSeconds(2));

		when(builderMock.requestInterceptor(any())).thenReturn(builderMock);
		when(builderMock.retryer(any())).thenReturn(builderMock);
		when(builderMock.errorDecoder(any())).thenReturn(builderMock);
		when(builderMock.options(any())).thenReturn(builderMock);

		// Act
		final var customizer = configuration.feignBuilderCustomizer();
		customizer.customize(builderMock);

		// Assert and verify
		verify(builderMock).requestInterceptor(requestInterceptorCaptor.capture());
		verify(builderMock).retryer(retryerCaptor.capture());
		verify(builderMock).errorDecoder(errorDecoderCaptor.capture());
		verify(builderMock).options(requestOptionsCaptor.capture());

		assertThat(requestInterceptorCaptor.getValue()).isInstanceOf(OAuth2RequestInterceptor.class);
		assertThat(retryerCaptor.getValue()).isInstanceOf(ActionRetryer.class);
		assertThat(errorDecoderCaptor.getValue()).isInstanceOfSatisfying(JsonPathErrorDecoder.class,
			decoder -> assertThat(decoder).hasFieldOrPropertyWithValue("integrationName", INTEGRATION_NAME));
		assertThat(requestOptionsCaptor.getValue().connectTimeout()).isEqualTo(1000);
		assertThat(requestOptionsCaptor.getValue().connectTimeoutUnit()).isEqualTo(MILLISECONDS);
		assertThat(requestOptionsCaptor.getValue().readTimeout()).isEqualTo(2000);
		assertThat(requestOptionsCaptor.getValue().readTimeoutUnit()).isEqualTo(MILLISECONDS);
	}
}
