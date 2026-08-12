package com.hackthon.hackathon.service;

import com.hackthon.hackathon.dto.ProfileSetupRequest;
import com.hackthon.hackathon.entity.User;
import com.hackthon.hackathon.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

@Service
@RequiredArgsConstructor

public class UserSetupService {
    private final UserRepository userRepository;
    @Transactional
    public void setupProfile(Long userId, ProfileSetupRequest profileSetupRequest ) {
        User user= userRepository.findById(userId).orElseThrow(()-> new IllegalStateException("해당 유저를 찾을 수 없습니다."));

        boolean hasHistory = false;
        String details = null;
        Boolean withinOneMonth = null;

        if (profileSetupRequest.getProcedure() != null) {
            hasHistory = profileSetupRequest.getProcedure().isHasProcedureHistory();
            details = profileSetupRequest.getProcedure().getProcedureDetails();
            withinOneMonth = profileSetupRequest.getProcedure().getProcedureWithinOneMonth();
        }

        user.setupProfile(
                profileSetupRequest.getName(),
                profileSetupRequest.getBaseAirport(),
                profileSetupRequest.getSkinTypes(),
                profileSetupRequest.getSkinConcerns(),
                hasHistory,
                details,
                withinOneMonth
        );
    }
}
