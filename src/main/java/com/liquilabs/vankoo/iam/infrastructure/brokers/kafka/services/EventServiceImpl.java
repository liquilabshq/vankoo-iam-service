package com.liquilabs.vankoo.iam.infrastructure.brokers.kafka.services;

import com.liquilabs.vankoo.iam.infrastructure.brokers.kafka.KafkaEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements KafkaEventService {

    private final StreamBridge streamBridge;

    @Override
    public void publishEvent(Object event) {
        streamBridge.send("iam-out-0", event);
    }
}
