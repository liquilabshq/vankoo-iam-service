package com.liquilabs.vankoo.iam.interfaces.rest.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/hello")
public class HelloWorldController {
    @GetMapping
    public String sayHello() {
        return "Hello, World!";
    }
}
