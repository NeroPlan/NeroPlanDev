import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { getErrorMessage, logout } from "../api/api";
import { getMyProfile, toProfileImageUrl, withdraw } from "../api/user";
import type { UserProfile } from "../api/user";
import { deleteMyPlanData, getPlanSummary } from "../api/plan";
import type { PlanSummary } from "../api/plan";
import { removeAccessToken } from "../api/auth";
import MyPageMenu from "../components/common/MyPageMenu";
import ProfileCard from "../components/common/ProfileCard";
import SummaryCard from "../components/common/SummaryCard";

export default function MyPage() {
  const navigate = useNavigate();

  const [user, setUser] = useState<UserProfile | null>(null);
  const [errorMessage, setErrorMessage] = useState("");
  const [summary, setSummary] = useState<PlanSummary | null>(null);
  const [summaryError, setSummaryError] = useState("");
  const [withdrawing, setWithdrawing] = useState(false);
  const [withdrawError, setWithdrawError] = useState("");

  useEffect(() => {
    getMyProfile()
      .then(setUser)
      .catch((error) => {
        console.error("내 정보를 불러오지 못했습니다:", error);
        setErrorMessage(getErrorMessage(error, "내 정보를 불러오지 못했습니다."));
      });
    getPlanSummary()
      .then(setSummary)
      .catch((error) => {
        console.error("요약을 불러오지 못했습니다:", error);
        setSummaryError(getErrorMessage(error, "요약을 불러오지 못했습니다."));
      });
  }, []);

const handleLogout = async () => {
  const confirmed = window.confirm("로그아웃 하시겠습니까?");

  if (!confirmed) return;

  // 서버에 refresh token 쿠키 삭제를 요청하고 메모리의 토큰도 삭제
  await logout().catch((error) => console.error("로그아웃 요청 실패:", error));

  // 로그인 페이지로 이동
  navigate("/");
};

  const handleWithdraw = async () => {
    if (withdrawing) return;
    const confirmed = window.confirm(
      "정말 회원 탈퇴하시겠어요?\n계획, 분석 기록, 카테고리가 모두 삭제되고 복구할 수 없습니다.",
    );

    if (!confirmed) return;

    setWithdrawing(true);
    setWithdrawError("");
    try {
      // 계획 데이터를 먼저 지움 - 계정을 먼저 지우면 이후 요청에 쓸 토큰을 재발급받지 못함
      await deleteMyPlanData();
      await withdraw();
      removeAccessToken();
      navigate("/");
    } catch (error) {
      console.error("회원 탈퇴 실패:", error);
      setWithdrawError(getErrorMessage(error, "회원 탈퇴에 실패했습니다. 다시 시도해 주세요."));
      setWithdrawing(false);
    }
  };

  return (
    <div className="mx-auto w-full max-w-[26rem] space-y-6 pb-10 lg:max-w-4xl">
      <h1 className="pt-2 text-center text-2xl font-bold text-slate-900 lg:hidden">
        마이페이지
      </h1>

      <div className="space-y-6 lg:grid lg:grid-cols-2 lg:items-start lg:gap-8 lg:space-y-0">
      {/* 왼쪽: 프로필과 요약 */}
      <div className="space-y-6">

      {user ? (
        <ProfileCard name={user.nickname} imageUrl={toProfileImageUrl(user.profileImage)} />
      ) : (
        <p className="text-center text-sm text-slate-500">
          {errorMessage || "내 정보를 불러오는 중..."}
        </p>
      )}

      {user && <SummaryCard summary={summary} errorMessage={summaryError} />}
      </div>

      {/* 오른쪽: 메뉴 */}
      <div className="space-y-6">
      <MyPageMenu
        items={[
          {
            label: "내 정보 수정하기",
            onClick: () => navigate("/mypage/edit"),
          },
          {
            label: "카테고리 관리",
            onClick: () => navigate("/mypage/categories"),
          },
          {
            label: "로그아웃",
            onClick: handleLogout,
          },
          {
            label: withdrawing ? "탈퇴 처리 중..." : "회원 탈퇴",
            onClick: handleWithdraw,
            danger: true,
          },
        ]}
      />

      {withdrawError && (
        <p className="text-center text-sm text-red-500">{withdrawError}</p>
      )}
      </div>
      </div>
    </div>
  );
}