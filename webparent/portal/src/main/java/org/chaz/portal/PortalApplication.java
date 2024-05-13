package org.chaz.portal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication
@EntityScan({"org.membershipcommons.entity", "org.chaz.membershipbackoffice.user"})
public class MembershipSiteApplication {

    public static void main(String[] args) {
        SpringApplication.run(MembershipSiteApplication.class, args);
    }

}
