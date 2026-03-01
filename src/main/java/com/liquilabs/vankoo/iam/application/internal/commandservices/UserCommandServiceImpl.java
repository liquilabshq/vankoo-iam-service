package com.liquilabs.vankoo.iam.application.internal.commandservices;

import com.liquilabs.vankoo.iam.application.internal.outboundservices.hashing.HashingService;
import com.liquilabs.vankoo.iam.application.internal.outboundservices.tokens.TokenService;
import com.liquilabs.vankoo.iam.domain.model.aggregates.User;
import com.liquilabs.vankoo.iam.domain.model.commands.SignInCommand;
import com.liquilabs.vankoo.iam.domain.model.commands.SignUpCommand;
import com.liquilabs.vankoo.iam.domain.model.valueobjects.Password;
import com.liquilabs.vankoo.iam.domain.model.valueobjects.RoleName;
import com.liquilabs.vankoo.iam.domain.services.UserCommandService;
import com.liquilabs.vankoo.iam.infrastructure.persistence.jpa.repositories.RoleRepository;
import com.liquilabs.vankoo.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import jakarta.transaction.Transactional;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserCommandServiceImpl implements UserCommandService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final HashingService hashingService;
    private final TokenService tokenService;

    public UserCommandServiceImpl(
            UserRepository userRepository,
            RoleRepository roleRepository,
            HashingService hashingService,
            TokenService tokenService
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.hashingService = hashingService;
        this.tokenService = tokenService;
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
        var user = new User(command.email(), new Password(hashingService.encode(command.password().password())), roles);
        user.registerUserCreatedEvent();
        userRepository.save(user);
        return Optional.of(user);
    }

    @Override
    @Transactional
    public Optional<ImmutablePair<User, String>> handle(SignInCommand command) {
        var user = userRepository.findByEmail(command.email());
        if (user.isEmpty())
            throw new IllegalArgumentException("User not found");
        if (!hashingService.matches(command.password().password(), user.get().getPassword().password()))
            throw new IllegalArgumentException("Invalid password");
        var token = tokenService.generateToken(user.get().getEmail().email());
        return Optional.of(ImmutablePair.of(user.get(), token));
    }
}
