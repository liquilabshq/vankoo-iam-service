package com.liquilabs.vankoo.iam.application.internal.commandservices;

import com.liquilabs.vankoo.iam.domain.model.aggregates.User;
import com.liquilabs.vankoo.iam.domain.model.commands.SignUpCommand;
import com.liquilabs.vankoo.iam.domain.model.valueobjects.RoleName;
import com.liquilabs.vankoo.iam.domain.services.UserCommandService;
import com.liquilabs.vankoo.iam.infrastructure.persistence.jpa.repositories.RoleRepository;
import com.liquilabs.vankoo.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserCommandServiceImpl implements UserCommandService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public UserCommandServiceImpl(
            UserRepository userRepository,
            RoleRepository roleRepository
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    @Transactional
    public Optional<User> handle(SignUpCommand command) {
        if (userRepository.existsByEmail(command.email()))
            throw new IllegalArgumentException("Email already in use");
        var roles = (command.roles() == null || command.roles().isEmpty())
                ? List.of(roleRepository.findByName(RoleName.ROLE_USER).orElseThrow(() -> new RuntimeException("Default role not found")))
                : command.roles().stream()
                .map(role -> roleRepository.findByName(role.getName()).orElseThrow(() -> new RuntimeException("Role name not found")))
                .toList();
        var user = new User(command.email(), command.password(), roles);
        user.registerUserCreatedEvent();
        userRepository.save(user);
        return Optional.of(user);
    }
}
