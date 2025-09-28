package com.example.fit4ever.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    @Column
    private String password; // hashed, nullable for OAuth2 users

    @Builder.Default
    private String role = "USER"; // simple role
    
    // OAuth2 fields
    @Column(name = "provider")
    private String provider; // LOCAL, GOOGLE
    
    @Column(name = "provider_id")
    private String providerId; // Google user ID
    
    @Column(name = "image_url")
    private String imageUrl; // Profile picture URL
    
    @Builder.Default
    @Column(name = "email_verified")
    private Boolean emailVerified = false;
}
