package com.hackthon.hackathon.dto.today; // 👈 패키지 경로 확인

import com.hackthon.hackathon.entity.Sunscreen;
import lombok.Builder;
import lombok.Data;

@Data      // 👈 추가! (Getter, Setter 등을 자동 생성해 JSON 변환을 가능하게 함)
@Builder   // 👈 추가! (fromEntity 내부에서 .builder()를 사용할 수 있게 함)
public class SunscreenSearchResponse {
    private Long id;
    private String name;
    private String brand;
    private String filterType;
    private String productType;
    private String spf;
    private String pa;

    public static SunscreenSearchResponse fromEntity(Sunscreen sunscreen) {
        return SunscreenSearchResponse.builder()
                .id(sunscreen.getId())
                .name(sunscreen.getName())
                .brand(sunscreen.getBrand())
                .filterType(sunscreen.getFilterType() != null ? sunscreen.getFilterType().name() : null)
                .productType(sunscreen.getProductType() != null ? sunscreen.getProductType().name() : null)
                .spf(sunscreen.getSpf())
                .pa(sunscreen.getPa() != null ? sunscreen.getPa().name() : null)
                .build();
    }
}