package com.liquilabs.vankoo.iam.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration
@Slf4j
public class KafkaTestConfig {

    @Bean
    public Consumer<String> userCreatedConsumer() {
        return message -> {
            log.info("********** KAFKA RECIBIDO EN IAM **********");
            log.info("Mensaje: {}", message);
            log.info("*******************************************");
        };
    }
}