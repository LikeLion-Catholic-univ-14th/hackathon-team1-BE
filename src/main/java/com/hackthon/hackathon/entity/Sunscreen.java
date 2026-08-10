package com.hackthon.hackathon.entity;

import com.hackthon.hackathon.enums.Pa;
import com.hackthon.hackathon.enums.SunscreenFilterType;
import com.hackthon.hackathon.enums.SunscreenProductType;
import jakarta.persistence.*;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.*;

@Entity
@Table(
        name = "sunscreen",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {"user_id", "brand", "name"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Sunscreen {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sunscreen_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 20)
    private String name;

    @Column(nullable = false, length = 20)
    private String brand;

    @Enumerated(EnumType.STRING)
    private SunscreenFilterType filterType;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_type", nullable = false)
    private SunscreenProductType productType;

    @Column(nullable = false)
    private String spf;//erd에는 숫자입력으로 했는데 08.11 00:08 기준 50+ 입력 사항 때문에 string으로 바꿀게용

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Pa pa;
}
