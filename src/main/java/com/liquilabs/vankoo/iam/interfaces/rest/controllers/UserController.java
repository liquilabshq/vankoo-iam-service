package com.liquilabs.vankoo.iam.interfaces.rest.controllers;

import com.liquilabs.vankoo.iam.application.internal.commandservices.UserCommandServiceImpl;
import com.liquilabs.vankoo.iam.domain.model.valueobjects.Email;
import com.liquilabs.vankoo.iam.interfaces.rest.resources.CreateUserResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserCommandServiceImpl userCommandService;

    public UserController(UserCommandServiceImpl userCommandService) {
        this.userCommandService = userCommandService;
    }

    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody CreateUserResource resource) {
        userCommandService.createUser(new Email(resource.email()), resource.name());
        return ResponseEntity.ok().build();
    }
}
