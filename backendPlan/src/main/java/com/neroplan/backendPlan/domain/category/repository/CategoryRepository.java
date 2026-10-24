package com.neroplan.backendPlan.domain.category.repository;

import com.neroplan.backendPlan.domain.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByUserIdOrderByCategoryIdAsc(Long userId);
    Optional<Category> findByUserIdAndName(Long userId, String name);
}
