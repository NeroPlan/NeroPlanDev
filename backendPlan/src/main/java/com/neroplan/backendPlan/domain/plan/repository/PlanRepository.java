package com.neroplan.backendPlan.domain.plan.repository;

import com.neroplan.backendPlan.domain.plan.entity.Plan;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;


public interface PlanRepository extends JpaRepository<Plan, Long>{
    @EntityGraph(attributePaths = "category")
    List<Plan> findByUserId(Long userId);
    Optional<Plan> findByPlanIdAndUserId(Long planId, Long userId);
    // 분석하기에서 카테고리 분류가 필요한 계획
    List<Plan> findByUserIdAndCategoryIsNull(Long userId);
    @EntityGraph(attributePaths = "category")
    List<Plan> findByUserIdAndPlanDate(Long userId, LocalDate planDate);
    // 달력 조회 - from~to(양 끝 포함) 기간의 계획
    @EntityGraph(attributePaths = "category")
    List<Plan> findByUserIdAndPlanDateBetween(Long userId, LocalDate from, LocalDate to);
}