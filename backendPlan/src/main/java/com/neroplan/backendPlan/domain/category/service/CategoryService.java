package com.neroplan.backendPlan.domain.category.service;

import com.neroplan.backendPlan.domain.category.dto.GetCategoryResponseDto;
import com.neroplan.backendPlan.domain.category.entity.Category;
import com.neroplan.backendPlan.domain.category.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;

    // 1. 유저별 카테고리 전체 조회
    @Transactional(readOnly = true)
    public List<GetCategoryResponseDto> getCategories(Long userId) {
        return categoryRepository.findByUserIdOrderByCategoryIdAsc(userId).stream()
                .map(GetCategoryResponseDto::from)
                .toList();
    }

    // AI에 기존 카테고리 범주로 넘겨줄 이름 목록
    @Transactional(readOnly = true)
    public List<String> getCategoryNames(Long userId) {
        return categoryRepository.findByUserIdOrderByCategoryIdAsc(userId).stream()
                .map(Category::getName)
                .toList();
    }

    // 2. AI가 분류한 카테고리 이름으로 조회, 없으면 새로 추가
    @Transactional
    public Category findOrCreate(Long userId, String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String trimmedName = name.trim();
        return categoryRepository.findByUserIdAndName(userId, trimmedName)
                .orElseGet(() -> categoryRepository.save(
                        Category.builder()
                                .userId(userId)
                                .name(trimmedName)
                                .build()));
    }
}
