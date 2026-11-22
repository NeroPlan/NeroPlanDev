package com.neroplan.backendPlan.domain.category.repository;

import com.neroplan.backendPlan.domain.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByUserIdOrderByCategoryIdAsc(Long userId);
    Optional<Category> findByUserIdAndName(Long userId, String name);
    Optional<Category> findByCategoryIdAndUserId(Long categoryId, Long userId);

    // 회원 탈퇴
    @Modifying
    @Query("DELETE FROM Category c WHERE c.userId = :userId")
    int deleteAllByUserId(@Param("userId") Long userId);

    // 같은 이름이 이미 있으면 아무것도 하지 않음 - 동시에 분석하기를 눌러 같은 카테고리를 만들려 해도 unique 제약 오류가 나지 않음
    // (다른 트랜잭션이 같은 이름을 넣는 중이면 그 커밋을 기다린 뒤 건너뜀)
    @Modifying
    @Query(value = "INSERT INTO category (user_id, name, created_time) VALUES (:userId, :name, :createdTime) "
            + "ON CONFLICT (user_id, name) DO NOTHING", nativeQuery = true)
    int insertIfAbsent(@Param("userId") Long userId, @Param("name") String name,
                       @Param("createdTime") LocalDateTime createdTime);
}
