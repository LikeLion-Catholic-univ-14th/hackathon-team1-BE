package com.hackthon.hackathon.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MypageResponse {
    private String name;
    private String baseAirport;
    private List<String> skinTypes;
    private List<String> skinConcerns;
    private List<PouchItemDto> pouch;

    @Data
    @Builder
    public static class PouchItemDto {
        private Long productId;
        private String name;
        private String type;
        private String spf;
    }
}
