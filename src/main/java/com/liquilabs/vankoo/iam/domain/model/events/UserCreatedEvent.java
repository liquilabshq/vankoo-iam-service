package com.liquilabs.vankoo.iam.domain.model.events;

import com.liquilabs.vankoo.iam.domain.model.aggregates.User;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class UserCreatedEvent extends ApplicationEvent {
    private final User user; // Pasamos el agregado completo

    public UserCreatedEvent(Object source, User user) {
        super(source);
        this.user = user;
    }
}
