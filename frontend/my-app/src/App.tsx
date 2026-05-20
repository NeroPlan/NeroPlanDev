// import { useState } from 'react'
import './App.css'

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
            <main className="flex-1 flex items-start justify-center pt-20">
            <div className="flex flex-col gap-12 w-64">
                <button
                    onClick={handleGoogleLogin}
                    className="px-10 py-4 bg-white border border-black text-black rounded"
                >
                    구글 계정으로 로그인
                </button>

                <button
                    onClick={handleCacaoLogin}
                    className="px-10 py-4 bg-[#FEE500] border border-black text-black rounded"
                >
                    카카오 계정으로 로그인
                </button>

                <button
                    onClick={handleNaverLogin}
                    className="px-10 py-4 bg-[#03C75A] border border-black text-black rounded"
                >
                    네이버 계정으로 로그인
                </button>
            </div>
            </main>
        </div>
    )
}

export default App