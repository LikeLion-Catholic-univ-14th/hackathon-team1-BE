package com.hackthon.hackathon.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hackthon.hackathon.dto.SolutionAiResponse;
import com.hackthon.hackathon.entity.ProcedureHistory;
import com.hackthon.hackathon.entity.User;
import com.hackthon.hackathon.repository.ProcedureHistoryRepository;
import com.hackthon.hackathon.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SolutionAiService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final UserRepository userRepository;
    private final UserSunscreenService userSunscreenService;
    private final ProcedureHistoryRepository procedureHistoryRepository;

    public SolutionAiService(
            @Value("${openai.api-key}") String apiKey,
            UserRepository userRepository,
            UserSunscreenService userSunscreenService,
            ProcedureHistoryRepository procedureHistoryRepository
    ) {

        this.restClient = RestClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader(
                        "Authorization",
                        "Bearer " + apiKey
                )
                .build();

        this.userRepository = userRepository;
        this.userSunscreenService = userSunscreenService;
        this.procedureHistoryRepository = procedureHistoryRepository;
    }

    public SolutionAiResponse generateSolution(
            Long userId,
            double uvIndex,
            int sunlightMinutes,
            String weatherCondition
    ) {

        // 1. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "해당 유저를 찾을 수 없습니다."
                        )
                );

        // 2. 사용자의 보유 선크림 계산 결과
        List<UserSunscreenService.SunscreenProtectionResponse> sunscreens =
                userSunscreenService.calculateUserSunscreens(
                        userId,
                        uvIndex
                );

        // 3. 시술 이력은 procedure_history를 기준으로 조회
        List<ProcedureHistory> procedures =
                procedureHistoryRepository.findByUserId(
                        userId
                );

        // 시술 이력이 하나라도 있으면 true
        boolean hasProcedureHistory =
                !procedures.isEmpty();

        // 하나라도 최근 1개월 내 시술이면 true
        boolean procedureWithinOneMonth =
                procedures.stream()
                        .anyMatch(
                                ProcedureHistory::isRecentOneMonth
                        );

        // 시술명을 AI에게 전달할 문자열로 변환
        String procedureDetails =
                procedures.isEmpty()
                        ? "없음"
                        : procedures.stream()
                        .map(
                                ProcedureHistory::getName
                        )
                        .collect(
                                Collectors.joining(", ")
                        );

        // 4. AI 프롬프트 생성
        String prompt =
                createPrompt(
                        user,
                        hasProcedureHistory,
                        procedureDetails,
                        procedureWithinOneMonth,
                        uvIndex,
                        sunlightMinutes,
                        weatherCondition,
                        sunscreens
                );

        // 5. OpenAI Responses API 요청
        Map<String, Object> requestBody =
                Map.of(
                        "model", "gpt-4.1-mini",
                        "input", List.of(
                                Map.of(
                                        "role", "user",
                                        "content", List.of(
                                                Map.of(
                                                        "type", "input_text",
                                                        "text", prompt
                                                )
                                        )
                                )
                        )
                );

        String response =
                restClient.post()
                        .uri("/responses")
                        .contentType(
                                MediaType.APPLICATION_JSON
                        )
                        .body(
                                requestBody
                        )
                        .retrieve()
                        .body(
                                String.class
                        );

        return parseResponse(
                response
        );
    }

    private String createPrompt(
            User user,
            boolean hasProcedureHistory,
            String procedureDetails,
            boolean procedureWithinOneMonth,
            double uvIndex,
            int sunlightMinutes,
            String weatherCondition,
            List<UserSunscreenService.SunscreenProtectionResponse> sunscreens
    ) {

        return """
                당신은 승무원의 자외선 차단 솔루션을 추천하는 AI입니다.

                아래 데이터를 바탕으로 오늘 사용할 선제품 1개를 선택하고,
                실제 행동 가능한 자외선 차단 솔루션을 생성하세요.

                [사용자 프로필]
                피부 타입: %s
                피부 고민: %s
                시술 이력 여부: %s
                시술 내역: %s
                최근 1개월 내 시술 여부: %s

                [오늘 환경]
                UV Index: %.1f
                예상 햇빛 노출 시간: %d분
                날씨: %s

                [보유 선제품]
                %s

                규칙:
                - 반드시 제공된 선제품 중 하나만 추천하세요.
                - sunscreenId는 후보에 존재하는 실제 ID만 반환하세요.
                - effectiveSpf와 requiredSpf를 반드시 고려하세요.
                - insufficient=true인 제품은 충분한 제품이 존재하면 추천하지 마세요.
                - 제공되지 않은 성분이나 제품 특성을 추측하지 마세요.
                - 피부 타입, 피부 고민, 시술 정보는 자외선 관리의 주의 수준을 조정하는 데 활용하세요.
                - 피부 타입이나 피부 고민을 특정 제품의 선택 이유와 직접적인 인과관계로 연결하지 마세요.
                - 답변은 반드시 JSON만 반환하세요.

                - 제품명에 포함된 단어를 근거로 성분, 효능, 진정 효과, 저자극 여부를 추론하지 마세요.
                - 추천 근거로 사용할 수 있는 제품 정보는 productType, effectiveSpf, requiredSpf, insufficient뿐입니다.
                - 피부 타입/피부 고민/시술 이력은 개인화 설명에 활용하되,
                  특정 제품이 해당 피부에 의학적으로 적합하다고 단정하지 마세요.
                - 제공되지 않은 제품의 성분, 효능, 자극도, 피부 적합성은 절대 생성하지 마세요.

                - productType은 도포 방식 및 실효 SPF 계산에만 활용하세요.
                - productType을 근거로 발림성, 사용감, 편의성, 흡수감,
                  덧바르기 용이성 등을 추론하지 마세요.

                - effectiveSpf, requiredSpf, insufficient, UV 계산값 등의 내부 계산 데이터는
                  추천 판단에만 사용하세요.
                - recommendationReason에는 내부 변수명, 계산식 또는 구체적인 계산 수치를 직접 언급하지 마세요.
                - 내부 계산 결과는
                  "오늘 자외선 환경에 충분한 차단이 가능한 제품"
                  같은 자연어로 설명하세요.

                - 특정 제품이 홍조, 잡티, 트러블 등의 피부 고민을
                  개선하거나 관리해준다고 추론하거나 표현하지 마세요.
                - 제품 성분, 효능, 저자극성, 안전성, 피부 적합성을
                  제공된 데이터 없이 단정하지 마세요.
                - "안전한 선택", "피부에 부담이 적다", "자극을 줄인다" 같은 표현도
                  근거가 없으면 사용하지 마세요.

                - solutions는 반드시
                  BEFORE_OUTING,
                  DURING_OUTING,
                  AFTER_OUTING
                  3단계로 반환하세요.

                - description은 사용자가 바로 행동할 수 있는 친숙한 표현으로 작성하세요.
                - 내부 계산 변수명이나 계산 수치는 사용자 문구에 노출하지 마세요.
                - solutions의 title과 description은 매 요청마다
                  입력 데이터를 바탕으로 새롭게 작성하세요.
                - JSON 예시의 title과 description 문구를 그대로 반복하지 마세요.
                - UV 지수, 햇빛 노출 시간, 날씨, 피부 타입,
                  피부 고민, 시술 이력을 종합해 행동 강도와 표현을 달리하세요.
                - 모든 사용자에게 동일한 문구를 반복하지 마세요.

                [제품 제형별 안내 규칙]

                선택한 제품의 productType을 반드시 확인하여
                도포 방법을 작성하세요.

                - CREAM:
                  크림형 제품에 적합한 도포 표현을 사용하세요.
                  얼굴과 노출 부위에 고르게 펴 바르는 방식으로 안내할 수 있습니다.

                - STICK:
                  스틱형 제품이므로
                  "손가락 두 마디", "동전 크기", "짜서 바르기" 등의
                  크림형 제품용 양 표현을 절대 사용하지 마세요.
                  피부에 직접 고르게 바르는 방식으로 안내하세요.
                  제공된 데이터에 없는 정확한 도포 횟수나
                  왕복 횟수를 임의로 생성하지 마세요.

                - SPRAY:
                  스프레이형 제품이므로
                  "손가락 두 마디", "동전 크기", "짜서 바르기" 등의
                  크림형 제품용 양 표현을 사용하지 마세요.
                  노출 부위에 고르게 분사하는 방식으로 안내하세요.
                  제공된 데이터에 없는 분사 횟수나
                  분사 시간을 임의로 생성하지 마세요.

                제품 제형과 맞지 않는 도포 방법을 생성하지 마세요.

                sunlightMinutes는 행동 강도를 결정하는 근거로 사용하되,
                사용자에게 안내할 때 지나치게 정밀한 분 단위 수치를
                그대로 반복하지 마세요.

                예:
                598분 → "장시간 야외 노출이 예상되므로"
                125분 → "약 2시간의 야외 노출이 예상되므로"

                - AFTER_OUTING 단계에서도
                  사용자의 피부가 실제로 자극받았다고 단정하지 마세요.
                - "자극받은 피부", "손상된 피부"처럼 상태를 확정하지 말고,
                  "외출 후에는 부드럽게 세안하고 보습해 주세요"
                  처럼 행동 중심으로 안내하세요.

                다음 JSON 형식으로만 반환하세요.

                {
                  "sunscreenId": 14,
                  "message": "오늘 상황에 맞는 짧은 추천 메시지",
                  "solutions": [
                    {
                      "phase": "BEFORE_OUTING",
                      "title": "외출 전 행동을 요약한 제목",
                      "description": "오늘 UV, 노출 시간, 사용자 상태를 반영한 구체적인 행동 안내"
                    },
                    {
                      "phase": "DURING_OUTING",
                      "title": "외출 중 행동을 요약한 제목",
                      "description": "오늘 환경과 선택 제품을 고려한 재도포 또는 보호 행동 안내"
                    },
                    {
                      "phase": "AFTER_OUTING",
                      "title": "외출 후 행동을 요약한 제목",
                      "description": "오늘 노출 상황을 반영한 사후 관리 행동 안내"
                    }
                  ]
                }
                """.formatted(
                user.getSkinTypes(),
                user.getSkinConcerns(),
                hasProcedureHistory,
                procedureDetails,
                procedureWithinOneMonth,
                uvIndex,
                sunlightMinutes,
                weatherCondition,
                sunscreens
        );
    }

    private SolutionAiResponse parseResponse(
            String response
    ) {

        try {

            JsonNode root =
                    objectMapper.readTree(
                            response
                    );

            String text =
                    root.path("output")
                            .get(0)
                            .path("content")
                            .get(0)
                            .path("text")
                            .asText();

            text =
                    text.replace(
                                    "```json",
                                    ""
                            )
                            .replace(
                                    "```",
                                    ""
                            )
                            .trim();

            return objectMapper.readValue(
                    text,
                    SolutionAiResponse.class
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    "AI 솔루션 응답 파싱에 실패했습니다.",
                    e
            );
        }
    }
}