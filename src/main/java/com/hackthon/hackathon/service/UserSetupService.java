package com.hackthon.hackathon.service;

import com.hackthon.hackathon.dto.ProfileSetupRequest;
import com.hackthon.hackathon.entity.User;
import com.hackthon.hackathon.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserSetupService {

    private final UserRepository userRepository;

    @Transactional
    public void setupProfile(Long userId, ProfileSetupRequest profileSetupRequest) {

        // 1. 프로시저 데이터 추출을 위한 변수들 (final 문제 해결을 위해 블록 밖에서 안전하게 처리)
        boolean hasHistory = false;
        String details = null;
        Boolean withinOneMonth = null;

        if (profileSetupRequest.getProcedure() != null) {
            hasHistory = profileSetupRequest.getProcedure().isHasProcedureHistory();
            details = profileSetupRequest.getProcedure().getProcedureDetails();
            withinOneMonth = profileSetupRequest.getProcedure().getProcedureWithinOneMonth();
        }

        // 2. 유저가 없으면 새로 만들고, 있으면 가져오기 (회원가입 겸용)
        // 람다 제약 에러를 피하기 위해 orElseGet 대신 안전한 ifPresent/orElse 패턴 사용
        User user = userRepository.findById(userId).orElse(null);

        if (user == null) {
            User newUser = new User(
                    profileSetupRequest.getName(),
                    profileSetupRequest.getBaseAirport(),
                    profileSetupRequest.getSkinTypes(),
                    profileSetupRequest.getSkinConcerns(),
                    hasHistory,
                    details,
                    withinOneMonth
            );
            user = userRepository.save(newUser);
        } else {
            // 이미 있는 유저라면 프로필 정보 업데이트
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
}