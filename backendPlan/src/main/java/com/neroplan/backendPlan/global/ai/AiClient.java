package com.neroplan.backendPlan.global.ai;

import com.neroplan.backendPlan.global.ai.dto.ClassifyCategoryRequestDto;
import com.neroplan.backendPlan.global.ai.dto.ClassifyCategoryResponseDto;
import com.neroplan.backendPlan.global.ai.dto.SchedulePlansRequestDto;
import com.neroplan.backendPlan.global.ai.dto.SchedulePlansResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Optional;

// aiService 호출 - aiService가 응답하지 않아도 계획 기능은 동작해야 하므로 실패 시 Optional.empty() 반환
@Slf4j
@Component
@RequiredArgsConstructor
public class AiClient {

    private static final String CLASSIFY_URI = "/api/ai/plans/classify";
    private static final String ANALYZE_URI = "/api/ai/analyze";

    private final RestClient aiRestClient;

    // 1. 계획 내용으로 카테고리 분류 - 기존 카테고리에 없으면 aiService가 새 이름을 만들어 반환
    public Optional<String> classifyCategory(String content, List<String> categories) {
        try {
            ClassifyCategoryResponseDto response = aiRestClient.post()
                    .uri(CLASSIFY_URI)
                    .body(new ClassifyCategoryRequestDto(content, categories))
                    .retrieve()
                    .body(ClassifyCategoryResponseDto.class);
            return Optional.ofNullable(response).map(ClassifyCategoryResponseDto::getCategory);
        } catch (RestClientException e) {
            log.warn("aiService 카테고리 분류 실패: {}", e.getMessage());
            return Optional.empty();
        }
    }

    // 2. 위험도 분석 및 계획 재배열 - 응답의 plans 순서가 추천 수행 순서
    public Optional<SchedulePlansResponseDto> analyzePlans(SchedulePlansRequestDto requestDto) {
        try {
            return Optional.ofNullable(aiRestClient.post()
                    .uri(ANALYZE_URI)
                    .body(requestDto)
                    .retrieve()
                    .body(SchedulePlansResponseDto.class));
        } catch (RestClientException e) {
            log.warn("aiService 계획 분석 실패: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
