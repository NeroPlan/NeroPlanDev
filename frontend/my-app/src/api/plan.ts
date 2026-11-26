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
    actualMinutes: number | null; // 집중 타이머로 기록한 시간(분) - 여러 번 기록하면 누적
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

export interface EstimateStat {
    sampleCount: number;
    averageRatio: number | null; // 실제/예상 평균 - 1.4면 예상보다 1.4배 걸림, 3건 미만이면 null
    overRate: number | null; // 예상보다 오래 걸린 비율(0~1)
    averageEstimatedMinutes: number | null;
    averageActualMinutes: number | null;
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
    estimateStat: EstimateStat; // 예상 vs 실제 - 집중 기록이 있는 완료 계획만
    postponeStat: {
        postponedCount: number;
        postponedCompletionRate: number | null; // 미룬 계획 중 결국 완료한 비율
        onTimeCompletionRate: number | null; // 미루지 않은 계획 중 완료한 비율
    };
}

export interface Category {
    categoryId: number;
    name: string;
    planCount: number; // 이 카테고리로 분류된 계획 수
}

// 완료 잔디 한 칸 - 계획이 없는 날은 응답에 없음
export interface DailyStat {
    date: string; // YYYY-MM-DD
    totalCount: number;
    doneCount: number;
}

// 마이페이지 요약 카드
export interface PlanSummary {
    streakDays: number; // 계획을 하나 이상 완료한 날이 이어진 일수 (오늘 완료가 없으면 어제까지)
    weekTotalCount: number; // 이번 주(일~토) 계획 수
    weekDoneCount: number;
    totalDoneCount: number;
}

// planDate(YYYY-MM-DD)가 없으면 오늘 계획으로 생성 - 지난 날짜는 서버가 거절
export const createPlan = async (
    content: string,
    estimatedMinutes?: number,
    planDate?: string
): Promise<Plan> => {
    const response = await api.post(
        "/api/v1/plans",
        {
            content,
            priority: 1,
            estimatedMinutes,
            planDate
        }
    );

    return response.data.result;
};

export const getPlans = async (): Promise<Plan[]> => {
    const response = await api.get("/api/v1/plans");

    return response.data.result;
};

// 달력 조회 - from~to(YYYY-MM-DD, 양 끝 포함, 최대 62일) 기간의 계획을 날짜순으로 반환
export const getPlansInRange = async (from: string, to: string): Promise<Plan[]> => {
    const response = await api.get("/api/v1/plans", { params: { from, to } });

    return response.data.result;
};

// 오늘 계획을 AI로 다시 분석 - 추천 수행 순서대로 정렬된 오늘 계획 전체(완료 포함) 반환
export const analyzePlans = async (): Promise<Plan[]> => {
    const response = await api.post("/api/v1/plans/analyze");
    return response.data.result;
};

// days가 있으면 최근 days일(오늘 포함) 수행일의 계획만 집계, 없으면 전체
export const getPlanStats = async (days?: number): Promise<PlanStats> => {
    const response = await api.get("/api/v1/plans/stats", { params: days ? { days } : undefined });
    return response.data.result;
};

// from~to(YYYY-MM-DD, 최대 120일)
export const getDailyStats = async (from: string, to: string): Promise<DailyStat[]> => {
    const response = await api.get("/api/v1/plans/stats/daily", { params: { from, to } });
    return response.data.result;
};

export const getPlanSummary = async (): Promise<PlanSummary> => {
    const response = await api.get("/api/v1/plans/stats/summary");
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

// 집중 타이머 기록 - minutes를 기존 기록에 더하고, complete면 완료 처리
export const recordFocus = async (
    planId: number,
    minutes: number,
    complete: boolean
): Promise<Plan> => {
    const response = await api.post(`/api/v1/plans/${planId}/focus`, { minutes, complete });
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

// 카테고리에 분류된 계획 목록 - 최근 수행일부터
export const getCategoryPlans = async (categoryId: number): Promise<Plan[]> => {
    const response = await api.get(`/api/v1/plans/categories/${categoryId}/plans`);
    return response.data.result;
};

export const renameCategory =async (categoryId: number, name: string): Promise<Category> => {
    const response = await api.patch(`/api/v1/plans/categories/${categoryId}`, { name });
    return response.data.result;
};

// moveTo가 없으면 그 카테고리의 계획은 미분류가 되고 다음 분석하기 때 다시 분류됨
export const deleteCategory = async (categoryId: number, moveTo?: number): Promise<void> => {
    await api.delete(`/api/v1/plans/categories/${categoryId}`, {
        params: moveTo ? { moveTo } : undefined,
    });
};

// 회원 탈퇴 - 계획·카테고리 데이터 전체 삭제 (backendUser 탈퇴 전에 호출)
export const deleteMyPlanData = async (): Promise<void> => {
    await api.delete("/api/v1/plans/me");
};

export const deletePlan = async (planId: number): Promise<void> => {
    await api.delete(`/api/v1/plans/${planId}`);
};
