package org.paykesho.clientportal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan({"org.commons.entity", "org.paykesho.clientportal"})
@EnableJpaRepositories(basePackages = {"org.paykesho.clientportal"})
public class ClientPortalApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClientPortalApplication.class, args);
    }

}
