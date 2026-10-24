package com.neroplan.backendPlan.domain.plan.entity;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
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
    @Column(nullable = false, columnDefinition = "varchar(20) default 'TODO'")
    private PlanStatus status = PlanStatus.TODO;

    private LocalDateTime completedTime;

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

    public void updatePlan(String content, Long priority) {
        if (content != null) this.content = content;
        if (priority != null) this.priority = priority;
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
