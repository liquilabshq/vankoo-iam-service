package com.liquilabs.vankoo.iam.domain.model.aggregates;

import com.liquilabs.vankoo.iam.domain.model.events.UserCreatedEvent;
import jakarta.persistence.*;
import lombok.Getter;
import org.springframework.data.domain.AbstractAggregateRoot;

@Entity
@Table(name = "users")
@Getter
public class User extends AbstractAggregateRoot<User> {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;

    private String name;

    protected User() {
    }

    public User(String email, String name) {
        this.email = email;
        this.name = name;
    }

    public void registerUserCreatedEvent() {
        this.registerEvent(new UserCreatedEvent(this, this));
    }
}
