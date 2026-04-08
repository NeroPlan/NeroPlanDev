package com.neroplan.neroplan.domain.plan.controller;

import com.neroplan.neroplan.domain.plan.dto.CreatePlanRequestDto;
import com.neroplan.neroplan.domain.plan.dto.CreatePlanResponseDto;
import com.neroplan.neroplan.domain.plan.dto.GetPlanResponseDto;
import com.neroplan.neroplan.domain.plan.dto.UpdatePlanRequestDto;
import com.neroplan.neroplan.domain.plan.service.PlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.neroplan.neroplan.domain.plan.repository.PlanRepository;

import java.util.List;

@RestController
@RequestMapping("")
@RequiredArgsConstructor
public class PlanController {
    private final PlanService planService;

    // 1. 플랜 생성
    @PostMapping
    public ResponseEntity<CreatePlanResponseDto> createPlan(@RequestBody CreatePlanRequestDto requestDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(planService.createPlan(requestDto));
    }

    // 2. 플랜별 단건 조회
    @GetMapping("/{planId}")
    public ResponseEntity<GetPlanResponseDto> getPlan(@PathVariable Long planId) {
        return ResponseEntity.ok(planService.getPlan(planId));
    }

    // 3. 유저별 전체 조회
    @GetMapping
    public ResponseEntity<List<GetPlanResponseDto>> getPlansByUser(@RequestParam Long userId) {
        return ResponseEntity.ok(planService.getPlansByUserId(userId));
    }

    // 4. 플랜 수정
    @PatchMapping("/{planId}")
    public ResponseEntity<GetPlanResponseDto> updatePlan(
            @PathVariable Long planId,
            @RequestBody UpdatePlanRequestDto requestDto) {
        return ResponseEntity.ok(planService.updatePlan(planId, requestDto));
    }

    // 5. 플랜 삭제
    @DeleteMapping("/{planId}")
    public ResponseEntity<Void> deletePlan(@PathVariable Long planId) {
        planService.deletePlan(planId);
        return ResponseEntity.noContent().build();
    }
}
