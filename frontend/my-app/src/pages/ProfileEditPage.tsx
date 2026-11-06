// pages/ProfileEditPage.tsx
import { useEffect, useRef, useState } from "react";
import type { ChangeEvent } from "react";
import { useNavigate } from "react-router-dom";
import { getErrorMessage } from "../api/api";
import {
  MAX_NICKNAME_LENGTH,
  getMyProfile,
  toProfileImageUrl,
  updateNickname,
  uploadProfileImage,
} from "../api/user";
import type { UserProfile } from "../api/user";

// 올리기 전에 정사각형으로 자르고 이 크기로 줄임 - 서버 제한(2MB)보다 훨씬 작아짐
const PROFILE_IMAGE_SIZE = 512;

const resizeToSquare = async (file: File): Promise<Blob> => {
  const bitmap = await createImageBitmap(file);
  const side = Math.min(bitmap.width, bitmap.height);
  const canvas = document.createElement("canvas");
  canvas.width = PROFILE_IMAGE_SIZE;
  canvas.height = PROFILE_IMAGE_SIZE;

  const ctx = canvas.getContext("2d");
  if (!ctx) throw new Error("canvas를 사용할 수 없습니다.");
  ctx.drawImage(
    bitmap,
    (bitmap.width - side) / 2,
    (bitmap.height - side) / 2,
    side,
    side,
    0,
    0,
    PROFILE_IMAGE_SIZE,
    PROFILE_IMAGE_SIZE,
  );
  bitmap.close();

  return new Promise((resolve, reject) => {
    canvas.toBlob(
      (blob) => (blob ? resolve(blob) : reject(new Error("이미지 변환에 실패했습니다."))),
      "image/jpeg",
      0.85,
    );
  });
};

export default function ProfileEditPage() {
  const navigate = useNavigate();
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [user, setUser] = useState<UserProfile | null>(null);
  const [nickname, setNickname] = useState("");
  // 새로 고른 사진과 미리보기 주소
  const [newImage, setNewImage] = useState<{ blob: Blob; previewUrl: string } | null>(null);
  const [saving, setSaving] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");

  useEffect(() => {
    getMyProfile()
      .then((profile) => {
        setUser(profile);
        setNickname(profile.nickname);
      })
      .catch((error) => {
        console.error("내 정보를 불러오지 못했습니다:", error);
        setErrorMessage(getErrorMessage(error, "내 정보를 불러오지 못했습니다."));
      });
  }, []);

  // 미리보기 주소는 사진을 바꾸거나 페이지를 나갈 때 해제
  useEffect(() => {
    if (!newImage) return;
    return () => URL.revokeObjectURL(newImage.previewUrl);
  }, [newImage]);

  const handleSelectImage = async (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    event.target.value = ""; // 같은 파일을 다시 골라도 onChange가 불리도록
    if (!file) return;

    if (!file.type.startsWith("image/")) {
      setErrorMessage("이미지 파일만 올릴 수 있어요.");
      return;
    }

    try {
      const blob = await resizeToSquare(file);
      setNewImage({ blob, previewUrl: URL.createObjectURL(blob) });
      setErrorMessage("");
    } catch (error) {
      console.error("이미지를 처리하지 못했습니다:", error);
      setErrorMessage("이미지를 읽지 못했어요. 다른 사진을 골라 주세요.");
    }
  };

  const handleSave = async () => {
    if (!user) return;

    const trimmed = nickname.trim();
    if (!trimmed) {
      setErrorMessage("이름을 입력하세요.");
      return;
    }

    setSaving(true);
    try {
      if (newImage) await uploadProfileImage(newImage.blob);
      if (trimmed !== user.nickname) await updateNickname(trimmed);
      navigate("/mypage");
    } catch (error) {
      console.error("내 정보를 저장하지 못했습니다:", error);
      setErrorMessage(getErrorMessage(error, "내 정보를 저장하지 못했습니다."));
    } finally {
      setSaving(false);
    }
  };

  if (!user) {
    return (
      <p className="text-center text-sm text-slate-500">
        {errorMessage || "내 정보를 불러오는 중..."}
      </p>
    );
  }

  const imageUrl = newImage?.previewUrl || toProfileImageUrl(user.profileImage);
  const initial = nickname.trim().charAt(0) || "U";
  const changed = newImage !== null || nickname.trim() !== user.nickname;

  return (
    <div className="mx-auto w-full max-w-[468px] space-y-6 pb-10">
      <h1 className="pt-2 text-center text-2xl font-bold text-slate-900">
        내 정보 수정하기
      </h1>

      <section className="space-y-6 rounded-[28px] bg-white px-6 py-8 shadow-sm">
        <div className="flex flex-col items-center gap-3">
          <button
            type="button"
            onClick={() => fileInputRef.current?.click()}
            className="group relative h-24 w-24 overflow-hidden rounded-full"
            aria-label="프로필 사진 바꾸기"
          >
            {imageUrl ? (
              <img
                src={imageUrl}
                alt="프로필 미리보기"
                referrerPolicy="no-referrer"
                className="h-full w-full object-cover"
              />
            ) : (
              <span className="flex h-full w-full items-center justify-center bg-slate-900 text-3xl font-bold text-white">
                {initial}
              </span>
            )}
            <span className="absolute inset-0 flex items-center justify-center bg-black/40 text-xs font-semibold text-white opacity-0 transition group-hover:opacity-100">
              변경
            </span>
          </button>
          <button
            type="button"
            onClick={() => fileInputRef.current?.click()}
            className="text-sm font-semibold text-slate-600 hover:text-slate-900"
          >
            사진 바꾸기
          </button>
          <input
            ref={fileInputRef}
            type="file"
            accept="image/jpeg,image/png,image/gif,image/webp"
            onChange={handleSelectImage}
            className="hidden"
          />
        </div>

        <label className="block space-y-2">
          <span className="text-sm font-semibold text-slate-700">이름</span>
          <input
            type="text"
            value={nickname}
            maxLength={MAX_NICKNAME_LENGTH}
            onChange={(e) => setNickname(e.target.value)}
            className="w-full rounded-2xl border border-slate-200 px-4 py-3 text-base outline-none focus:border-slate-900"
          />
        </label>

        {errorMessage && <p className="text-sm text-red-500">{errorMessage}</p>}

        <div className="flex gap-3">
          <button
            type="button"
            onClick={() => navigate("/mypage")}
            className="flex-1 rounded-2xl bg-slate-100 py-3 font-bold text-slate-700 transition hover:bg-slate-200"
          >
            취소
          </button>
          <button
            type="button"
            onClick={handleSave}
            disabled={!changed || saving}
            className="flex-1 rounded-2xl bg-slate-900 py-3 font-bold text-white transition hover:bg-slate-700 disabled:opacity-40"
          >
            {saving ? "저장 중..." : "저장"}
          </button>
        </div>
      </section>
    </div>
  );
}
