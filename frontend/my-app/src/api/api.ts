import axios from "axios";
import type { InternalAxiosRequestConfig } from "axios";
import { getAccessToken, removeAccessToken, saveAccessToken } from "./auth";

// 프론트와 API가 같은 도메인(proxy) 뒤에 있으므로 상대 경로 사용
const baseURL = import.meta.env.VITE_API_BASE_URL ?? "";

const api = axios.create({ baseURL });

// refresh token은 HttpOnly 쿠키라 자바스크립트로 읽을 수 없고, 이 요청에 브라우저가 자동으로 실어 보냄
// 동시에 여러 요청이 401을 받아도 재발급은 한 번만 하도록 진행 중인 요청을 공유
let refreshPromise: Promise<string> | null = null;

export const refreshAccessToken = () => {
  if (!refreshPromise) {
    refreshPromise = axios
      .post(`${baseURL}/api/v1/auth/refresh`, null, { withCredentials: true })
      .then((response) => {
        const token: string = response.data.result.accessToken;
        saveAccessToken(token);
        return token;
      })
      .finally(() => {
        refreshPromise = null;
      });
  }
  return refreshPromise;
};

const goToLogin = () => {
  removeAccessToken();
  if (window.location.pathname !== "/") {
    window.location.href = "/";
  }
};

api.interceptors.request.use(async (config) => {
  let token = getAccessToken();

  // 로그인 직후·새로고침 후에는 메모리에 토큰이 없으므로 먼저 발급받음 (실패하면 401 처리에 맡김)
  if (!token) {
    token = await refreshAccessToken().catch(() => null);
  }

  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }

  return config;
});

// 401(토큰 없음·만료) - 한 번만 재발급받아 다시 요청하고, 그래도 실패하면 로그인 화면으로 이동
// 403은 권한 문제이므로 로그아웃시키지 않음
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const status = error.response?.status;
    const original = error.config as (InternalAxiosRequestConfig & { _retried?: boolean }) | undefined;

    if (status === 401 && original && !original._retried) {
      original._retried = true;
      try {
        const token = await refreshAccessToken();
        original.headers.Authorization = `Bearer ${token}`;
        return api(original);
      } catch {
        goToLogin();
      }
    } else if (status === 401) {
      goToLogin();
    }

    return Promise.reject(error);
  }
);

// refresh token 쿠키 삭제 - 실패해도 화면은 로그인으로 이동
export const logout = async () => {
  try {
    await axios.post(`${baseURL}/api/v1/auth/logout`, null, { withCredentials: true });
  } finally {
    removeAccessToken();
  }
};

// 서버 ApiResponse의 message를 꺼내고, 없으면 fallback 사용
export const getErrorMessage = (error: unknown, fallback: string) => {
  if (axios.isAxiosError(error)) {
    return error.response?.data?.message ?? fallback;
  }

  return fallback;
};

export default api;
