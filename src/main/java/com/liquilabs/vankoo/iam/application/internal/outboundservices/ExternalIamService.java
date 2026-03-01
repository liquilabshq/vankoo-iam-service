package com.liquilabs.vankoo.iam.application.internal.outboundservices;

import com.liquilabs.vankoo.iam.domain.model.events.UserCreatedEvent;

public interface ExternalIamService {
    void publishUserCreated(UserCreatedEvent event);
}
