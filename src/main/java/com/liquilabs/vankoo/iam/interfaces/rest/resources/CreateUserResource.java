package com.liquilabs.vankoo.iam.interfaces.rest.resources;

public record CreateUserResource(
        String email,
        String name
) {
}
