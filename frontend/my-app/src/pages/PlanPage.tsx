// pages/PlanPage.tsx

import { useEffect, useMemo, useState } from "react";
import PlanInput from "../components/common/PlanInput";
import AnalyzeButton from "../components/PlanPage/button";
import AnalysisResult from "../components/PlanPage/AnalysisResult";
import PlanCalendar from "../components/PlanPage/PlanCalendar";
import { addDays, formatDayTitle, getVisibleDays, isSameDay, toDateKey } from "../components/PlanPage/calendarDate";
import type { CalendarView } from "../components/PlanPage/calendarDate";
import { getErrorMessage } from "../api/api";
import { analyzePlans, getPlansInRange } from "../api/plan";
import type { Plan } from "../api/plan";

export default function PlanPage() {
    const today = useMemo(() => new Date(), []);
    const todayKey = toDateKey(today);

    const [view, setView] = useState<CalendarView>("week");
    const [cursor, setCursor] = useState<Date>(today); // 보고 있는 주/월의 기준 날짜
    const [selected, setSelected] = useState<Date>(today);

    const [plans, setPlans] = useState<Plan[]>([]); // 달력에 보이는 기간의 계획
    const [loadedRange, setLoadedRange] = useState(""); // 마지막으로 응답을 받은 기간 - 보이는 기간과 다르면 불러오는 중
    const [loadError, setLoadError] = useState("");

    const [analyzedPlans, setAnalyzedPlans] = useState<Plan[] | null>(null); // 분석하기 전에는 null
    const [analyzing, setAnalyzing] = useState(false);
    const [analyzeError, setAnalyzeError] = useState("");

    const days = useMemo(() => getVisibleDays(view, cursor), [view, cursor]);
    const fromKey = toDateKey(days[0]);
    const toKey = toDateKey(days[days.length - 1]);
    const rangeKey = `${fromKey}~${toKey}`;
    const loading = loadedRange !== rangeKey;

    // 보이는 기간이 바뀔 때마다 그 기간의 계획을 다시 불러옴
    useEffect(() => {
        let ignore = false; // 빠르게 넘길 때 늦게 도착한 이전 기간 응답은 버림
        getPlansInRange(fromKey, toKey)
            .then((result) => {
                if (ignore) return;
                setPlans(result);
                setLoadError("");
                setLoadedRange(`${fromKey}~${toKey}`);
            })
            .catch((error) => {
                if (ignore) return;
                console.error("계획 목록을 불러오지 못했습니다:", error);
                setLoadError(getErrorMessage(error, "계획 목록을 불러오지 못했습니다."));
                setLoadedRange(`${fromKey}~${toKey}`);
            });
        return () => {
            ignore = true;
        };
    }, [fromKey, toKey]);

    const plansByDate = useMemo(() => {
        const grouped: Record<string, Plan[]> = {};
        for (const p of plans) (grouped[p.planDate] ??= []).push(p);
        return grouped;
    }, [plans]);

    const selectedKey = toDateKey(selected);
    const isToday = selectedKey === todayKey;

    const handleMove = (dir: -1 | 1) => {
        if (view === "week") {
            setCursor((c) => addDays(c, dir * 7));
            setSelected((s) => addDays(s, dir * 7));
        } else {
            setCursor((c) => new Date(c.getFullYear(), c.getMonth() + dir, 1));
        }
    };

    const handleSelect = (d: Date) => {
        setSelected(d);
        // 월간에서 앞뒤 달 칸을 누르면 그 달로 넘어감
        if (view === "month" && d.getMonth() !== cursor.getMonth()) setCursor(d);
    };

    const handleViewChange = (v: CalendarView) => {
        setView(v);
        setCursor(selected);
    };

    const handleToday = () => {
        setCursor(today);
        setSelected(today);
    };

    // 추가·수정·완료·연기 결과 반영 - 연기된 계획은 planDate가 바뀌어 다른 날짜 칸으로 옮겨감
    const handleUpsert = (plan: Plan) => {
        setPlans((prev) =>
            prev.some((p) => p.planId === plan.planId)
                ? prev.map((p) => (p.planId === plan.planId ? { ...p, ...plan } : p))
                : [...prev, plan]
        );
    };

    const handleRemove = (planId: number) => {
        setPlans((prev) => prev.filter((p) => p.planId !== planId));
    };

    // 서버가 aiService로 오늘 계획을 다시 분석하고 추천 순서대로 돌려줌
    const handleAnalyze = async () => {
        setAnalyzing(true);
        setAnalyzeError("");

        try {
            const result = await analyzePlans();

            setAnalyzedPlans(result.filter((p) => p.status === "TODO"));
            // 오늘 칸도 추천 순서·분류 결과로 갱신
            setPlans((prev) => [...prev.filter((p) => p.planDate !== todayKey), ...result]);
        } catch (error) {
            console.error("계획을 분석하지 못했습니다:", error);
            setAnalyzeError(getErrorMessage(error, "계획을 분석하지 못했습니다."));
        } finally {
            setAnalyzing(false);
        }
    };

    return (
            <div className="space-y-8">

                <PlanCalendar
                    view={view}
                    cursor={cursor}
                    days={days}
                    selected={selected}
                    today={today}
                    plansByDate={plansByDate}
                    loading={loading}
                    onViewChange={handleViewChange}
                    onMove={handleMove}
                    onSelect={handleSelect}
                    onToday={handleToday}
                />

                {loadError && (
                    <p className="text-sm text-red-500 text-center">{loadError}</p>
                )}

                <PlanInput
                    key={selectedKey} // 날짜를 바꾸면 입력·수정 중이던 내용 초기화
                    title={`${isSameDay(selected, today) ? "오늘 · " : ""}${formatDayTitle(selected)} 계획`}
                    planDate={selectedKey}
                    plans={plansByDate[selectedKey] ?? []}
                    readOnly={selectedKey < todayKey}
                    canFocus={isToday}
                    onUpsert={handleUpsert}
                    onRemove={handleRemove}
                />

                {/* 분석은 오늘 계획만 가능 */}
                {isToday && (
                    <>
                        <AnalyzeButton onClick={handleAnalyze} loading={analyzing} />

                        {analyzeError && (
                            <p className="text-sm text-red-500 text-center">{analyzeError}</p>
                        )}

                        {analyzedPlans && <AnalysisResult plans={analyzedPlans} />}
                    </>
                )}
            </div>
    );
}
