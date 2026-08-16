package com.hackthon.hackathon.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class ProfileUpdateRequest {
    private String name;
    private String baseAirport;
    private List<String> skinTypes;
    private List<String> skinConcerns;
    private ProcedureHistoryDto procedureHistory;

    @Getter
    @NoArgsConstructor
    public static class ProcedureHistoryDto {
        private boolean hasHistory;
        private String detail; // 레이저 시술 등 상세 내용

        @JsonProperty("isRecentOneMonth")
        private boolean isRecentOneMonth;
    }
}