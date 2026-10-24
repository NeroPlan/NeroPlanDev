package com.neroplan.backendPlan.domain.plan.entity;


import com.neroplan.backendPlan.domain.category.entity.Category;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
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

    // 예상 소요 시간(분) - AI 위험도 판정에 사용
    private Integer estimatedMinutes;

    @Builder.Default
    private LocalDateTime createdTime = LocalDateTime.now();

    // 계획 수행 예정일 - 연기 시 이 값만 변경되며 createdTime은 유지
    @Builder.Default
    @Column(nullable = false, columnDefinition = "date default current_date")
    private LocalDate planDate = LocalDate.now();

    @Builder.Default
    @Column(nullable = false, columnDefinition = "integer default 0")
    private Integer postponeCount = 0;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'TODO'")
    @Column(nullable = false, length = 20)
    private PlanStatus status = PlanStatus.TODO;

    private LocalDateTime completedTime;

//    @NotNull
//    user 객체 생성 시 수정
//    @ManyToOne
//    @JoinColumn(name = "user_id")
//    private User user;
    @Column(nullable = true)
    private Long userId;

    // AI 분류 전에는 null
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    public void updatePlan(String content, Long priority, Integer estimatedMinutes) {
        if (content != null) this.content = content;
        if (priority != null) this.priority = priority;
        if (estimatedMinutes != null) this.estimatedMinutes = estimatedMinutes;
    }

    public void assignCategory(Category category) {
        this.category = category;
    }

    public void postpone(LocalDate toDate) {
        this.planDate = toDate;
        this.postponeCount++;
    }

    public void complete() {
        this.status = PlanStatus.DONE;
        this.completedTime = LocalDateTime.now();
    }

    public void cancelComplete() {
        this.status = PlanStatus.TODO;
        this.completedTime = null;
    }
}
