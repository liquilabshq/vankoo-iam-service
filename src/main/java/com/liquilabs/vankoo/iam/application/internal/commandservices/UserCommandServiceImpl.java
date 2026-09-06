package com.liquilabs.vankoo.iam.application.internal.commandservices;

import com.liquilabs.vankoo.iam.application.internal.outboundservices.hashing.HashingService;
import com.liquilabs.vankoo.iam.application.internal.outboundservices.tokens.TokenService;
import com.liquilabs.vankoo.iam.domain.exceptions.EmailAlreadyInUseException;
import com.liquilabs.vankoo.iam.domain.exceptions.InvalidCredentialsException;
import com.liquilabs.vankoo.iam.domain.model.aggregates.User;
import com.liquilabs.vankoo.iam.domain.model.commands.SignInCommand;
import com.liquilabs.vankoo.iam.domain.model.commands.SignUpCommand;
import com.liquilabs.vankoo.iam.domain.model.entities.Role;
import com.liquilabs.vankoo.iam.domain.model.valueobjects.Password;
import com.liquilabs.vankoo.iam.domain.model.valueobjects.RoleName;
import com.liquilabs.vankoo.iam.domain.services.UserCommandService;
import com.liquilabs.vankoo.iam.infrastructure.persistence.jpa.repositories.RoleRepository;
import com.liquilabs.vankoo.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import jakarta.transaction.Transactional;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
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
            throw new EmailAlreadyInUseException(command.email());
        var roleNames = (command.roles() == null || command.roles().isEmpty())
                ? List.of(RoleName.ROLE_USER)
                : command.roles().stream().map(Role::getName).toList();
        var roles = roleRepository.findAllByNameIn(roleNames);
        // Reaching here means the roles table was never seeded, not that the client got
        // anything wrong: the requested names were already checked against the
        // self-assignable set before the command was built.
        if (roles.size() != roleNames.size())
            throw new IllegalStateException("Roles missing from the database: " + roleNames);
        var user = new User(command.email(), new Password(hashingService.encode(command.password().password())), roles);
        user.registerUserCreatedEvent();
        userRepository.save(user);
        return Optional.of(user);
    }

    @Override
    @Transactional
    public Optional<ImmutablePair<User, String>> handle(SignInCommand command) {
        // Both failures below raise the same exception on purpose: an unknown address
        // and a wrong password must be impossible to tell apart from outside, or this
        // endpoint becomes a way of discovering who has an account.
        var user = userRepository.findByEmailWithRoles(command.email())
                .orElseThrow(InvalidCredentialsException::new);
        if (!hashingService.matches(command.password().password(), user.getPassword().password()))
            throw new InvalidCredentialsException();
        var roles = user.getRoles().stream().map(role -> role.getName().name()).toList();
        var token = tokenService.generateToken(Objects.requireNonNull(user.getId()).id().toString(), user.getEmail().email(), roles);
        return Optional.of(ImmutablePair.of(user, token));
    }
}
