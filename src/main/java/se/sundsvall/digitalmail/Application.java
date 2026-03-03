package se.sundsvall.digitalmail;

import java.security.Security;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import se.sundsvall.dept44.ServiceApplication;

import static org.springframework.boot.SpringApplication.run;

@ServiceApplication
@EnableFeignClients
@ConfigurationPropertiesScan("se.sundsvall.digitalmail")
public class Application {

	static void main(final String[] args) {
		run(Application.class, args);

		// Set unlimited strength for cryptographic functions.
		Security.setProperty("crypto.policy", "unlimited");
	}
}
