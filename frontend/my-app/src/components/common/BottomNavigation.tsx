// components/common/BottomNavigation.tsx
// 좁은 화면(1024px 미만) 하단 메뉴 - 넓은 화면에서는 SideNavigation이 대신함
import { NavLink } from "react-router-dom";
import { NAV_MENUS, navLinkClass } from "./navMenus";

export default function BottomNavigation() {
  return (
    <nav className="shrink-0 border-t border-slate-200 bg-white lg:hidden">
      <div className="mx-auto flex h-[4.25rem] w-full max-w-[26rem] items-center justify-around px-4">
        {NAV_MENUS.map((menu) => (
          <NavLink
            key={menu.path}
            to={menu.path}
            className={({ isActive }) =>
              `flex min-w-[4rem] flex-col items-center gap-1 px-3 py-2 text-xs ${navLinkClass(isActive)}`
            }
          >
            <span className="text-base leading-none">{menu.icon}</span>
            <span>{menu.label}</span>
          </NavLink>
        ))}
      </div>
    </nav>
  );
}
