package com.neroplan.neroplan.domain.plan.entity;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

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

    @NotNull
    private LocalDateTime createdTime;

    @NotNull
//    user 객체 생성 시 수정
//    @ManyToOne
//    @JoinColumn(name = "user_id")
//    private User user;
    private Long userId;

    @NotNull
//    category 객체 생성 시 수정
//    @ManyToOne
//    @JoinColumn(name = "category_id")
//    private Category category;
    private Long categoryId;

    public void updatePlan(String content, Long priority) {
        if (content != null) this.content = content;
        if (priority != null) this.priority = priority;
    }
}
