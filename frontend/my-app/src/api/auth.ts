// access token은 메모리에만 보관 - localStorage는 XSS 스크립트가 그대로 읽어 갈 수 있음
// 새로고침하면 사라지므로 api.ts가 refresh token 쿠키(HttpOnly)로 다시 발급받음
let accessToken: string | null = null;

export const saveAccessToken = (token: string) => {
  accessToken = token;
};

export const getAccessToken = () => {
  return accessToken;
};

export const removeAccessToken = () => {
  accessToken = null;
  // 이전 버전이 localStorage에 남긴 토큰 정리
  localStorage.removeItem("accessToken");
};
