package org.chaz.backoffice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EntityScan({"org.commons.entity", "org.chaz.backoffice.user"})
//@ComponentScan(basePackages = {"org.chaz.backoffice.security","org.chaz.backoffice.user"})
public class BackOfficeApplication {
    public static void main(String[] args) {
        SpringApplication.run(BackOfficeApplication.class, args);
    }
}
