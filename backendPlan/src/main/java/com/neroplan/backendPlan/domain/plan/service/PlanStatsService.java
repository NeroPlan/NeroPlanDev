package com.neroplan.backendPlan.domain.plan.service;

import com.neroplan.backendPlan.domain.plan.dto.GetPlanStatsResponseDto;
import com.neroplan.backendPlan.domain.plan.dto.GetPlanStatsResponseDto.CategoryStat;
import com.neroplan.backendPlan.domain.plan.dto.GetPlanStatsResponseDto.PostponeStat;
import com.neroplan.backendPlan.domain.plan.dto.GetPlanStatsResponseDto.RateStat;
import com.neroplan.backendPlan.domain.plan.entity.Plan;
import com.neroplan.backendPlan.domain.plan.entity.PlanOutcome;
import com.neroplan.backendPlan.domain.plan.entity.PlanStatus;
import com.neroplan.backendPlan.domain.plan.repository.PlanRepository;
import com.neroplan.backendPlan.global.apiPayload.code.status.ErrorStatus;
import com.neroplan.backendPlan.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

// 분석 페이지 통계 - 유저의 전체 기록으로 카테고리별 성공률과 수행 패턴을 계산
@Service
@RequiredArgsConstructor
public class PlanStatsService {

    // 기록이 이보다 적으면 성공률을 보여주지 않음 - aiService의 MIN_RECORDS_FOR_RATE와 같은 값
    private static final int MIN_RECORDS_FOR_RATE = 3;

    private final PlanRepository planRepository;

    @Transactional(readOnly = true)
    public GetPlanStatsResponseDto getStats(Long userId) {
        if (userId == null) {
            throw new GeneralException(ErrorStatus._UNAUTHORIZED);
        }
        LocalDate today = LocalDate.now();
        List<Plan> userPlans = planRepository.findByUserId(userId);

        Map<Plan, PlanOutcome> outcomes = new LinkedHashMap<>();
        userPlans.forEach(plan -> {
            PlanOutcome outcome = PlanOutcome.of(plan, today);
            if (outcome != null) {
                outcomes.put(plan, outcome);
            }
        });

        return new GetPlanStatsResponseDto(
                outcomes.size(),
                successRate(outcomes.values()),
                toCategoryStats(outcomes),
                toDurationStats(outcomes),
                toDailyLoadStats(userPlans, outcomes, today),
                toPostponeStat(outcomes));
    }

    private List<CategoryStat> toCategoryStats(Map<Plan, PlanOutcome> outcomes) {
        // 미분류(null)도 하나의 묶음으로 집계 - Collectors.groupingBy는 null 키를 허용하지 않아 직접 묶음
        Map<String, List<Plan>> plansByCategory = new LinkedHashMap<>();
        outcomes.keySet().forEach(plan -> plansByCategory
                .computeIfAbsent(plan.getCategory() != null ? plan.getCategory().getName() : null, key -> new ArrayList<>())
                .add(plan));

        return plansByCategory.entrySet().stream()
                .map(entry -> {
                    List<PlanOutcome> categoryOutcomes = entry.getValue().stream().map(outcomes::get).toList();
                    return new CategoryStat(
                            entry.getKey(),
                            categoryOutcomes.size(),
                            count(categoryOutcomes, PlanOutcome.DONE),
                            count(categoryOutcomes, PlanOutcome.POSTPONED),
                            count(categoryOutcomes, PlanOutcome.FAILED),
                            successRate(categoryOutcomes),
                            (int) entry.getValue().stream().filter(plan -> plan.getEstimatedMinutes() != null).count());
                })
                .sorted(Comparator.comparing(CategoryStat::getRecordCount).reversed())
                .toList();
    }

    private List<RateStat> toDurationStats(Map<Plan, PlanOutcome> outcomes) {
        Map<String, List<PlanOutcome>> byBin = emptyBins("30분 이하", "31~60분", "60분 초과");
        outcomes.forEach((plan, outcome) -> {
            Integer minutes = plan.getEstimatedMinutes();
            if (minutes == null) {
                return;
            }
            String bin = minutes <= 30 ? "30분 이하" : minutes <= 60 ? "31~60분" : "60분 초과";
            byBin.get(bin).add(outcome);
        });
        return toRateStats(byBin);
    }

    // 지난 날짜마다 그날 세운 계획 수로 구간을 나누고, 그날 계획들의 성공률을 집계
    // 미룬 계획은 옮겨 간 날짜에 세운 계획으로 셈
    private List<RateStat> toDailyLoadStats(List<Plan> userPlans, Map<Plan, PlanOutcome> outcomes, LocalDate today) {
        Map<LocalDate, List<Plan>> plansByDate = userPlans.stream()
                .filter(plan -> plan.getPlanDate().isBefore(today))
                .collect(Collectors.groupingBy(Plan::getPlanDate));

        Map<String, List<PlanOutcome>> byBin = emptyBins("1~2개", "3~4개", "5개 이상");
        plansByDate.values().forEach(plans -> {
            String bin = plans.size() <= 2 ? "1~2개" : plans.size() <= 4 ? "3~4개" : "5개 이상";
            plans.forEach(plan -> byBin.get(bin).add(outcomes.get(plan)));
        });
        return toRateStats(byBin);
    }

    // 미룬 계획은 판정상 모두 미룸이므로, 성공률 대신 실제로 완료했는지(status)를 비교
    private PostponeStat toPostponeStat(Map<Plan, PlanOutcome> outcomes) {
        Map<Boolean, List<Plan>> byPostponed = outcomes.keySet().stream()
                .collect(Collectors.partitioningBy(plan -> plan.getPostponeCount() > 0));
        List<Plan> postponed = byPostponed.get(true);
        List<Plan> onTime = byPostponed.get(false);

        return new PostponeStat(postponed.size(), completionRate(postponed), completionRate(onTime));
    }

    private Map<String, List<PlanOutcome>> emptyBins(String... labels) {
        return Arrays.stream(labels)
                .collect(Collectors.toMap(Function.identity(), label -> new ArrayList<>(),
                        (a, b) -> a, LinkedHashMap::new));
    }

    private List<RateStat> toRateStats(Map<String, List<PlanOutcome>> byBin) {
        return byBin.entrySet().stream()
                .map(entry -> new RateStat(entry.getKey(), entry.getValue().size(), successRate(entry.getValue())))
                .toList();
    }

    private int count(Collection<PlanOutcome> outcomes, PlanOutcome target) {
        return (int) outcomes.stream().filter(outcome -> outcome == target).count();
    }

    private Double successRate(Collection<PlanOutcome> outcomes) {
        if (outcomes.size() < MIN_RECORDS_FOR_RATE) {
            return null;
        }
        return round((double) count(outcomes, PlanOutcome.DONE) / outcomes.size());
    }

    private Double completionRate(List<Plan> plans) {
        if (plans.size() < MIN_RECORDS_FOR_RATE) {
            return null;
        }
        long done = plans.stream().filter(plan -> plan.getStatus() == PlanStatus.DONE).count();
        return round((double) done / plans.size());
    }

    private double round(double rate) {
        return Math.round(rate * 1000) / 1000.0;
    }
}
