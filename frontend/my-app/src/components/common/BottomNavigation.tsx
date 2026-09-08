import { NavLink } from "react-router-dom";

const menus = [
  { label: "계획", path: "/plan", icon: "✓" },
  { label: "마이", path: "/mypage", icon: "◉" },
];

export default function BottomNavigation() {
  return (
    <nav className="fixed bottom-0 left-0 right-0 z-50 border-t border-slate-200 bg-white">
      <div className="mx-auto flex h-[76px] w-full max-w-[468px] items-center justify-around px-4">
        {menus.map((menu) => (
          <NavLink
            key={menu.path}
            to={menu.path}
            className={({ isActive }) =>
              `flex min-w-[72px] flex-col items-center gap-1 rounded-xl px-3 py-2 text-xs font-semibold transition ${
                isActive
                  ? "bg-slate-900 text-white"
                  : "text-slate-400 hover:bg-slate-100 hover:text-slate-700"
              }`
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