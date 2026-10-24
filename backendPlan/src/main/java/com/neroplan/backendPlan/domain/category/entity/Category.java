package com.neroplan.backendPlan.domain.category.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// 유저별 카테고리 - 처음에는 비어 있고, 계획 생성 시 AI가 분류한 이름으로 추가됨
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "name"}))
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Getter
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long categoryId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 50)
    private String name;

    @Builder.Default
    private LocalDateTime createdTime = LocalDateTime.now();
}
