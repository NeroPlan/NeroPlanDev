import { useNavigate } from "react-router-dom";
import MyPageMenu from "../components/common/MyPageMenu";
import ProfileCard from "../components/common/ProfileCard";

export default function MyPage() {
  const navigate = useNavigate();

  // 나중에 로그인 사용자 API 데이터로 교체
  const user = {
    name: "홍길동",
    imageUrl: "",
  };

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

      <ProfileCard name={user.name} imageUrl={user.imageUrl} />

      <MyPageMenu
        items={[
          {
            label: "내 정보 수정하기",
            onClick: () => alert("내 정보 수정 기능을 준비 중입니다."),
            // onClick: () => navigate("/mypage/edit"),
          },
          {
            label: "초기 정보 수정하기", 
            onClick: () => navigate("/onboarding"),
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