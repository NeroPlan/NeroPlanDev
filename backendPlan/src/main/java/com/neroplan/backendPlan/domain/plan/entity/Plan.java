package com.neroplan.backendPlan.domain.plan.entity;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Getter
public class Plan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long planId;

    @NotNull
    private String content;

    @NotNull
    private Long priority;

    @Builder.Default
    private LocalDateTime createdTime = LocalDateTime.now();

//    @NotNull
//    user 객체 생성 시 수정
//    @ManyToOne
//    @JoinColumn(name = "user_id")
//    private User user;
    @Column(nullable = true)
    private Long userId;

//    @NotNull
//    category 객체 생성 시 수정
//    @ManyToOne
//    @JoinColumn(name = "category_id")
//    private Category category;
    @Column(nullable = true)
    private Long categoryId;

    @Enumerated(EnumType.STRING)
    @ColumnDefault("'PENDING'")
    @Builder.Default
    private PlanStatus status = PlanStatus.PENDING;

    @Column(nullable = true)
    private LocalDateTime completedTime;

    // aiService 모델 입력 피처 (estimated_minutes, time_slot)
    @Column(nullable = true)
    private Integer estimatedMinutes;

    @Column(nullable = true)
    private String timeSlot;

    public void updatePlan(String content, Long priority) {
        if (content != null) this.content = content;
        if (priority != null) this.priority = priority;
    }

    public void updateStatus(PlanStatus status) {
        if (status == null) return;
        this.status = status;
        this.completedTime = (status == PlanStatus.COMPLETED) ? LocalDateTime.now() : null;
    }
}
