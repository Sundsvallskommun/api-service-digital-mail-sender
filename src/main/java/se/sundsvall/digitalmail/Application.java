package se.sundsvall.digitalmail;

import static org.springframework.boot.SpringApplication.run;

import java.security.Security;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import se.sundsvall.dept44.ServiceApplication;

@ServiceApplication
@EnableFeignClients
@ConfigurationPropertiesScan("se.sundsvall.digitalmail")
@EnableCaching
public class Application {

	public static void main(String[] args) {
		run(Application.class, args);

		// Set unlimited strength for cryptographic functions.
		Security.setProperty("crypto.policy", "unlimited");
	}
}
