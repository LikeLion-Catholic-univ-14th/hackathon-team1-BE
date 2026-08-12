package com.hackthon.hackathon.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hackthon.hackathon.dto.ScheduleExtractResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.http.MediaType;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.io.IOException;

@Service
public class ScheduleAiService {
    private final RestClient restClient;
    private final ObjectMapper objectMapper= new ObjectMapper();

    public ScheduleAiService(
            @Value("${openai.api-key}") String apiKey
    ) {
        this.restClient = RestClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();

    }

    public ScheduleExtractResponse extract(MultipartFile image) {

        try {
            String base64Image = Base64.getEncoder()
                    .encodeToString(image.getBytes());

            String contentType = image.getContentType();

            if (contentType == null) {
                contentType = "image/jpeg";
            }

            String dataUrl =
                    "data:" + contentType + ";base64," + base64Image;

            Map<String, Object> requestBody = Map.of(
                    "model", "gpt-4.1-mini",
                    "input", List.of(
                            Map.of(
                                    "role", "user",
                                    "content", List.of(
                                            Map.of(
                                                    "type", "input_text",
                                                    "text", createPrompt()
                                            ),
                                            Map.of(
                                                    "type", "input_image",
                                                    "image_url", dataUrl
                                            )
                                    )
                            )
                    )
            );

            String response = restClient.post()
                    .uri("/responses")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            return parseResponse(response);

        } catch (IOException e) {
            throw new RuntimeException("이미지 처리 중 오류가 발생했습니다.", e);
        }
    }

    private String createPrompt() {
        return """
                승무원 비행 일정 이미지를 분석하세요.

                다음 필드만 JSON으로 반환하세요.

                {
                  "flightNumber": "편명",
                  "departureAirport": "출발 공항 IATA 코드",
                  "arrivalAirport": "도착 공항 IATA 코드",
                  "departureTime": "yyyy-MM-dd'T'HH:mm:ss",
                  "arrivalTime": "yyyy-MM-dd'T'HH:mm:ss",
                  "isQuickTurn": false
                }

                규칙:
                - 공항은 반드시 IATA 3자리 코드로 반환
                - 이미지에 적힌 내용만 사용
                - 확인할 수 없는 내용은 추측하지 않음
                - JSON 이외의 설명은 작성하지 않음
                """;
    }

    private ScheduleExtractResponse parseResponse(String response) {

        try {
            JsonNode root = objectMapper.readTree(response);

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
                    ScheduleExtractResponse.class
            );

        } catch (Exception e) {
            throw new RuntimeException(
                    "AI 일정 응답 파싱에 실패했습니다.",
                    e
            );
        }
    }
}
