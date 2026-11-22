// components/common/SideNavigation.tsx
// 넓은 화면(1024px 이상) 왼쪽 메뉴 - 좁은 화면에서는 BottomNavigation이 대신함
import { NavLink } from "react-router-dom";
import { NAV_MENUS, navLinkClass } from "./navMenus";

export default function SideNavigation() {
    return (
        <aside className="hidden w-60 shrink-0 flex-col border-r border-slate-200 bg-white px-4 py-8 lg:flex">
            <p className="px-4 text-xs font-bold uppercase tracking-widest text-slate-300">메뉴</p>
            <nav className="mt-3 flex flex-col gap-1">
                {NAV_MENUS.map((menu) => (
                    <NavLink
                        key={menu.path}
                        to={menu.path}
                        className={({ isActive }) => `flex items-center gap-3 px-4 py-3 text-sm ${navLinkClass(isActive)}`}
                    >
                        <span className="w-4 text-center text-base leading-none">{menu.icon}</span>
                        {menu.label}
                    </NavLink>
                ))}
            </nav>

            {/* 서비스 한 줄 소개 */}
            <div className="mt-auto mx-4 rounded-2xl bg-gray-100 p-4">
                <p className="text-xs font-bold text-slate-900">계획은 세우는 것보다 지키는 것</p>
                <p className="mt-1 text-xs leading-relaxed text-slate-500">
                    AI가 내 기록을 보고 지킬 수 있는 순서와 시간을 알려 줘요.
                </p>
            </div>
        </aside>
    );
}
