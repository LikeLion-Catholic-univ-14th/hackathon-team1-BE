package com.hackthon.hackathon.service;

import com.hackthon.hackathon.dto.ProfileSetupRequest;
import com.hackthon.hackathon.entity.User;
import com.hackthon.hackathon.entity.ProcedureHistory;
import com.hackthon.hackathon.repository.UserRepository;
import com.hackthon.hackathon.repository.ProcedureHistoryRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserSetupService {

    private final UserRepository userRepository;

    // 👇 수정됨: 타입이 ProcedureHistory(엔티티)에서 ProcedureHistoryRepository로 변경!
    private final ProcedureHistoryRepository procedureHistoryRepository;

    @Transactional
    public void setupProfile(Long userId, ProfileSetupRequest profileSetupRequest) {

        // 1. 프로시저 데이터 추출을 위한 변수들
        boolean hasHistory = false;
        String details = null;
        Boolean withinOneMonth = null;

        if (profileSetupRequest.getProcedure() != null) {
            hasHistory = profileSetupRequest.getProcedure().isHasProcedureHistory();
            details = profileSetupRequest.getProcedure().getProcedureDetails();
            withinOneMonth = profileSetupRequest.getProcedure().getProcedureWithinOneMonth();
        }

        // 2. 유저가 없으면 새로 만들고, 있으면 가져오기 (회원가입 겸용)
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

        if (hasHistory && details != null && !details.trim().isEmpty()) {

            // withinOneMonth 값이 null일 수 있으므로(Boolean), 기본값을 false로 안전하게 처리합니다.
            boolean isRecent = (withinOneMonth != null) ? withinOneMonth : false;

            ProcedureHistory procedureHistory = ProcedureHistory.builder()
                    .user(user)
                    .name(details)
                    .isRecentOneMonth(isRecent)
                    .build();

            procedureHistoryRepository.save(procedureHistory);
        }
    }
}