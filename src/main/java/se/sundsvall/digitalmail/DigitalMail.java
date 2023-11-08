package se.sundsvall.digitalmail;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import se.sundsvall.dept44.ServiceApplication;

import java.security.Security;

@ServiceApplication
@EnableFeignClients
@ConfigurationPropertiesScan("se.sundsvall.digitalmail")
@EnableCaching
public class DigitalMail {
    
    public static void main(String[] args) {
        SpringApplication.run(DigitalMail.class, args);
        
        //Set unlimited strength for cryptographic functions.
        Security.setProperty("crypto.policy", "unlimited");
    }
}
