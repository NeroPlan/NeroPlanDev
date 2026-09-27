import api from "./api";

// aiService 인지편향 모델의 outcome(완료/미룸/실패)과 매핑되는 계획 상태
// PENDING: 진행 전, COMPLETED: 완료, POSTPONED: 연기(미룸), FAILED: 실패
export type PlanStatus =
    | "PENDING"
    | "COMPLETED"
    | "POSTPONED"
    | "FAILED";

export interface Plan {
    planId: number;
    content: string;
    createdTime: string;
    priority: number;
    userId: number;
    categoryId: number | null;
    status: PlanStatus;
    completedTime: string | null;
    // aiService 모델 입력 피처 (estimated_minutes, time_slot)
    estimatedMinutes: number | null;
    timeSlot: string | null;
}

export interface UpdatePlanRequest {
    content?: string;
    priority?: number;
    status?: PlanStatus;
}

export const createPlan = async (
    content: string
): Promise<Plan> => {
    const response = await api.post(
        "/api/v1/plans",
        {
            content,
            priority: 1
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
    data: UpdatePlanRequest
): Promise<Plan> => {
    const response = await api.patch(`/api/v1/plans/${planId}`, data);
    return response.data.result;
};
