package neyan.tech.ni3ma_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties
public class Ni3maBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(Ni3maBackendApplication.class, args);
	}

}
