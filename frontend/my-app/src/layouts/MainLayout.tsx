// layouts/MainLayout.tsx

import { useMemo } from "react";
import { Outlet, useLocation } from "react-router-dom";
import Logo from "../components/common/Logo";
import SideNavigation from "../components/common/SideNavigation";
import BottomNavigation from "../components/common/BottomNavigation";
import { NAV_MENUS } from "../components/common/navMenus";

const WEEKDAYS = ["일", "월", "화", "수", "목", "금", "토"];

// 넓은 화면(1024px 이상): 위쪽 로고 바 + 왼쪽 메뉴 + 페이지 제목 영역 + 넓은 본문(최대 72rem), 페이지 안은 2칸 배치
// 좁은 화면: 위쪽 로고 + 한 줄 본문 + 하단 메뉴
export default function MainLayout() {
    const { pathname } = useLocation();
    // 메뉴 페이지에서만 제목 영역 표시 - 정보 수정·카테고리 관리 같은 하위 페이지는 자체 제목 사용
    const current = NAV_MENUS.find((m) => m.path === pathname);
    const todayText = useMemo(() => {
        const d = new Date();
        return `${d.getMonth() + 1}월 ${d.getDate()}일 ${WEEKDAYS[d.getDay()]}요일`;
    }, []);

    return (
        // 화면 높이에 맞춘 배치 - 로고 바와 메뉴는 고정, 본문만 스크롤
        <div className="flex h-dvh flex-col bg-gray-100">
            <header className="flex shrink-0 items-center justify-center bg-white py-5 lg:h-20 lg:justify-between lg:border-b lg:border-slate-200 lg:px-8 lg:py-0">
                <Logo size={30} />
                <span className="hidden text-sm font-semibold text-slate-400 lg:block">{todayText}</span>
            </header>

            <div className="flex min-h-0 flex-1">
                <SideNavigation />

                <main className="flex-1 overflow-y-auto">
                    <div className="mx-auto max-w-md p-5 lg:max-w-6xl lg:px-12 lg:pb-16 lg:pt-12">
                        {current && (
                            <div className="mb-10 hidden lg:block">
                                <h1 className="text-3xl font-extrabold tracking-tight text-black">{current.title}</h1>
                                <p className="mt-2 text-sm text-slate-500">{current.description}</p>
                            </div>
                        )}
                        <Outlet />
                    </div>
                </main>
            </div>

            <BottomNavigation />
        </div>
    );
}
