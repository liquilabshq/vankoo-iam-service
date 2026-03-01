package com.liquilabs.vankoo.iam.interfaces.rest.resources;

public record SignInResource(
        String email,
        String password
) {
}
