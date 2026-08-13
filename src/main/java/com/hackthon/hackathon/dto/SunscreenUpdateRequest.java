package com.hackthon.hackathon.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SunscreenUpdateRequest {
    private String brand;
    private String name;
    private String productType;
    private String filterType;
    private String spf;
    private String pa;
}
