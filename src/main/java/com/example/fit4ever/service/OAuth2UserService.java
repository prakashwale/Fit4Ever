package com.example.fit4ever.service;

import com.example.fit4ever.model.User;
import com.example.fit4ever.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String providerId = oAuth2User.getAttribute("sub");
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String imageUrl = oAuth2User.getAttribute("picture");
        Boolean emailVerified = oAuth2User.getAttribute("email_verified");

        log.info("OAuth2 login attempt - Provider: {}, Email: {}", registrationId, email);

        if (email == null || email.trim().isEmpty()) {
            log.error("OAuth2 user email is null or empty for provider: {}", registrationId);
            throw new OAuth2AuthenticationException("Email not found from OAuth2 provider");
        }

        User user = processOAuth2User(registrationId, providerId, email, name, imageUrl, emailVerified);
        
        return new OAuth2UserPrincipal(user, oAuth2User.getAttributes());
    }

    private User processOAuth2User(String provider, String providerId, String email, 
                                 String name, String imageUrl, Boolean emailVerified) {
        
        Optional<User> existingUser = userRepository.findByEmail(email);
        
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            
            // Update OAuth2 info if user exists but doesn't have OAuth2 data
            if (user.getProvider() == null || !user.getProvider().equals(provider.toUpperCase())) {
                user.setProvider(provider.toUpperCase());
                user.setProviderId(providerId);
                user.setImageUrl(imageUrl);
                user.setEmailVerified(emailVerified != null ? emailVerified : false);
                user = userRepository.save(user);
                log.info("Updated existing user with OAuth2 data: {}", email);
            }
            
            return user;
        } else {
            // Create new user
            User newUser = User.builder()
                    .name(name != null ? name : email.split("@")[0])
                    .email(email.toLowerCase().trim())
                    .provider(provider.toUpperCase())
                    .providerId(providerId)
                    .imageUrl(imageUrl)
                    .emailVerified(emailVerified != null ? emailVerified : false)
                    .role("USER")
                    .build();
            
            User savedUser = userRepository.save(newUser);
            log.info("Created new OAuth2 user: {}", email);
            return savedUser;
        }
    }
}
