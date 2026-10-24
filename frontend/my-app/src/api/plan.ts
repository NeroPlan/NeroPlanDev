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
    successRate?: number | null;
    isBiased?: boolean | null;
    biasedReason?: string | null;
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
