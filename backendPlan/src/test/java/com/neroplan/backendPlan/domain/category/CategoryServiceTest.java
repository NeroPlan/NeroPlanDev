package com.neroplan.backendPlan.domain.category;

import com.neroplan.backendPlan.domain.category.dto.GetCategoryResponseDto;
import com.neroplan.backendPlan.domain.category.entity.Category;
import com.neroplan.backendPlan.domain.category.repository.CategoryRepository;
import com.neroplan.backendPlan.domain.category.service.CategoryService;
import com.neroplan.backendPlan.domain.plan.repository.PlanRepository;
import com.neroplan.backendPlan.global.apiPayload.exception.GeneralException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// 카테고리 관리 - 조회(계획 수), 이름 변경, 삭제
class CategoryServiceTest {

    private final CategoryRepository categoryRepository = mock(CategoryRepository.class);
    private final PlanRepository planRepository = mock(PlanRepository.class);
    private final CategoryService categoryService = new CategoryService(categoryRepository, planRepository);

    private final Category study = Category.builder().categoryId(1L).userId(1L).name("공부").build();
    private final Category workout = Category.builder().categoryId(2L).userId(1L).name("운동").build();

    @Test
    void 조회는_카테고리별_계획_수를_함께_준다() {
        when(categoryRepository.findByUserIdOrderByCategoryIdAsc(1L)).thenReturn(List.of(study, workout));
        when(planRepository.countByCategory(1L)).thenReturn(List.<Object[]>of(new Object[]{1L, 5L}));

        assertThat(categoryService.getCategories(1L))
                .extracting(GetCategoryResponseDto::getName, GetCategoryResponseDto::getPlanCount)
                .containsExactly(tuple("공부", 5L), tuple("운동", 0L));
    }

    @Test
    void 이름을_바꾸면_앞뒤_공백을_지우고_저장한다() {
        when(categoryRepository.findByCategoryIdAndUserId(1L, 1L)).thenReturn(Optional.of(study));
        when(categoryRepository.findByUserIdAndName(1L, "알고리즘")).thenReturn(Optional.empty());

        assertThat(categoryService.rename(1L, 1L, "  알고리즘 ").getName()).isEqualTo("알고리즘");
    }

    @Test
    void 다른_카테고리와_이름이_겹치거나_비어_있으면_거절한다() {
        when(categoryRepository.findByCategoryIdAndUserId(1L, 1L)).thenReturn(Optional.of(study));
        when(categoryRepository.findByUserIdAndName(1L, "운동")).thenReturn(Optional.of(workout));

        assertThatThrownBy(() -> categoryService.rename(1L, 1L, "운동")).isInstanceOf(GeneralException.class);
        assertThatThrownBy(() -> categoryService.rename(1L, 1L, "  ")).isInstanceOf(GeneralException.class);
        assertThat(study.getName()).isEqualTo("공부");
    }

    @Test
    void 삭제하면_계획을_지정한_카테고리로_옮긴다() {
        when(categoryRepository.findByCategoryIdAndUserId(1L, 1L)).thenReturn(Optional.of(study));
        when(categoryRepository.findByCategoryIdAndUserId(2L, 1L)).thenReturn(Optional.of(workout));

        categoryService.delete(1L, 1L, 2L);

        verify(planRepository).moveCategory(1L, 1L, workout);
        verify(categoryRepository).delete(study);
    }

    @Test
    void 옮길_곳이_없으면_미분류로_두고_자기_자신으로는_옮길_수_없다() {
        when(categoryRepository.findByCategoryIdAndUserId(1L, 1L)).thenReturn(Optional.of(study));

        categoryService.delete(1L, 1L, null);
        verify(planRepository).moveCategory(1L, 1L, null);

        assertThatThrownBy(() -> categoryService.delete(1L, 1L, 1L)).isInstanceOf(GeneralException.class);
    }

    @Test
    void 다른_사용자의_카테고리는_찾을_수_없다() {
        when(categoryRepository.findByCategoryIdAndUserId(1L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.delete(2L, 1L, null)).isInstanceOf(GeneralException.class);
        verify(categoryRepository, never()).delete(any());
    }
}
