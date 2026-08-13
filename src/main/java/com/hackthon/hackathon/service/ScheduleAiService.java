package com.hackthon.hackathon.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hackthon.hackathon.dto.ScheduleExtractResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class ScheduleAiService {

    private final RestClient restClient;

    // 다시 직접 생성
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ScheduleAiService(
            @Value("${openai.api-key}") String apiKey
    ) {

        this.restClient = RestClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader(
                        "Authorization",
                        "Bearer " + apiKey
                )
                .build();
    }

    public ScheduleExtractResponse extract(
            MultipartFile image
    ) {

        try {

            String base64Image =
                    Base64.getEncoder()
                            .encodeToString(
                                    image.getBytes()
                            );

            String contentType =
                    image.getContentType();

            if (contentType == null) {
                contentType = "image/jpeg";
            }

            String dataUrl =
                    "data:"
                            + contentType
                            + ";base64,"
                            + base64Image;

            Map<String, Object> requestBody =
                    Map.of(
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

            String response =
                    restClient.post()
                            .uri("/responses")
                            .contentType(
                                    MediaType.APPLICATION_JSON
                            )
                            .body(requestBody)
                            .retrieve()
                            .body(String.class);

            List<ScheduleExtractResponse.ExtractedSchedule>
                    schedules =
                    parseResponse(response);

            return new ScheduleExtractResponse(
                    image.getOriginalFilename(),
                    schedules
            );

        } catch (IOException e) {

            throw new RuntimeException(
                    "이미지 처리 중 오류가 발생했습니다.",
                    e
            );
        }
    }

    private String createPrompt() {

        return """
                승무원 비행 일정 이미지를 분석하세요.

                이미지에서 확인할 수 있는 모든 비행 일정을 추출하세요.

                다음 JSON 형식으로만 반환하세요.

                {
                  "schedules": [
                    {
                      "flightNumber": "KE121",
                      "departureAirport": "ICN",
                      "arrivalAirport": "SYD",
                      "departureTime": "2026-08-09T09:00:00",
                      "arrivalTime": "2026-08-09T13:00:00",
                      "isQuickTurn": false
                    }
                  ]
                }

                규칙:
                - 이미지에 존재하는 모든 비행편을 추출하세요.
                - 공항은 반드시 IATA 3자리 코드로 반환하세요.
                - 시간 형식은 반드시 yyyy-MM-dd'T'HH:mm:ss 로 반환하세요.
                - 이미지에 있는 정보만 사용하세요.
                - 확인할 수 없는 내용을 임의로 추측하지 마세요.
                - 여러 비행편이 있으면 schedules 배열에 모두 넣으세요.
                - JSON 이외의 설명이나 문장은 작성하지 마세요.
                """;
    }

    private List<ScheduleExtractResponse.ExtractedSchedule>
    parseResponse(
            String response
    ) {

        try {

            JsonNode root =
                    objectMapper.readTree(response);

            String text =
                    root
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

            JsonNode result =
                    objectMapper.readTree(text);

            return objectMapper
                    .readerForListOf(
                            ScheduleExtractResponse
                                    .ExtractedSchedule.class
                    )
                    .readValue(
                            result.path("schedules")
                    );

        } catch (Exception e) {

            throw new RuntimeException(
                    "AI 일정 응답 파싱에 실패했습니다.",
                    e
            );
        }
    }
}