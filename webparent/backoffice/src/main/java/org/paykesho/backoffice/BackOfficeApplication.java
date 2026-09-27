package org.paykesho.backoffice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "org.paykesho.backoffice.adminauth")
@EntityScan(basePackages = "org.paykesho.backoffice.adminauth.entity")
@EnableJpaRepositories(basePackages = "org.paykesho.backoffice.adminauth.repository")
public class BackOfficeApplication {
    public static void main(String[] args) {
        SpringApplication.run(BackOfficeApplication.class, args);
    }
}
