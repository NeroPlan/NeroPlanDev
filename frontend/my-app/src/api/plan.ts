import api from "./api";

export interface Plan {
    planId: number;
    content: string;
    createdTime: string;
    priority: number;
    userId: number;
    categoryId: number | null;
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
