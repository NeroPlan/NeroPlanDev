import axios from "axios";

const API_URL = "http://localhost:8080/api/v1";   // 백엔드 주소

export const createPlan = async (content: string) => {
    const response = await axios.post(`${API_URL}/plans`, {
        content,
        priority: 1
    });

    return response.data;
};