package com.neroplan.backendPlan.domain.plan.repository;

import com.neroplan.backendPlan.domain.plan.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


public interface PlanRepository extends JpaRepository<Plan, Long>{
    List<Plan> findByUserId(Long userId);
    Optional<Plan> findByPlanIdAndUserId(Long planId, Long userId);
    List<Plan> findByUserIdAndCreatedTimeBetween(Long userId, LocalDateTime start, LocalDateTime end);
}