// import { useState } from 'react'
import './App.css'

function Header() {
    return <h1 className="text-2xl font-bold mb-10"></h1>
}

function App() {
    const handleLogin = () => {
        window.location.href = "http://localhost:4000/api/v1/auth/login/google"
    }

    return (
        <div className="flex flex-col items-center justify-start h-screen pt-40">
            <Header />
            <div className="flex flex-col gap-64 w-64">
                <button
                    onClick={handleLogin}
                    className="px-10 py-6 bg-white border border-black text-black rounded"
                >
                    구글 로그인
                </button>

                <button
                    onClick={handleLogin}
                    className="px-10 py-6 bg-white border border-black text-black rounded"
                >
                    카카오 로그인
                </button>

                <button
                    onClick={handleLogin}
                    className="px-10 py-6 bg-white border border-black text-black rounded"
                >
                    네이버 로그인
                </button>
            </div>
        </div>
    )
}

export default App