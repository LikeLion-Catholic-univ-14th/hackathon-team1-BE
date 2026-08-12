package com.hackthon.hackathon.dto;
import com.hackthon.hackathon.enums.Pa;
import com.hackthon.hackathon.enums.SunscreenFilterType;
import com.hackthon.hackathon.enums.SunscreenProductType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class SunscreenSaveRequest {
    private List<SunscreenList> sunscreens;

    @Getter
    @NoArgsConstructor
    public static class SunscreenList {
        private String brand;
        private String name;
        private SunscreenFilterType filterType;
        private SunscreenProductType productType;
        private String spf;
        private Pa pa;
    }


}
