// pages/FocusPage.tsx
// 집중 타이머 - 오늘 계획을 골라 0부터 시간을 재고, 실제로 집중한 시간을 기록
// 링 한 바퀴 = 예상 시간, 넘기면 밝은 파랑으로 한 바퀴 더 (두 바퀴 이후로는 링은 가득 찬 채 시간만 계속 셈)
// 진행 중인 타이머는 localStorage에 시작 시각으로 저장 - 다른 페이지에 다녀오거나 새로고침해도 이어짐
import { useEffect, useMemo, useState } from "react";
import type { ReactNode } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { getErrorMessage } from "../api/api";
import { getPlans, recordFocus } from "../api/plan";
import type { Plan } from "../api/plan";

const SESSION_KEY = "neroplan.focusSession";
const DEFAULT_LAP_MINUTES = 60; // 예상 시간이 없는 계획은 한 바퀴 = 1시간
const RING_RADIUS = 88;
const RING_LENGTH = 2 * Math.PI * RING_RADIUS;

interface FocusSession {
    planId: number;
    accumulatedMs: number; // 일시정지 전까지 쌓인 시간
    startedAt: number | null; // 진행 중이면 시작(또는 재개) 시각, 일시정지면 null
}

// 브라우저 저장소는 막혀 있을 수 있으므로 실패해도 타이머는 동작
const loadSession = (): FocusSession | null => {
    try {
        const raw = localStorage.getItem(SESSION_KEY);
        return raw ? (JSON.parse(raw) as FocusSession) : null;
    } catch {
        return null;
    }
};

const saveSession = (session: FocusSession | null) => {
    try {
        if (session) localStorage.setItem(SESSION_KEY, JSON.stringify(session));
        else localStorage.removeItem(SESSION_KEY);
    } catch {
        // 저장하지 못하면 이 페이지에 있는 동안만 유지
    }
};

const elapsedMs = (s: FocusSession, now: number) => s.accumulatedMs + (s.startedAt != null ? now - s.startedAt : 0);

const formatClock = (ms: number) => {
    const total = Math.floor(ms / 1000);
    const h = Math.floor(total / 3600);
    const m = Math.floor((total % 3600) / 60);
    const s = total % 60;
    const mmss = `${String(m).padStart(2, "0")}:${String(s).padStart(2, "0")}`;
    return h > 0 ? `${h}:${mmss}` : mmss;
};

interface FocusResult {
    plan: Plan;
    minutes: number;
    completed: boolean;
}

export default function FocusPage() {
    const [searchParams] = useSearchParams();
    const [plans, setPlans] = useState<Plan[] | null>(null);
    const [loadError, setLoadError] = useState("");
    const [session, setSession] = useState<FocusSession | null>(loadSession);
    const [pickedId, setPickedId] = useState<number | null>(() => Number(searchParams.get("planId")) || null);
    const [now, setNow] = useState(() => Date.now());
    const [finishing, setFinishing] = useState(false); // 기록 방법을 고르는 중
    const [saving, setSaving] = useState(false);
    const [saveError, setSaveError] = useState("");
    const [result, setResult] = useState<FocusResult | null>(null);

    useEffect(() => {
        getPlans()
            .then((result) => {
                setPlans(result);
                // 저장된 타이머의 계획이 오늘 목록에 없으면(삭제·지난 날짜) 버림
                const saved = loadSession();
                if (saved && !result.some((p) => p.planId === saved.planId)) {
                    setSession(null);
                    saveSession(null);
                }
            })
            .catch((error) => {
                console.error("오늘 계획을 불러오지 못했습니다:", error);
                setLoadError(getErrorMessage(error, "오늘 계획을 불러오지 못했습니다."));
            });
    }, []);

    const updateSession = (next: FocusSession | null) => {
        setSession(next);
        saveSession(next);
    };

    const running = session?.startedAt != null;

    // 진행 중일 때만 1초마다 다시 그림 - 경과 시간은 시작 시각 기준이라 탭이 멈춰도 정확함
    useEffect(() => {
        if (!running) return;
        const id = window.setInterval(() => setNow(Date.now()), 1000);
        return () => window.clearInterval(id);
    }, [running]);

    const todoPlans = useMemo(() => plans?.filter((p) => p.status === "TODO") ?? [], [plans]);
    // 세션이 있으면 그 계획, 없으면 고른 계획 (목록에 없으면 첫 번째 계획)
    const activeId = session?.planId ?? (todoPlans.some((p) => p.planId === pickedId) ? pickedId : todoPlans[0]?.planId ?? null);
    const activePlan = plans?.find((p) => p.planId === activeId) ?? null;

    // 링 한 바퀴 = 계획에 입력한 예상 시간 - 저장된 타이머 값이 아니라 항상 계획의 현재 값을 씀
    // (예전 버전이 저장한 목표 시간이 남아 있어도 무시되고, 계획 페이지에서 예상 시간을 고치면 바로 반영됨)
    const targetMinutes = activePlan?.estimatedMinutes ?? DEFAULT_LAP_MINUTES;
    const hasEstimate = activePlan?.estimatedMinutes != null;
    const elapsed = session ? elapsedMs(session, now) : 0;
    const targetMs = targetMinutes * 60_000;
    const over = elapsed > targetMs;
    const firstLap = Math.min(elapsed / targetMs, 1); // 예상 시간까지
    const secondLap = Math.min(Math.max(elapsed - targetMs, 0) / targetMs, 1); // 초과한 시간 - 한 바퀴까지
    const recordMinutes = Math.round(elapsed / 60_000);

    // 탭 제목에 집중한 시간 표시
    useEffect(() => {
        if (!session || !activePlan) return;
        const original = document.title;
        document.title = `${formatClock(elapsed)} · ${activePlan.content}`;
        return () => {
            document.title = original;
        };
    }, [session, activePlan, elapsed]);

    const handleStart = () => {
        if (!activePlan) return;
        const t = Date.now();
        setNow(t);
        setResult(null);
        updateSession(
            session
                ? { ...session, startedAt: t }
                : { planId: activePlan.planId, accumulatedMs: 0, startedAt: t }
        );
    };

    const handlePause = () => {
        if (!session || session.startedAt == null) return;
        const t = Date.now();
        setNow(t);
        updateSession({ ...session, accumulatedMs: elapsedMs(session, t), startedAt: null });
    };

    const handleFinish = () => {
        handlePause();
        setSaveError("");
        setFinishing(true);
    };

    const handleDiscard = () => {
        updateSession(null);
        setFinishing(false);
    };

    const handleRecord = async (complete: boolean) => {
        if (!session || !activePlan) return;
        setSaving(true);
        setSaveError("");
        try {
            const updated = await recordFocus(session.planId, recordMinutes, complete);
            setPlans((prev) => prev?.map((p) => (p.planId === updated.planId ? updated : p)) ?? prev);
            setResult({ plan: updated, minutes: recordMinutes, completed: complete });
            updateSession(null);
            setFinishing(false);
        } catch (error) {
            console.error("집중 기록을 저장하지 못했습니다:", error);
            setSaveError(getErrorMessage(error, "집중 기록을 저장하지 못했습니다."));
        } finally {
            setSaving(false);
        }
    };

    if (loadError) {
        return <p className="text-sm text-red-500 text-center">{loadError}</p>;
    }

    if (!plans) {
        return <p className="text-center text-gray-500">오늘 계획을 불러오는 중...</p>;
    }

    return (
        <div className="space-y-8">
            {result && <ResultCard result={result} />}

            {!activePlan ? (
                <section className="bg-white rounded-3xl p-6 shadow-sm text-center space-y-3">
                    <h2 className="text-xl font-bold">집중할 계획이 없어요</h2>
                    <p className="text-sm text-gray-500">오늘 할 일을 먼저 추가하면 여기서 타이머를 쓸 수 있어요.</p>
                    <Link
                        to="/plan"
                        className="block w-full rounded-2xl bg-slate-900 py-3 font-bold text-white transition hover:bg-slate-700"
                    >
                        계획 세우러 가기
                    </Link>
                </section>
            ) : (
                <>
                    {/* ── 타이머 ── */}
                    <section className="bg-white rounded-3xl p-6 shadow-sm">
                        <p className="text-center text-sm text-gray-400">{session ? "집중 중인 계획" : "집중할 계획"}</p>
                        <h2 className="mt-1 text-xl font-bold text-center truncate">{activePlan.content}</h2>
                        <p className="mt-1 text-center text-sm text-gray-500">
                            {activePlan.estimatedMinutes != null ? `예상 ${activePlan.estimatedMinutes}분` : "예상 시간 없음"}
                            {activePlan.actualMinutes != null && ` · 지금까지 ${activePlan.actualMinutes}분 집중`}
                        </p>

                        <hr className="my-5" />

                        <div className="relative mx-auto h-52 w-52">
                            <svg viewBox="0 0 200 200" className="h-full w-full -rotate-90">
                                <circle cx="100" cy="100" r={RING_RADIUS} fill="none" strokeWidth="10" className="stroke-gray-100" />
                                {/* 첫 바퀴 - 예상 시간까지 */}
                                <circle
                                    cx="100"
                                    cy="100"
                                    r={RING_RADIUS}
                                    fill="none"
                                    strokeWidth="10"
                                    strokeLinecap={firstLap > 0 ? "round" : "butt"}
                                    strokeDasharray={RING_LENGTH}
                                    strokeDashoffset={RING_LENGTH * (1 - firstLap)}
                                    className="stroke-blue-500 transition-[stroke-dashoffset] duration-1000 ease-linear"
                                />
                                {/* 두 번째 바퀴 - 예상 시간을 넘긴 만큼 밝은 파랑으로 덮음 */}
                                {secondLap > 0 && (
                                    <circle
                                        cx="100"
                                        cy="100"
                                        r={RING_RADIUS}
                                        fill="none"
                                        strokeWidth="10"
                                        strokeLinecap="round"
                                        strokeDasharray={RING_LENGTH}
                                        strokeDashoffset={RING_LENGTH * (1 - secondLap)}
                                        className="stroke-blue-200 transition-[stroke-dashoffset] duration-1000 ease-linear"
                                    />
                                )}
                            </svg>
                            <div className="absolute inset-0 flex flex-col items-center justify-center">
                                <span className="text-xs font-semibold text-gray-400">
                                    {over && hasEstimate ? "예상 시간 초과" : "집중한 시간"}
                                </span>
                                <span className="mt-1 text-4xl font-bold tabular-nums text-slate-900">
                                    {formatClock(elapsed)}
                                </span>
                                <span className="mt-1 text-xs text-gray-400 tabular-nums">
                                    {!hasEstimate
                                        ? `예상 시간 없음 · 한 바퀴 = ${targetMinutes}분`
                                        : over
                                          ? `+${formatClock(elapsed - targetMs)} · 예상 ${targetMinutes}분`
                                          : `한 바퀴 = 예상 ${targetMinutes}분`}
                                </span>
                            </div>
                        </div>

                        {/* 링 범례 */}
                        <div className="mt-4 flex justify-center gap-4 text-xs text-gray-500">
                            <span className="flex items-center gap-1">
                                <span className="h-2 w-2 rounded-full bg-blue-500" />
                                예상 시간까지
                            </span>
                            <span className="flex items-center gap-1">
                                <span className="h-2 w-2 rounded-full bg-blue-200" />
                                초과한 시간
                            </span>
                        </div>

                        {finishing && session ? (
                            <div className="mt-6 space-y-3 rounded-2xl bg-gray-50 p-4">
                                {recordMinutes < 1 ? (
                                    <>
                                        <p className="text-sm text-center text-gray-600">1분 이상 집중해야 기록할 수 있어요.</p>
                                        <div className="grid grid-cols-2 gap-2">
                                            <SecondaryButton onClick={() => setFinishing(false)}>계속하기</SecondaryButton>
                                            <SecondaryButton onClick={handleDiscard}>기록 안 함</SecondaryButton>
                                        </div>
                                    </>
                                ) : (
                                    <>
                                        <p className="text-sm text-center text-gray-600">
                                            <span className="font-bold text-slate-900">{recordMinutes}분</span> 집중했어요. 어떻게 기록할까요?
                                        </p>
                                        <button
                                            onClick={() => handleRecord(true)}
                                            disabled={saving}
                                            className="w-full rounded-2xl bg-slate-900 py-3 font-bold text-white transition hover:bg-slate-700 disabled:opacity-40"
                                        >
                                            완료로 기록
                                        </button>
                                        <div className="grid grid-cols-2 gap-2">
                                            <SecondaryButton onClick={() => handleRecord(false)} disabled={saving}>
                                                시간만 기록
                                            </SecondaryButton>
                                            <SecondaryButton onClick={() => setFinishing(false)} disabled={saving}>
                                                계속하기
                                            </SecondaryButton>
                                        </div>
                                        <button
                                            onClick={handleDiscard}
                                            disabled={saving}
                                            className="w-full text-xs text-gray-400 underline"
                                        >
                                            기록하지 않고 버리기
                                        </button>
                                    </>
                                )}
                                {saveError && <p className="text-sm text-red-500 text-center">{saveError}</p>}
                            </div>
                        ) : (
                            <div className="mt-6 grid grid-cols-2 gap-2">
                                {running ? (
                                    <SecondaryButton onClick={handlePause}>일시정지</SecondaryButton>
                                ) : (
                                    <button
                                        onClick={handleStart}
                                        className={`rounded-2xl bg-slate-900 py-3 font-bold text-white transition hover:bg-slate-700 ${session ? "" : "col-span-2"}`}
                                    >
                                        {session ? "이어하기" : "집중 시작"}
                                    </button>
                                )}
                                {session && <SecondaryButton onClick={handleFinish}>끝내기</SecondaryButton>}
                            </div>
                        )}
                    </section>

                    {/* ── 오늘 할 일 - 집중 중에는 바꿀 수 없음 ── */}
                    <section className="bg-white rounded-3xl p-6 shadow-sm">
                        <h2 className="text-xl font-bold text-center mb-3">오늘 할 일</h2>
                        <hr className="mb-4" />
                        {todoPlans.length === 0 ? (
                            <p className="text-center text-gray-500">남은 계획이 없어요. 오늘 다 끝냈어요!</p>
                        ) : (
                            <ul className="space-y-2">
                                {todoPlans.map((p) => {
                                    const selected = p.planId === activeId;
                                    return (
                                        <li key={p.planId}>
                                            <button
                                                onClick={() => setPickedId(p.planId)}
                                                disabled={session != null && !selected}
                                                className={`flex w-full items-center gap-3 rounded-2xl border px-4 py-3 text-left transition ${
                                                    selected ? "border-slate-900" : "border-gray-100 hover:bg-gray-50"
                                                } disabled:opacity-40`}
                                            >
                                                <span className="flex-1 min-w-0 truncate font-semibold">{p.content}</span>
                                                <span className="shrink-0 text-sm text-gray-500">
                                                    {p.estimatedMinutes != null ? `${p.estimatedMinutes}분` : "-"}
                                                    {p.actualMinutes != null && (
                                                        <span className="text-gray-400"> · {p.actualMinutes}분 집중</span>
                                                    )}
                                                </span>
                                            </button>
                                        </li>
                                    );
                                })}
                            </ul>
                        )}
                        {session && (
                            <p className="mt-3 text-center text-xs text-gray-400">다른 계획은 지금 집중을 끝낸 뒤 고를 수 있어요.</p>
                        )}
                    </section>
                </>
            )}
        </div>
    );
}

// 방금 기록한 결과 - 예상 대비 실제
function ResultCard({ result }: { result: FocusResult }) {
    const { plan, minutes, completed } = result;
    const estimated = plan.estimatedMinutes;
    const total = plan.actualMinutes ?? minutes;
    const ratio = estimated ? total / estimated : null;

    let message = `${minutes}분 집중을 기록했어요.`;
    if (completed && ratio != null) {
        if (ratio > 1.1) message = `예상보다 ${ratio.toFixed(1)}배 걸렸어요. 다음엔 시간을 넉넉히 잡아 볼까요?`;
        else if (ratio < 0.9) message = `예상보다 빨리 끝냈어요!`;
        else message = `예상한 시간에 딱 맞게 끝냈어요!`;
    }

    return (
        <section className="bg-white rounded-3xl p-6 shadow-sm text-center">
            <p className="text-sm text-gray-400">{completed ? "완료" : "기록 저장"}</p>
            <h2 className="mt-1 text-xl font-bold truncate">{plan.content}</h2>
            <div className="mt-4 grid grid-cols-2 divide-x divide-gray-100">
                <div>
                    <p className="text-xs text-gray-400">예상</p>
                    <p className="text-2xl font-bold">{estimated != null ? `${estimated}분` : "-"}</p>
                </div>
                <div>
                    <p className="text-xs text-gray-400">실제 (누적)</p>
                    <p className="text-2xl font-bold">{total}분</p>
                </div>
            </div>
            <p className="mt-4 text-sm font-semibold">{message}</p>
        </section>
    );
}

function SecondaryButton({
    onClick,
    disabled,
    children,
}: {
    onClick: () => void;
    disabled?: boolean;
    children: ReactNode;
}) {
    return (
        <button
            onClick={onClick}
            disabled={disabled}
            className="rounded-2xl bg-gray-100 py-3 font-bold text-slate-700 transition hover:bg-gray-200 disabled:opacity-40"
        >
            {children}
        </button>
    );
}
