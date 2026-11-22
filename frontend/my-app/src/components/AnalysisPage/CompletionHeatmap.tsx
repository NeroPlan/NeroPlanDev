// components/AnalysisPage/CompletionHeatmap.tsx
// 완료 잔디 - 최근 12주를 하루 한 칸으로, 그날 계획 중 완료한 비율만큼 진하게 표시 (기간 필터와 무관하게 항상 12주)
import { useEffect, useMemo, useState } from "react";
import { getErrorMessage } from "../../api/api";
import { getDailyStats } from "../../api/plan";
import type { DailyStat } from "../../api/plan";
import { WEEKDAYS, addDays, toDateKey } from "../PlanPage/calendarDate";
import StatCard from "./StatCard";

const WEEKS = 12;

// 파랑 한 톤에서 명도로만 구분 - 진한 파랑은 쓰지 않음 (계획 페이지 달력과 같은 기준)
const LEVELS = [
    { className: "bg-gray-100", label: "계획 없음" },
    { className: "bg-blue-50 ring-1 ring-inset ring-blue-100", label: "완료 0%" },
    { className: "bg-blue-200", label: "50% 미만" },
    { className: "bg-blue-300", label: "50% 이상" },
    { className: "bg-blue-500", label: "모두 완료" },
];

const getLevel = (stat: DailyStat | undefined) => {
    if (!stat || stat.totalCount === 0) return 0;
    const rate = stat.doneCount / stat.totalCount;
    if (rate === 0) return 1;
    if (rate < 0.5) return 2;
    if (rate < 1) return 3;
    return 4;
};

export default function CompletionHeatmap() {
    const today = useMemo(() => new Date(), []);
    // 이번 주 토요일까지 12주 - 열 하나가 일~토 한 주
    const start = useMemo(() => addDays(today, -today.getDay() - (WEEKS - 1) * 7), [today]);
    const days = useMemo(() => Array.from({ length: WEEKS * 7 }, (_, i) => addDays(start, i)), [start]);

    const [stats, setStats] = useState<Record<string, DailyStat> | null>(null);
    const [errorMessage, setErrorMessage] = useState("");

    useEffect(() => {
        getDailyStats(toDateKey(days[0]), toDateKey(days[days.length - 1]))
            .then((result) => setStats(Object.fromEntries(result.map((s) => [s.date, s]))))
            .catch((error) => {
                console.error("완료 기록을 불러오지 못했습니다:", error);
                setErrorMessage(getErrorMessage(error, "완료 기록을 불러오지 못했습니다."));
            });
    }, [days]);

    const todayKey = toDateKey(today);
    const pastStats = stats ? Object.values(stats).filter((s) => s.date <= todayKey) : [];
    const plannedDays = pastStats.length;
    const perfectDays = pastStats.filter((s) => s.doneCount === s.totalCount).length;
    const insight =
        plannedDays > 0
            ? `최근 ${WEEKS}주 중 ${plannedDays}일 계획을 세웠고, 그중 ${perfectDays}일은 모두 끝냈어요.`
            : null;

    // 열(주)마다 그 주에 달이 바뀌면 달 이름 표시
    const monthLabels = Array.from({ length: WEEKS }, (_, w) => {
        const weekStart = days[w * 7];
        const prev = w > 0 ? days[(w - 1) * 7] : null;
        if (!prev || prev.getMonth() !== weekStart.getMonth()) return `${weekStart.getMonth() + 1}월`;
        return "";
    });

    return (
        <StatCard title="완료 잔디" insight={insight}>
            {errorMessage ? (
                <p className="text-sm text-red-500 text-center">{errorMessage}</p>
            ) : (
                <>
                    <div className="flex gap-1">
                        {/* 요일 - 월/수/금만 표시 */}
                        <div className="grid grid-rows-[auto_repeat(7,1fr)] gap-1 text-[10px] text-gray-400">
                            <span className="h-3" />
                            {WEEKDAYS.map((w, i) => (
                                <span key={w} className="flex items-center leading-none">
                                    {i % 2 === 1 ? w : ""}
                                </span>
                            ))}
                        </div>

                        <div className="flex-1 min-w-0">
                            <div className="mb-1 grid grid-cols-12 gap-1 text-[10px] text-gray-400 h-3 leading-3">
                                {monthLabels.map((label, i) => (
                                    <span key={i} className="whitespace-nowrap">{label}</span>
                                ))}
                            </div>
                            <div className={`grid grid-flow-col grid-cols-12 grid-rows-7 gap-1 ${stats ? "" : "opacity-50"}`}>
                                {days.map((d) => {
                                    const key = toDateKey(d);
                                    if (key > todayKey) return <span key={key} className="aspect-square" />;
                                    const stat = stats?.[key];
                                    return (
                                        <span
                                            key={key}
                                            title={
                                                stat
                                                    ? `${d.getMonth() + 1}/${d.getDate()} · ${stat.totalCount}개 중 ${stat.doneCount}개 완료`
                                                    : `${d.getMonth() + 1}/${d.getDate()} · 계획 없음`
                                            }
                                            className={`aspect-square rounded-[4px] ${LEVELS[getLevel(stat)].className} ${
                                                key === todayKey ? "outline outline-2 outline-offset-1 outline-slate-900" : ""
                                            }`}
                                        />
                                    );
                                })}
                            </div>
                        </div>
                    </div>

                    {/* 범례 */}
                    <div className="mt-4 flex items-center justify-end gap-1 text-[10px] text-gray-400">
                        <span className="mr-1">적음</span>
                        {LEVELS.map((l) => (
                            <span key={l.label} title={l.label} className={`h-3 w-3 rounded-[3px] ${l.className}`} />
                        ))}
                        <span className="ml-1">많음</span>
                    </div>
                </>
            )}
        </StatCard>
    );
}
