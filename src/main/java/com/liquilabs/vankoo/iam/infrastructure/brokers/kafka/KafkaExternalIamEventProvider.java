package com.liquilabs.vankoo.iam.infrastructure.brokers.kafka;

import com.liquilabs.vankoo.iam.application.internal.outboundservices.ExternalIamService;
import com.liquilabs.vankoo.iam.domain.model.events.UserCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KafkaExternalIamEventProvider implements ExternalIamService {

    private final StreamBridge streamBridge;

    @Override
    public void publishUserCreated(UserCreatedEvent event) {
        streamBridge.send("userCreated-out-0", event);
    }

    // Este Record define el JSON exacto que verá el resto del mundo
    private record UserCreatedIntegrationMessage(Long id, String email) {}
}
