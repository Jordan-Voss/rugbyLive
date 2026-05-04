package dev.jordanvoss.rugbylive;

import dev.jordanvoss.rugbylive.provider.apisports.ApiSportsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(ApiSportsProperties.class)
public class RugbyliveApplication {

	public static void main(String[] args) {
		SpringApplication.run(RugbyliveApplication.class, args);
	}

}
