// components/PlanPage/calendarDate.ts
// 달력 날짜 계산 - 서버와 주고받는 날짜는 모두 YYYY-MM-DD 문자열

export type CalendarView = "week" | "month";

export const WEEKDAYS = ["일", "월", "화", "수", "목", "금", "토"];

// 로컬 시간 기준 YYYY-MM-DD (toISOString은 UTC라 새벽에 날짜가 하루 밀림)
export const toDateKey = (d: Date) =>
    `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;

export const addDays = (d: Date, n: number) => {
    const next = new Date(d);
    next.setDate(next.getDate() + n);
    return next;
};

export const isSameDay = (a: Date, b: Date) => toDateKey(a) === toDateKey(b);

// 달력에 보이는 날짜 - 주간은 일~토 7일, 월간은 그 달을 감싸는 6주(42칸)
export const getVisibleDays = (view: CalendarView, cursor: Date) => {
    if (view === "week") {
        const start = addDays(cursor, -cursor.getDay());
        return Array.from({ length: 7 }, (_, i) => addDays(start, i));
    }
    const first = new Date(cursor.getFullYear(), cursor.getMonth(), 1);
    const start = addDays(first, -first.getDay());
    return Array.from({ length: 42 }, (_, i) => addDays(start, i));
};

export const formatDayTitle = (d: Date) =>
    `${d.getMonth() + 1}월 ${d.getDate()}일 (${WEEKDAYS[d.getDay()]})`;
