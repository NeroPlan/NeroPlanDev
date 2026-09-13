import api from "./api";

export const createPlan = async (
    content: string
) => {
    const response = await api.post(
        "/api/v1/plans",
        {
            content,
            priority: 1
        }
    );

    return response.data;
};