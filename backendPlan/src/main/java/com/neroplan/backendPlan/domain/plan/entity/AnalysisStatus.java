package com.neroplan.backendPlan.domain.plan.entity;

// aiService 위험 판정 상태 - 카테고리 기록이 10건 이상이어야 모델로 판정
public enum AnalysisStatus {
    NO_RECORD,   // 같은 카테고리 기록 없음
    COLLECTING,  // 기록 10건 미만 - 연속 실패 횟수로 규칙 판정
    PREDICTED;   // 기록 10건 이상 - 모델 판정 (성공률, 근거 문장 있음)

    // aiService가 보내는 상태 문장("기록 없음" / "기록 수집 중(규칙판정)" / "판정됨(모델)")을 변환
    public static AnalysisStatus fromAiStatus(String status) {
        if (status == null) return null;
        if (status.startsWith("기록 없음")) return NO_RECORD;
        if (status.startsWith("기록 수집 중")) return COLLECTING;
        if (status.startsWith("판정됨")) return PREDICTED;
        return null;
    }
}
