// api/user.ts
import api from "./api";

export interface UserProfile {
    userId: number;
    nickname: string;
    profileImage: string | null; // 구글 사진 주소 또는 /api/v1/users/{id}/profile-image
    birth: string | null;
    gender: string | null;
}

export const MAX_NICKNAME_LENGTH = 30; // backendUser users.nickname 컬럼 길이

export const getMyProfile = async (): Promise<UserProfile> => {
    const response = await api.get("/api/v1/users/me");
    return response.data.result;
};

export const updateNickname = async (nickname: string): Promise<UserProfile> => {
    const response = await api.patch("/api/v1/users/me", { nickname });
    return response.data.result;
};

export const uploadProfileImage = async (image: Blob): Promise<UserProfile> => {
    const formData = new FormData();
    formData.append("image", image, "profile.jpg");
    const response = await api.put("/api/v1/users/me/profile-image", formData);
    return response.data.result;
};

// 직접 올린 사진은 /api/... 상대 주소로 오므로 API 서버 주소를 붙임
export const toProfileImageUrl = (profileImage: string | null) => {
    if (!profileImage) return "";
    if (profileImage.startsWith("/")) {
        return `${import.meta.env.VITE_API_BASE_URL ?? ""}${profileImage}`;
    }
    return profileImage;
};
