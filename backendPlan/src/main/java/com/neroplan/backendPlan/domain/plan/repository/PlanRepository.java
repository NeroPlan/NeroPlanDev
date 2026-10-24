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
    @EntityGraph(attributePaths = "category")
    List<Plan> findByUserIdAndPlanDate(Long userId, LocalDate planDate);
}