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

    // 👇 이거 추가! 프론트가 원하는 시술 이력 중괄호 객체
    private ProcedureHistoryDto procedureHistory;

    private List<PouchItemDto> pouch;

    // 👇 시술 이력용 작은 바구니 클래스 추가!
    @Data
    @Builder
    public static class ProcedureHistoryDto {
        private boolean hasHistory;
        private String detail;
        private Boolean isRecentOneMonth;
    }

    @Data
    @Builder
    public static class PouchItemDto {
        private Long productId;
        private String name;
        private String productType;
        private String filterType;
        private String spf;
        private String pa;
    }
}