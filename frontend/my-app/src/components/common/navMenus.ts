// components/common/navMenus.ts
// 메인 메뉴 - 왼쪽 사이드바(넓은 화면), 하단 메뉴(좁은 화면), 페이지 제목 영역이 함께 사용
export const NAV_MENUS = [
    {
        label: "계획",
        path: "/plan",
        icon: "✓",
        title: "계획",
        description: "날짜를 골라 계획을 세우고, 오늘 할 일은 AI가 수행 순서를 추천해요.",
    },
    {
        label: "집중",
        path: "/focus",
        icon: "◔",
        title: "집중",
        description: "예상한 시간 안에 끝낼 수 있는지 직접 재 보세요.",
    },
    {
        label: "분석",
        path: "/analysis",
        icon: "▤",
        title: "분석",
        description: "쌓인 기록으로 내 계획 습관을 한눈에 봐요.",
    },
    {
        label: "마이",
        path: "/mypage",
        icon: "◉",
        title: "마이페이지",
        description: "내 정보와 지금까지의 기록을 관리해요.",
    },
];

export const navLinkClass = (isActive: boolean) =>
    `rounded-xl font-semibold transition ${
        isActive ? "bg-slate-900 text-white" : "text-slate-400 hover:bg-slate-100 hover:text-slate-700"
    }`;
