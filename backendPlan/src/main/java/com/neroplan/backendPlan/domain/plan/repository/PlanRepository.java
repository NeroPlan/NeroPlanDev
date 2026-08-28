package com.neroplan.backendPlan.domain.plan.repository;

import com.neroplan.backendPlan.domain.plan.entity.Plan;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface PlanRepository extends JpaRepository<Plan, Long>{
    List<Plan> findByUserId(Long userId);
}