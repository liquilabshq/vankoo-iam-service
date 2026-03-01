package com.liquilabs.vankoo.iam.application.internal.eventhandlers;

import com.liquilabs.vankoo.iam.application.internal.outboundservices.ExternalIamService;
import com.liquilabs.vankoo.iam.domain.model.events.UserCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
public class UserCreatedEventHandler {

    private final ExternalIamService externalIamService;
    private static final Logger LOGGER = LoggerFactory.getLogger(UserCreatedEventHandler.class);

    public UserCreatedEventHandler(ExternalIamService externalIamService) {
        this.externalIamService = externalIamService;
    }

    // phase = AFTER_COMMIT asegura que si la DB falla, NO se envía nada a Kafka
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(UserCreatedEvent event) {
        LOGGER.info("Handling UserCreatedEvent for user email: {}", event.getUser().getEmail());
        externalIamService.publishUserCreated(event);
    }
}
