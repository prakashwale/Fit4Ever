package com.example.fit4ever.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping("/me")
    public ResponseEntity<MeResponse> me(Authentication auth) {
        return ResponseEntity.ok(new MeResponse(auth.getName()));
    }

    record MeResponse(String email) {}
}
