package com.neroplan.backendPlan.domain.category.service;

import com.neroplan.backendPlan.domain.category.dto.GetCategoryResponseDto;
import com.neroplan.backendPlan.domain.category.entity.Category;
import com.neroplan.backendPlan.domain.category.repository.CategoryRepository;
import com.neroplan.backendPlan.domain.plan.dto.GetPlanResponseDto;
import com.neroplan.backendPlan.domain.plan.repository.PlanRepository;
import com.neroplan.backendPlan.global.apiPayload.code.status.ErrorStatus;
import com.neroplan.backendPlan.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryService {

    private static final int MAX_NAME_LENGTH = 50; // category.name 컬럼 길이

    private final CategoryRepository categoryRepository;
    private final PlanRepository planRepository;

    // 1. 유저별 카테고리 전체 조회 - 카테고리별 계획 수 포함
    @Transactional(readOnly = true)
    public List<GetCategoryResponseDto> getCategories(Long userId) {
        Map<Long, Long> counts = planRepository.countByCategory(userId).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
        return categoryRepository.findByUserIdOrderByCategoryIdAsc(userId).stream()
                .map(category -> GetCategoryResponseDto.from(category,
                        counts.getOrDefault(category.getCategoryId(), 0L)))
                .toList();
    }

    // 5. 카테고리에 속한 계획 목록 - 이름을 바꾸기 전에 어떤 계획이 묶여 있는지 확인용
    @Transactional(readOnly = true)
    public List<GetPlanResponseDto> getPlans(Long userId, Long categoryId) {
        findCategory(userId, categoryId);
        return planRepository.findByUserIdAndCategoryCategoryIdOrderByPlanDateDescCreatedTimeDesc(userId, categoryId)
                .stream()
                .map(GetPlanResponseDto::from)
                .toList();
    }

    // 3. 이름 변경 - 같은 사용자 안에서 이름은 겹칠 수 없음
    @Transactional
    public GetCategoryResponseDto rename(Long userId, Long categoryId, String name) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty() || trimmed.length() > MAX_NAME_LENGTH) {
            throw new GeneralException(ErrorStatus._INVALID_CATEGORY_NAME);
        }
        Category category = findCategory(userId, categoryId);
        categoryRepository.findByUserIdAndName(userId, trimmed)
                .filter(other -> !other.getCategoryId().equals(categoryId))
                .ifPresent(other -> {
                    throw new GeneralException(ErrorStatus._DUPLICATED_CATEGORY_NAME);
                });
        category.rename(trimmed);
        return GetCategoryResponseDto.from(category);
    }

    // 4. 삭제 - 계획은 moveTo 카테고리로 옮기거나(null이면) 미분류로 둠
    @Transactional
    public void delete(Long userId, Long categoryId, Long moveTo) {
        Category category = findCategory(userId, categoryId);
        Category target = null;
        if (moveTo != null) {
            if (moveTo.equals(categoryId)) {
                throw new GeneralException(ErrorStatus._INVALID_CATEGORY_MOVE);
            }
            target = findCategory(userId, moveTo);
        }
        planRepository.moveCategory(userId, categoryId, target);
        categoryRepository.delete(category);
    }

    private Category findCategory(Long userId, Long categoryId) {
        return categoryRepository.findByCategoryIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._CATEGORY_NOT_FOUND));
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
                .orElseGet(() -> {
                    // 조회와 저장 사이에 다른 요청이 먼저 만들 수 있으므로 충돌 시 건너뛰고 다시 조회
                    categoryRepository.insertIfAbsent(userId, trimmedName, LocalDateTime.now());
                    return categoryRepository.findByUserIdAndName(userId, trimmedName)
                            .orElseThrow(() -> new IllegalStateException("카테고리 생성 실패: " + trimmedName));
                });
    }
}
