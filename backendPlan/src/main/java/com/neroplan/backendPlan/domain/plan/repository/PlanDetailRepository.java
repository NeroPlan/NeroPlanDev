package com.neroplan.backendPlan.domain.plan.repository;

import com.neroplan.backendPlan.domain.plan.entity.Plan;
import com.neroplan.backendPlan.domain.plan.entity.PlanDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public interface PlanDetailRepository extends JpaRepository<PlanDetail, Long> {
    List<PlanDetail> findByPlanIn(Collection<Plan> plans);
    void deleteByPlan(Plan plan);

    // 회원 탈퇴 - plan보다 먼저 지워야 FK 오류가 나지 않음
    @Modifying
    @Query("DELETE FROM PlanDetail d WHERE d.plan.planId IN (SELECT p.planId FROM Plan p WHERE p.userId = :userId)")
    int deleteAllByUserId(@Param("userId") Long userId);

    // planId -> PlanDetail
    // unique 제약 추가 전에 쌓인 중복 행이 있어도 예외 없이 가장 최근 행을 사용
    default Map<Long, PlanDetail> findMapByPlanIn(Collection<Plan> plans) {
        return findByPlanIn(plans).stream()
                .collect(Collectors.toMap(
                        detail -> detail.getPlan().getPlanId(),
                        Function.identity(),
                        (a, b) -> a.getPlanDetailId() >= b.getPlanDetailId() ? a : b));
    }

    // 같은 사용자의 재배열 계산을 한 번에 하나만 실행 - 트랜잭션이 끝나면 자동으로 풀림
    // 비동기 재계산(계획 변경)과 동기 재계산(분석하기)이 겹쳐 PlanDetail을 중복 저장하는 것을 막음
    @Query(value = "SELECT 1 FROM pg_advisory_xact_lock(:userId)", nativeQuery = true)
    Integer lockScheduleByUserId(@Param("userId") long userId);
}
