package pucp.pdds.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		// Set headless mode to false for GUI
		System.setProperty("java.awt.headless", "false");
		
		// Create the application
		SpringApplication app = new SpringApplication(BackendApplication.class);
		
		// Set default profile to dev if not specified
		if (System.getProperty("spring.profiles.active") == null) {
			System.setProperty("spring.profiles.active", "dev");
		}
		
		// Run the application
		app.run(args);
	}

}
