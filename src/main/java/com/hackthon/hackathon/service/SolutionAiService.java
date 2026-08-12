package com.hackthon.hackathon.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hackthon.hackathon.dto.SolutionAiResponse;
import com.hackthon.hackathon.entity.User;
import com.hackthon.hackathon.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class SolutionAiService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final UserRepository userRepository;
    private final UserSunscreenService userSunscreenService;

    public SolutionAiService(
            @Value("${openai.api-key}") String apiKey,
            UserRepository userRepository,
            UserSunscreenService userSunscreenService
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
    }

    public SolutionAiResponse generateSolution(
            Long userId,
            double uvIndex,
            int sunlightMinutes,
            String weatherCondition
    ) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "해당 유저를 찾을 수 없습니다."
                        )
                );

        List<UserSunscreenService.SunscreenProtectionResponse> sunscreens =
                userSunscreenService.calculateUserSunscreens(
                        userId,
                        uvIndex
                );

        String prompt = createPrompt(
                user,
                uvIndex,
                sunlightMinutes,
                weatherCondition,
                sunscreens
        );

        Map<String, Object> requestBody = Map.of(
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(requestBody)
                        .retrieve()
                        .body(String.class);

        return parseResponse(response);
    }
    private String createPrompt(
            User user,
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
            최근 1개월 내 시술 여부: %s

            [오늘 환경]
            UV Index: %.1f
            예상 햇빛 노출 시간: %d분
            날씨: %s

            [보유 선제품]
            %s

            규칙:
            - 반드시 제공된 선제품 중 하나만 추천
            - sunscreenId는 후보에 존재하는 실제 ID만 반환
            - effectiveSpf와 requiredSpf를 반드시 고려
            - insufficient=true인 제품은 충분한 제품이 존재하면 추천하지 않음
            - 제공되지 않은 성분이나 제품 특성을 추측하지 않음
            - 피부 타입, 피부 고민, 시술 정보를 추천 이유에 반영
            - 답변은 반드시 JSON만 반환
            - 제품명에 포함된 단어를 근거로 성분, 효능, 진정 효과, 저자극 여부를 추론하지 마세요.
            - 추천 근거로 사용할 수 있는 제품 정보는 productType, effectiveSpf, requiredSpf, insufficient뿐입니다.
            - 피부 타입/피부 고민/시술 이력은 개인화 설명에 활용하되, 특정 제품이 해당 피부에 의학적으로 적합하다고 단정하지 마세요.
            - 제공되지 않은 제품의 성분, 효능, 자극도, 피부 적합성은 절대 생성하지 마세요.
            - 도포량은 사용자가 바로 이해하고 행동할 수 있는 친숙한 표현으로 설명하세요.
            - "2mg/cm²"처럼 전문적인 수치만 단독으로 제시하지 마세요.
            - 예: "얼굴 기준 500원 동전 크기 정도", "손가락 두 마디 정도"처럼 일상적인 크기나 양으로 표현하세요.
            - 필요한 경우 일상적인 표현 뒤에 정확한 기준을 괄호로 함께 표시할 수 있습니다
            - productType은 도포 방식 및 실효 SPF 계산에만 활용하고, 발림성·사용감·자극도 등의 특성을 추론하지 마세요.
            - effectiveSpf, requiredSpf, UV 계산값 등 내부 계산 데이터는 추천 판단에만 사용하세요.
            - recommendationReason에는 내부 변수명, 계산식 또는 구체적인 계산 수치를 직접 언급하지 마세요.
            - 계산 결과는 사용자가 이해하기 쉬운 자연어로 해석해서 설명하세요. 
            - 도포량은 사용자가 바로 이해할 수 있는 친숙한 표현으로 설명하세요.
            - 전문적인 수치만 단독으로 제시하지 마세요.
            - 예: "얼굴 기준 500원 동전 크기 정도", "손가락 두 마디 정도"
            - productType은 내부 차단 효과 판단에만 사용하세요.
            - recommendationReason에서 제품 제형을 근거로 발림성, 편의성, 덧바르기 용이성, 사용감 등을 설명하지 마세요.
            - recommendationReason에는 effectiveSpf, requiredSpf, insufficient, UV 계산값 등 내부 계산용 변수명이나 수치를 절대 노출하지 마세요.
            - 내부 계산 결과는 추천 판단에만 사용하고, 사용자에게는 "오늘 자외선 환경에 충분한 차단이 가능한 제품"처럼 자연어로만 설명하세요.  
            - 피부 타입, 피부 고민, 시술 이력은 자외선 관리의 주의 수준을 조정하는 데만 사용하세요.
            - 특정 제품이 홍조, 잡티, 트러블 등 피부 고민을 개선하거나 관리해준다고 추론하거나 표현하지 마세요.
            - 제품 성분, 효능, 저자극성, 안전성, 피부 적합성을 제공된 데이터 없이 단정하지 마세요.
            - "안전한 선택", "피부에 부담이 적다", "자극을 줄인다" 같은 표현도 근거가 없으면 사용하지 마세요.
            - productType을 근거로 발림성, 사용 편의성, 덧바르기 용이성, 흡수감 등을 추론하거나 설명하지 마세요.
                - solutions는 반드시 BEFORE_OUTING, DURING_OUTING, AFTER_OUTING 3단계로 반환하세요.
                - description은 사용자가 바로 행동할 수 있는 친숙한 표현으로 작성하세요.
                - 내부 계산 변수명이나 계산 수치는 사용자 문구에 노출하지 마세요.
            
            다음 JSON 형식으로만 반환하세요.
                
                  {
                    "sunscreenId": 14,
                    "message": "오늘 자외선 환경에 적합한 제품이에요.",
                    "solutions": [
                      {
                        "phase": "BEFORE_OUTING",
                        "title": "외출 전 선크림 도포",
                        "description": "얼굴 기준 500원 동전 크기 정도로 고르게 발라주세요."
                      },
                      {
                        "phase": "DURING_OUTING",
                        "title": "자외선 차단 보충",
                        "description": "2~3시간 간격으로 다시 발라주세요."
                      },
                      {
                        "phase": "AFTER_OUTING",
                        "title": "외출 후 피부 관리",
                        "description": "외출 후에는 피부에 남은 제품을 깨끗하게 제거해 주세요."
                      }
                    ]
                  }
              ]
            }
            """.formatted(
                user.getSkinTypes(),
                user.getSkinConcerns(),
                user.isHasProcedureHistory(),
                user.getProcedureWithinOneMonth(),
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
                    objectMapper.readTree(response);

            String text = root
                    .path("output")
                    .get(0)
                    .path("content")
                    .get(0)
                    .path("text")
                    .asText();

            text = text
                    .replace("```json", "")
                    .replace("```", "")
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