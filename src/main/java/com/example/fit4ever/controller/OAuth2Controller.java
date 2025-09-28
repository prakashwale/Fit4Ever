package com.example.fit4ever.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

@Controller
@RequestMapping("/oauth2")
@Slf4j
public class OAuth2Controller {

    @GetMapping("/redirect")
    public RedirectView oauth2Redirect(@RequestParam(name = "token") String token) {
        log.info("OAuth2 redirect received with token");
        
        // For development - redirect to frontend with token
        String redirectUrl = "/?token=" + token + "&auth=oauth2";
        
        return new RedirectView(redirectUrl);
    }
}
