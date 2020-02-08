package sen.cloud.project;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

@ServletComponentScan
@SpringBootApplication
public class AwsParseInvoiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AwsParseInvoiceApplication.class, args);
	}

}
