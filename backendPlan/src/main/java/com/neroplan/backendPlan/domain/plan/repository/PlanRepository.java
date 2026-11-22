package com.neroplan.backendPlan.domain.plan.repository;

import com.neroplan.backendPlan.domain.category.entity.Category;
import com.neroplan.backendPlan.domain.plan.entity.Plan;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    // 카테고리 관리 - 카테고리별 계획 수 [categoryId, count]
    @Query("SELECT p.category.categoryId, COUNT(p) FROM Plan p WHERE p.userId = :userId AND p.category IS NOT NULL GROUP BY p.category.categoryId")
    List<Object[]> countByCategory(@Param("userId") Long userId);

    // 카테고리 삭제 시 계획을 다른 카테고리(또는 미분류 null)로 옮김
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Plan p SET p.category = :target WHERE p.userId = :userId AND p.category.categoryId = :sourceId")
    int moveCategory(@Param("userId") Long userId, @Param("sourceId") Long sourceId,
                     @Param("target") Category target);

    // 회원 탈퇴
    @Modifying
    @Query("DELETE FROM Plan p WHERE p.userId = :userId")
    int deleteAllByUserId(@Param("userId") Long userId);
}