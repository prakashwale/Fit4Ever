package com.example.fit4ever.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping("/me")
    public Object me(Authentication auth) {
        return new MeResponse(auth.getName()); // email from JWT
    }

    record MeResponse(String email) {}
}
