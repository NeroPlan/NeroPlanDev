package com.neroplan.backendPlan.global.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ClassifyCategoryRequestDto {

    private String content;

    // 유저가 현재 가진 카테고리 이름 목록 (처음에는 빈 배열)
    private List<String> categories;
}
