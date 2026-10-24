import axios from "axios";
import { getAccessToken, removeAccessToken } from "./auth";

const api = axios.create({
  baseURL: "http://localhost:8080",
});

api.interceptors.request.use((config) => {
  const token = getAccessToken();

  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }

  return config;
});

// 토큰이 없거나 만료되면 서버가 403을 반환 - 토큰을 지우고 로그인 화면으로 이동
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status;

    if (status === 401 || status === 403) {
      removeAccessToken();
      window.location.href = "/";
    }

    return Promise.reject(error);
  }
);

// 서버 ApiResponse의 message를 꺼내고, 없으면 fallback 사용
export const getErrorMessage = (error: unknown, fallback: string) => {
  if (axios.isAxiosError(error)) {
    return error.response?.data?.message ?? fallback;
  }

  return fallback;
};

export default api; 