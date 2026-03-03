package com.liquilabs.vankoo.iam;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
@EnableDiscoveryClient
public class VankooIamServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(VankooIamServiceApplication.class, args);
    }

}
