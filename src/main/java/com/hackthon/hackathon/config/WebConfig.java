package com.hackthon.hackathon.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 프로젝트의 모든 주소(API)에 적용
                .allowedOriginPatterns("*") // 모든 프론트엔드 주소 허용
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

}
