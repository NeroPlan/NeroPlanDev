import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { getErrorMessage } from "../api/api";
import { getMyProfile, toProfileImageUrl } from "../api/user";
import type { UserProfile } from "../api/user";
import MyPageMenu from "../components/common/MyPageMenu";
import ProfileCard from "../components/common/ProfileCard";

export default function MyPage() {
  const navigate = useNavigate();

  const [user, setUser] = useState<UserProfile | null>(null);
  const [errorMessage, setErrorMessage] = useState("");

  useEffect(() => {
    getMyProfile()
      .then(setUser)
      .catch((error) => {
        console.error("내 정보를 불러오지 못했습니다:", error);
        setErrorMessage(getErrorMessage(error, "내 정보를 불러오지 못했습니다."));
      });
  }, []);

const handleLogout = () => {
  const confirmed = window.confirm("로그아웃 하시겠습니까?");

  if (!confirmed) return;

  // 저장된 로그인 토큰 삭제
  localStorage.removeItem("accessToken");

  // 로그인 페이지로 이동
  navigate("/");
};

  const handleWithdraw = () => {
    const confirmed = window.confirm(
      "정말 회원 탈퇴하시겠어요?\n삭제된 정보는 복구할 수 없습니다.",
    );

    if (!confirmed) return;

    // 회원 탈퇴 API 호출 위치
    // 성공 후 navigate("/login");
  };

  return (
    <div className="mx-auto w-full max-w-[468px] space-y-6 pb-10">
      <h1 className="pt-2 text-center text-2xl font-bold text-slate-900">
        마이페이지
      </h1>

      {user ? (
        <ProfileCard name={user.nickname} imageUrl={toProfileImageUrl(user.profileImage)} />
      ) : (
        <p className="text-center text-sm text-slate-500">
          {errorMessage || "내 정보를 불러오는 중..."}
        </p>
      )}

      <MyPageMenu
        items={[
          {
            label: "내 정보 수정하기",
            onClick: () => navigate("/mypage/edit"),
          },
          {
            label: "로그아웃",
            onClick: handleLogout,
          },
          {
            label: "회원 탈퇴",
            onClick: handleWithdraw,
            danger: true,
          },
        ]}
      />
    </div>
  );
}