package com.liquilabs.vankoo.iam;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class VankooIamServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(VankooIamServiceApplication.class, args);
    }

}
