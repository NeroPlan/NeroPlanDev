// layouts/MainLayout.tsx

import { Outlet } from "react-router-dom";
import Header from "../components/common/Header";
import BottomNavigation from "../components/common/BottomNavigation";

export default function MainLayout() {
    return (
        // 화면 높이에 맞춘 세로 배치 - 헤더와 하단 메뉴는 고정, 그 사이 내용만 스크롤
        <div className="flex h-dvh flex-col bg-gray-100">
            <Header />

            <main className="flex-1 overflow-y-auto">
                <div className="max-w-md mx-auto p-5">
                    <Outlet />
                </div>
            </main>
            <BottomNavigation />
        </div>
    );
}