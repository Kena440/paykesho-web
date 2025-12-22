package org.paykesho.organizationportal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;

@SpringBootApplication(exclude = {OAuth2ClientAutoConfiguration.class})
@EntityScan({"org.commons.entity"})
public class OrganizationPortalApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrganizationPortalApplication.class, args);
    }

}
