package com.neroplan.backendPlan.domain.plan.repository;

import com.neroplan.backendPlan.domain.plan.entity.Plan;
import com.neroplan.backendPlan.domain.plan.entity.PlanDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface PlanDetailRepository extends JpaRepository<PlanDetail, Long> {
    List<PlanDetail> findByPlanIn(Collection<Plan> plans);
    void deleteByPlan(Plan plan);
}
