package com.hackthon.hackathon.controller;

import com.hackthon.hackathon.dto.ProfileSetupRequest;
import com.hackthon.hackathon.entity.User;
import com.hackthon.hackathon.service.UserSetupService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserController {
    private final UserSetupService userSetupService;
    @PostMapping("/users/{userId}/profile")
    public void userSetup(@PathVariable Long userId, @RequestBody ProfileSetupRequest profileSetupRequest ) {
        userSetupService.setupProfile(userId, profileSetupRequest);


    }

}
