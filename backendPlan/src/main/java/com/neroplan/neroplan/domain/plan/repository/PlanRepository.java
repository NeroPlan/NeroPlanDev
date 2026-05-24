package com.neroplan.neroplan.domain.plan.repository;

import com.neroplan.neroplan.domain.plan.dto.CreatePlanRequestDto;
import com.neroplan.neroplan.domain.plan.dto.CreatePlanResponseDto;
import com.neroplan.neroplan.domain.plan.dto.GetPlanResponseDto;
import com.neroplan.neroplan.domain.plan.dto.UpdatePlanRequestDto;
import com.neroplan.neroplan.domain.plan.entity.Plan;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface PlanRepository extends JpaRepository<Plan, Long>{
    List<Plan> findByUserId(Long userId);

}