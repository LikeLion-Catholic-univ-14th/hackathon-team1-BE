package com.hackthon.hackathon.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class ProcedureDto {

    @Getter
    @NoArgsConstructor
    public static class Request {
        private String name;
        private boolean isRecentOneMonth;
    }

    @Getter
    @Builder
    public static class Response {
        private Long procedureId;
        private String name;
        private boolean isRecentOneMonth;
    }
}
