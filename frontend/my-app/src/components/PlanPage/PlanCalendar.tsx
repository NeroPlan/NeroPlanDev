// components/PlanPage/PlanCalendar.tsx
// 주간/월간 달력 - 날짜를 고르면 아래 계획 카드가 그 날짜로 바뀜
import type { Plan } from "../../api/plan";
import { WEEKDAYS, isSameDay, toDateKey } from "./calendarDate";
import type { CalendarView } from "./calendarDate";

type PlanState = "done" | "postponed" | "todo";

// 상태별 색 - 파랑 한 톤에서 명도로만 구분 (진한 파랑은 쓰지 않음)
// 선택·오늘 표시는 하단 메뉴와 같은 slate-900
const STATE_STYLES: Record<PlanState, { dot: string; chip: string; label: string }> = {
    done: { dot: "bg-blue-500", chip: "bg-blue-500 text-white", label: "완료" },
    postponed: { dot: "bg-blue-300", chip: "bg-blue-200 text-blue-900", label: "미룬 계획" },
    todo: { dot: "bg-blue-100", chip: "bg-blue-50 text-blue-900", label: "할 일" },
};

// 미룬 계획은 옮겨 간 날짜에 표시됨 - 아직 안 했고 한 번 이상 미뤘으면 미룬 계획
const getPlanState = (plan: Plan): PlanState => {
    if (plan.status === "DONE") return "done";
    return plan.postponeCount > 0 ? "postponed" : "todo";
};

interface PlanCalendarProps {
    view: CalendarView;
    cursor: Date; // 보고 있는 주/월의 기준 날짜
    days: Date[];
    selected: Date;
    today: Date;
    plansByDate: Record<string, Plan[]>;
    loading: boolean;
    onViewChange: (view: CalendarView) => void;
    onMove: (dir: -1 | 1) => void;
    onSelect: (date: Date) => void;
    onToday: () => void;
}

export default function PlanCalendar({
    view,
    cursor,
    days,
    selected,
    today,
    plansByDate,
    loading,
    onViewChange,
    onMove,
    onSelect,
    onToday,
}: PlanCalendarProps) {
    const first = days[0];
    const last = days[days.length - 1];
    const title =
        view === "week"
            ? `${first.getMonth() + 1}월 ${first.getDate()}일 – ${last.getMonth() + 1}월 ${last.getDate()}일`
            : `${cursor.getFullYear()}년 ${cursor.getMonth() + 1}월`;

    return (
        <div className="bg-white rounded-3xl p-5 shadow-sm">
            <div className="flex items-center justify-between mb-4">
                <button
                    onClick={() => onMove(-1)}
                    className="h-9 w-9 rounded-full text-xl text-gray-500 hover:bg-gray-100"
                    aria-label={view === "week" ? "이전 주" : "이전 달"}
                >
                    ‹
                </button>
                <div className="text-center">
                    <p className="font-bold">{title}</p>
                    {!isSameDay(selected, today) && (
                        <button onClick={onToday} className="text-xs text-gray-500 font-semibold underline">
                            오늘로 이동
                        </button>
                    )}
                </div>
                <button
                    onClick={() => onMove(1)}
                    className="h-9 w-9 rounded-full text-xl text-gray-500 hover:bg-gray-100"
                    aria-label={view === "week" ? "다음 주" : "다음 달"}
                >
                    ›
                </button>
            </div>

            {/* 주간/월간 전환 */}
            <div className="mb-4 grid grid-cols-2 rounded-xl bg-gray-100 p-1 text-sm font-semibold">
                {(["week", "month"] as const).map((v) => (
                    <button
                        key={v}
                        onClick={() => onViewChange(v)}
                        className={`rounded-lg py-1.5 transition ${view === v ? "bg-white shadow-sm text-gray-900" : "text-gray-400"}`}
                    >
                        {v === "week" ? "주간" : "월간"}
                    </button>
                ))}
            </div>

            <div className="grid grid-cols-7 text-center text-xs text-gray-400 mb-1">
                {WEEKDAYS.map((w) => (
                    <span key={w}>{w}</span>
                ))}
            </div>

            <div className={`grid grid-cols-7 ${view === "week" ? "gap-1" : "gap-y-1"} ${loading ? "opacity-60" : ""}`}>
                {days.map((d) => {
                    const key = toDateKey(d);
                    const cellProps = {
                        date: d,
                        plans: plansByDate[key] ?? [],
                        selected: isSameDay(d, selected),
                        today: isSameDay(d, today),
                        onClick: () => onSelect(d),
                    };
                    return view === "week" ? (
                        <WeekCell key={key} {...cellProps} />
                    ) : (
                        <MonthCell key={key} {...cellProps} outside={d.getMonth() !== cursor.getMonth()} />
                    );
                })}
            </div>

            {/* 범례 */}
            <div className="mt-4 flex justify-center gap-4 text-xs text-gray-500">
                {(["done", "postponed", "todo"] as const).map((s) => (
                    <span key={s} className="flex items-center gap-1">
                        <span className={`h-2 w-2 rounded-full ${STATE_STYLES[s].dot}`} />
                        {STATE_STYLES[s].label}
                    </span>
                ))}
            </div>
        </div>
    );
}

interface CellProps {
    date: Date;
    plans: Plan[];
    selected: boolean;
    today: boolean;
    onClick: () => void;
}

// 주간 칸 - 날짜 + 상태 점(최대 4개) + 완료 수
function WeekCell({ date, plans, selected, today, onClick }: CellProps) {
    const done = plans.filter((p) => p.status === "DONE").length;
    return (
        <button
            onClick={onClick}
            className={`flex flex-col items-center gap-1.5 rounded-2xl py-2.5 transition ${
                selected ? "bg-slate-900 text-white" : "hover:bg-gray-50"
            }`}
        >
            <span className={`text-lg font-bold leading-none ${today && !selected ? "text-blue-500" : ""}`}>
                {date.getDate()}
            </span>
            <div className="flex h-1.5 gap-0.5">
                {plans.slice(0, 4).map((p) => (
                    <span
                        key={p.planId}
                        className={`h-1.5 w-1.5 rounded-full ${selected ? "bg-white/80" : STATE_STYLES[getPlanState(p)].dot}`}
                    />
                ))}
            </div>
            <span className={`text-[10px] ${selected ? "text-slate-300" : "text-gray-400"}`}>
                {plans.length > 0 ? `${done}/${plans.length}` : "–"}
            </span>
        </button>
    );
}

// 월간 칸 - 계획 제목을 칩으로 최대 2개, 나머지는 +N
function MonthCell({ date, plans, selected, today, outside, onClick }: CellProps & { outside: boolean }) {
    return (
        <button
            onClick={onClick}
            className={`flex h-[72px] flex-col items-stretch gap-0.5 rounded-lg border px-0.5 pt-1 text-left transition ${
                selected ? "border-slate-900 bg-slate-50" : "border-transparent hover:bg-gray-50"
            } ${outside ? "opacity-35" : ""}`}
        >
            <span
                className={`mx-auto flex h-5 w-5 items-center justify-center rounded-full text-xs font-semibold ${
                    today ? "bg-slate-900 text-white" : ""
                }`}
            >
                {date.getDate()}
            </span>
            {plans.slice(0, 2).map((p) => (
                <span
                    key={p.planId}
                    className={`truncate rounded px-1 text-[9px] leading-[14px] ${STATE_STYLES[getPlanState(p)].chip}`}
                >
                    {p.content}
                </span>
            ))}
            {plans.length > 2 && <span className="px-1 text-[9px] text-gray-400">+{plans.length - 2}</span>}
        </button>
    );
}
