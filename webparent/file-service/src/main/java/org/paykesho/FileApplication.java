package org.paykesho;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"org.paykesho", "org.commons"})
@EnableJpaRepositories(basePackages = {"org.paykesho.fileservice.repository", "org.paykesho.fileservice.notification.repository"})
@EntityScan({"org.commons.entity", "org.paykesho.fileservice.entity"})
@EnableConfigurationProperties
public class FileApplication {
    public static void main(String[] args) {
        SpringApplication.run(FileApplication.class, args);
    }
}
