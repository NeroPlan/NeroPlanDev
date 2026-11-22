// pages/LoginPage.tsx
// 로고 + 한 줄 소개 + 로그인 버튼만 가운데에

import { FcGoogle } from "react-icons/fc";
import { SiNaver, SiKakaotalk } from "react-icons/si";
import Logo from "../components/common/Logo";

export default function LoginPage() {
    const handleGoogleLogin = () => {
        window.location.href =
            "/api/v1/auth/login/google";
    };

    const handleCacaoLogin = () => {
        window.location.href =
            "/api/v1/auth/login/cacao";
    };

    const handleNaverLogin = () => {
        window.location.href =
            "/api/v1/auth/login/naver";
    };

    return (
        <div className="flex min-h-dvh items-center justify-center bg-gray-100 px-5 py-12">
            <div className="w-full max-w-sm text-center">
                <Logo size={48} />
                <p className="mt-5 whitespace-nowrap text-slate-500">하루 계획을 나에게 꼭 맞게 다듬는 AI 플래너</p>

                <div className="mt-10 flex flex-col gap-3 rounded-3xl bg-white p-6 shadow-sm">
                    <button
                        onClick={handleGoogleLogin}
                        className="flex items-center justify-center gap-3 rounded-2xl border border-gray-200 bg-white px-5 py-3.5 font-semibold text-slate-900 transition hover:bg-gray-50"
                    >
                        <FcGoogle size={24} />
                        <span>Google로 계속하기</span>
                    </button>

                    <button
                        onClick={handleCacaoLogin}
                        className="flex items-center justify-center gap-3 rounded-2xl bg-[#FEE500] px-5 py-3.5 font-semibold text-black transition hover:brightness-95"
                    >
                        <SiKakaotalk size={22} />
                        <span>카카오로 계속하기</span>
                    </button>

                    <button
                        onClick={handleNaverLogin}
                        className="flex items-center justify-center gap-3 rounded-2xl bg-[#03C75A] px-5 py-3.5 font-semibold text-white transition hover:brightness-95"
                    >
                        <SiNaver size={18} />
                        <span>네이버로 계속하기</span>
                    </button>
                </div>
            </div>
        </div>
    );
}
