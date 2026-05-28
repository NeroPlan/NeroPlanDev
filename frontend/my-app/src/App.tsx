// import { useState } from 'react'
import './App.css'
import { FcGoogle } from "react-icons/fc";
import { SiNaver, SiKakaotalk } from "react-icons/si";

function Header() {
    return (
        <header className="w-full h-8 bg-white flex items-center justify-center px-6">
            <h1 className="text-2xl font-bold">NeroPlan</h1>
        </header>
    )
}

function App() {
    const handleGoogleLogin = () => {
        window.location.href = "http://localhost:4000/api/v1/auth/login/google"
    }
    const handleCacaoLogin = () => {
        window.location.href = "http://localhost:4000/api/v1/auth/login/cacao"
    }
    const handleNaverLogin = () => {
        window.location.href = "http://localhost:4000/api/v1/auth/login/naver"
    }

    return (
        <div className="flex flex-col items-center justify-start h-screen pt-80">
            <Header />
            <main className="flex-1 flex items-start justify-center pt-5">
                <div className="flex min-h-screen items-center justify-center">
                    <div className="flex flex-col gap-12 w-64">
                        <button
                            onClick={handleGoogleLogin}
                            className="flex items-center justify-center gap-3 rounded-2xl border border-gray-300 bg-white py-3 px-5 text-xl font-semibold shadow-md transition hover:-translate-y-1 hover:shadow-xl"
                        >
                            <FcGoogle size={32} />
                            <span>Google 로그인</span>
                        </button>

                        <button
                            onClick={handleCacaoLogin}
                            className="flex items-center justify-center gap-3 rounded-2xl bg-[#FEE500] py-3 px-5 text-xl font-semibold text-black shadow-md transition hover:-translate-y-1 hover:shadow-xl"
                        >
                            <SiKakaotalk size={30} />
                            <span>카카오 로그인</span>
                        </button>

                        <button
                            onClick={handleNaverLogin}
                           className="flex items-center justify-center gap-3 rounded-2xl bg-[#03C75A] py-3 px-5 text-xl font-semibold text-white shadow-md transition hover:-translate-y-1 hover:shadow-xl"
                        >
                            <SiNaver size={28} />
                            <span>네이버 로그인</span>
                        </button>
                    </div> 
                </div>
            </main>
        </div>
    )
}

export default App