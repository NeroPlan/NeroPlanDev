
export const saveAccessToken = (token: string) => {
  localStorage.setItem("accessToken", token);
};

export const getAccessToken = () => {
  return localStorage.getItem("accessToken");
};