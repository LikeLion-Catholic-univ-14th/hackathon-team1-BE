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

        String fileName =
                image.getOriginalFilename();

        try {

            if (image.isEmpty()) {

                return ScheduleExtractResponse.failed(
                        fileName,
                        "업로드된 이미지가 없습니다."
                );
            }

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

            /*
             * AI가 응답은 했지만
             * 일정이 하나도 추출되지 않은 경우
             */
            if (schedules == null
                    || schedules.isEmpty()) {

                return ScheduleExtractResponse.failed(
                        fileName,
                        "일정을 인식하지 못했습니다."
                );
            }

            return ScheduleExtractResponse.success(
                    fileName,
                    schedules
            );

        } catch (IOException e) {

            return ScheduleExtractResponse.failed(
                    fileName,
                    "이미지 처리 중 오류가 발생했습니다."
            );

        } catch (Exception e) {

            return ScheduleExtractResponse.failed(
                    fileName,
                    "일정을 인식하지 못했습니다."
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

            JsonNode output =
                    root.path("output");

            if (!output.isArray()
                    || output.isEmpty()) {

                return List.of();
            }

            JsonNode content =
                    output.get(0)
                            .path("content");

            if (!content.isArray()
                    || content.isEmpty()) {

                return List.of();
            }

            String text =
                    content.get(0)
                            .path("text")
                            .asText();

            if (text == null
                    || text.isBlank()) {

                return List.of();
            }

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

            JsonNode result =
                    objectMapper.readTree(
                            text
                    );

            JsonNode schedulesNode =
                    result.path(
                            "schedules"
                    );

            if (!schedulesNode.isArray()) {
                return List.of();
            }

            return objectMapper
                    .readerForListOf(
                            ScheduleExtractResponse
                                    .ExtractedSchedule.class
                    )
                    .readValue(
                            schedulesNode
                    );

        } catch (Exception e) {

            return List.of();
        }
    }
}