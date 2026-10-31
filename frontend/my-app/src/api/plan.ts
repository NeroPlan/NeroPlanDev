import api from "./api";

export type PlanStatus = "TODO" | "DONE";

// AI 위험 판정 상태 - 기록 없음 / 기록 10건 미만(규칙 판정) / 기록 10건 이상(모델 판정)
export type AnalysisStatus = "NO_RECORD" | "COLLECTING" | "PREDICTED";

export interface Plan {
    planId: number;
    content: string;
    createdTime: string;
    priority: number;
    userId: number;
    categoryId: number | null;
    categoryName: string | null;
    estimatedMinutes: number | null;
    planDate: string; // 수행 예정일 (YYYY-MM-DD)
    postponeCount: number;
    status: PlanStatus;
    completedTime: string | null;

    // AI 재배열 결과 - 조회 응답에만 포함되고 생성 응답에는 없음, 분석 전이면 null
    recommendedOrder?: number | null;
    analysisStatus?: AnalysisStatus | null;
    recordCount?: number | null; // 같은 카테고리의 확정된 과거 기록 수
    successRate?: number | null; // 모델 판정일 때만 - 예측 성공률(0~1)
    completionRate?: number | null; // 기록 수집 중일 때만 - 지금까지 실제 완료율(0~1)
    priorityReason?: string | null; // AI 판단 근거 문장
    isBiased?: boolean | null;
    biasedReason?: string | null;
}

// 분석 페이지 통계 - 성공률은 계획한 날 완료한 비율(0~1), 기록이 3건 미만이면 null
export interface CategoryStat {
    categoryName: string | null; // 미분류면 null
    recordCount: number;
    doneCount: number;
    postponedCount: number;
    failedCount: number;
    successRate: number | null;
    aiRecordCount: number; // AI가 쓰는 기록 수(소요시간이 있는 기록) - 10건부터 성공률 예측
}

export interface RateStat {
    label: string;
    recordCount: number;
    successRate: number | null;
}

export interface PlanStats {
    recordCount: number; // 결과가 확정된 계획 수
    successRate: number | null;
    categories: CategoryStat[]; // 기록 많은 순
    durationStats: RateStat[]; // 예상 소요시간 구간별
    dailyLoadStats: RateStat[]; // 하루에 세운 계획 수 구간별
    postponeStat: {
        postponedCount: number;
        postponedCompletionRate: number | null; // 미룬 계획 중 결국 완료한 비율
        onTimeCompletionRate: number | null; // 미루지 않은 계획 중 완료한 비율
    };
}

export interface Category {
    categoryId: number;
    name: string;
}

export const createPlan = async (
    content: string,
    estimatedMinutes?: number
): Promise<Plan> => {
    const response = await api.post(
        "/api/v1/plans",
        {
            content,
            priority: 1,
            estimatedMinutes
        }
    );

    return response.data.result;
};

export const getPlans = async (): Promise<Plan[]> => {
    const response = await api.get("/api/v1/plans");

    return response.data.result;
};

// 오늘 계획을 AI로 다시 분석 - 추천 수행 순서대로 정렬된 오늘 계획 전체(완료 포함) 반환
export const analyzePlans = async (): Promise<Plan[]> => {
    const response = await api.post("/api/v1/plans/analyze");
    return response.data.result;
};

export const getPlanStats = async (): Promise<PlanStats> => {
    const response = await api.get("/api/v1/plans/stats");
    return response.data.result;
};

export const updatePlan = async (
    planId: number,
    data: { content?: string; priority?: number; estimatedMinutes?: number }
): Promise<Plan> => {
    const response = await api.patch(`/api/v1/plans/${planId}`, data);
    return response.data.result;
};

// completed가 false면 완료 취소
export const completePlan = async (
    planId: number,
    completed: boolean
): Promise<Plan> => {
    const response = await api.patch(`/api/v1/plans/${planId}/complete`, { completed });
    return response.data.result;
};

// toDate(YYYY-MM-DD)가 없으면 기존 수행일 다음 날로 연기
export const postponePlan = async (
    planId: number,
    toDate?: string
): Promise<Plan> => {
    const response = await api.patch(
        `/api/v1/plans/${planId}/postpone`,
        toDate ? { toDate } : undefined
    );
    return response.data.result;
};

export const getCategories = async (): Promise<Category[]> => {
    const response = await api.get("/api/v1/plans/categories");
    return response.data.result;
};

export const deletePlan = async (planId: number): Promise<void> => {
    await api.delete(`/api/v1/plans/${planId}`);
};
