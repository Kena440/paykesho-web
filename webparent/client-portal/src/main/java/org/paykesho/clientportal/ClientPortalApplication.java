package org.paykesho.clientportal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication
@EntityScan({"org.commons.entity"})
public class ClientPortalApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClientPortalApplication.class, args);
    }

}
