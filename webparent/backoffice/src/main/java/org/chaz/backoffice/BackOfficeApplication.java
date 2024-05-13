package org.chaz.backoffice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EntityScan({"org.membershipcommons.entity", "org.chaz.membershipbackoffice.user"})
//@ComponentScan(basePackages = {"org.chaz.membershipbackoffice.security","org.chaz.membershipbackoffice.user"})
public class MembershipBackOfficeApplication {
    public static void main(String[] args) {
        SpringApplication.run(MembershipBackOfficeApplication.class, args);
    }
}
