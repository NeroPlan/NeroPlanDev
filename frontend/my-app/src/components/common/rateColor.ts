// 성공률(0~1) 막대 색 - 계획 페이지 분석 결과와 분석 페이지가 같은 기준을 사용
export const getRateColor = (rate: number) => {
    if (rate >= 0.7) return "bg-green-500";
    if (rate >= 0.4) return "bg-yellow-400";
    return "bg-red-500";
};

export const toPercent = (rate: number) => `${Math.round(rate * 100)}%`;

// 기록이 이보다 적으면 서버가 성공률을 null로 줌 - backendPlan PlanStatsService와 같은 값
export const MIN_RECORDS_FOR_RATE = 3;

// 성공률이 없을 때 이유를 짧게 표시
export const getMissingRateText = (recordCount: number) =>
    recordCount === 0 ? "기록 없음" : `${MIN_RECORDS_FOR_RATE}건부터 표시`;
