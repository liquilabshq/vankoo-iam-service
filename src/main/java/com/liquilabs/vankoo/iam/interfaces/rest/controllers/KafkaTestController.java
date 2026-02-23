package com.liquilabs.vankoo.iam.interfaces.rest.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/test-kafka")
@RequiredArgsConstructor
public class KafkaTestController {

    private final StreamBridge streamBridge;

    @PostMapping("/send")
    public ResponseEntity<String> sendMessage(@RequestBody String message) {
        // El nombre debe coincidir con el del yaml: userCreatedProducer-out-0
        streamBridge.send("userCreatedProducer-out-0", message);
        return ResponseEntity.ok("Mensaje enviado a Kafka: " + message);
    }
}