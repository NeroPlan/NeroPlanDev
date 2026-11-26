// pages/CategoryPage.tsx
// 카테고리 관리 - AI가 분류한 카테고리 목록, 속한 계획 보기, 이름 변경, 삭제(계획을 다른 카테고리로 옮기거나 미분류로)
import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { getErrorMessage } from "../api/api";
import { deleteCategory, getCategories, getCategoryPlans, renameCategory } from "../api/plan";
import type { Category, Plan } from "../api/plan";
import { toDateKey } from "../components/PlanPage/calendarDate";

const MAX_CATEGORY_NAME_LENGTH = 50; // backendPlan category.name 컬럼 길이

type Mode =
    | { type: "view" }
    | { type: "rename"; categoryId: number; name: string }
    | { type: "delete"; categoryId: number; moveTo: string }; // moveTo: categoryId 문자열, ""이면 미분류

export default function CategoryPage() {
    const navigate = useNavigate();
    const [categories, setCategories] = useState<Category[] | null>(null);
    const [mode, setMode] = useState<Mode>({ type: "view" });
    const [saving, setSaving] = useState(false);
    const [errorMessage, setErrorMessage] = useState("");

    // 펼친 카테고리와 불러온 계획 목록 - 한 번 불러온 카테고리는 다시 펼쳐도 요청하지 않음
    const [expandedId, setExpandedId] = useState<number | null>(null);
    const [plansByCategory, setPlansByCategory] = useState<Record<number, Plan[]>>({});
    const [plansError, setPlansError] = useState<Record<number, string>>({});

    useEffect(() => {
        getCategories()
            .then(setCategories)
            .catch((error) => {
                console.error("카테고리를 불러오지 못했습니다:", error);
                setErrorMessage(getErrorMessage(error, "카테고리를 불러오지 못했습니다."));
            });
    }, []);

    const toggleExpand = (categoryId: number) => {
        if (expandedId === categoryId) {
            setExpandedId(null);
            return;
        }
        setExpandedId(categoryId);
        if (plansByCategory[categoryId]) return;

        setPlansError((prev) => ({ ...prev, [categoryId]: "" }));
        getCategoryPlans(categoryId)
            .then((plans) => setPlansByCategory((prev) => ({ ...prev, [categoryId]: plans })))
            .catch((error) => {
                console.error("계획 목록을 불러오지 못했습니다:", error);
                setPlansError((prev) => ({
                    ...prev,
                    [categoryId]: getErrorMessage(error, "계획 목록을 불러오지 못했습니다."),
                }));
            });
    };

    const cancel = () => {
        setMode({ type: "view" });
        setErrorMessage("");
    };

    const handleRename = async (categoryId: number, name: string) => {
        if (!name.trim()) {
            setErrorMessage("카테고리 이름을 입력하세요.");
            return;
        }
        setSaving(true);
        try {
            const updated = await renameCategory(categoryId, name);
            setCategories((prev) =>
                prev?.map((c) => (c.categoryId === categoryId ? { ...c, name: updated.name } : c)) ?? prev
            );
            cancel();
        } catch (error) {
            console.error("이름을 바꾸지 못했습니다:", error);
            setErrorMessage(getErrorMessage(error, "이름을 바꾸지 못했습니다."));
        } finally {
            setSaving(false);
        }
    };

    const handleDelete = async (categoryId: number, moveTo: string) => {
        setSaving(true);
        try {
            const target = moveTo ? Number(moveTo) : undefined;
            await deleteCategory(categoryId, target);
            setCategories((prev) => {
                if (!prev) return prev;
                const removed = prev.find((c) => c.categoryId === categoryId);
                return prev
                    .filter((c) => c.categoryId !== categoryId)
                    .map((c) =>
                        c.categoryId === target ? { ...c, planCount: c.planCount + (removed?.planCount ?? 0) } : c
                    );
            });
            // 삭제한 카테고리와 계획을 넘겨받은 카테고리는 목록이 달라졌으므로 다시 불러오도록 비움
            setPlansByCategory((prev) => {
                const next = { ...prev };
                delete next[categoryId];
                if (target !== undefined) delete next[target];
                return next;
            });
            if (expandedId === categoryId || expandedId === target) setExpandedId(null);
            cancel();
        } catch (error) {
            console.error("카테고리를 삭제하지 못했습니다:", error);
            setErrorMessage(getErrorMessage(error, "카테고리를 삭제하지 못했습니다."));
        } finally {
            setSaving(false);
        }
    };

    return (
        <div className="mx-auto w-full max-w-[26rem] space-y-6 pb-10">
            <h1 className="pt-2 text-center text-2xl font-bold text-slate-900">카테고리 관리</h1>

            <p className="text-center text-sm text-slate-500">
                분석하기를 누르면 AI가 계획을 이 카테고리들로 분류해요.
                <br />
                카테고리별 성공률은 분석 페이지에서 볼 수 있어요.
            </p>

            {categories === null ? (
                <p className="text-center text-sm text-slate-500">
                    {errorMessage || "카테고리를 불러오는 중..."}
                </p>
            ) : categories.length === 0 ? (
                <section className="rounded-[28px] bg-white px-6 py-8 text-center shadow-sm">
                    <p className="font-semibold text-slate-900">아직 카테고리가 없어요</p>
                    <p className="mt-1 text-sm text-slate-500">계획을 세우고 분석하기를 누르면 자동으로 만들어져요.</p>
                </section>
            ) : (
                <section className="overflow-hidden rounded-[28px] bg-white shadow-sm">
                    {categories.map((c, index) => {
                        const border = index !== categories.length - 1 ? "border-b border-slate-100" : "";

                        if (mode.type === "rename" && mode.categoryId === c.categoryId) {
                            return (
                                <div key={c.categoryId} className={`space-y-3 px-6 py-4 ${border}`}>
                                    <input
                                        type="text"
                                        value={mode.name}
                                        maxLength={MAX_CATEGORY_NAME_LENGTH}
                                        autoFocus
                                        onChange={(e) => setMode({ ...mode, name: e.target.value })}
                                        onKeyDown={(e) => e.key === "Enter" && handleRename(c.categoryId, mode.name)}
                                        className="w-full rounded-2xl border border-slate-200 px-4 py-2.5 text-base outline-none focus:border-slate-900"
                                    />
                                    <ActionButtons
                                        onCancel={cancel}
                                        onConfirm={() => handleRename(c.categoryId, mode.name)}
                                        confirmLabel="저장"
                                        disabled={saving || mode.name.trim() === c.name}
                                    />
                                </div>
                            );
                        }

                        if (mode.type === "delete" && mode.categoryId === c.categoryId) {
                            const others = categories.filter((o) => o.categoryId !== c.categoryId);
                            return (
                                <div key={c.categoryId} className={`space-y-3 bg-red-50/40 px-6 py-4 ${border}`}>
                                    <p className="font-semibold text-slate-900">'{c.name}'을(를) 삭제할까요?</p>
                                    {c.planCount > 0 && (
                                        <label className="block space-y-1">
                                            <span className="text-sm text-slate-600">
                                                이 카테고리의 계획 {c.planCount}개를 옮길 곳
                                            </span>
                                            <select
                                                value={mode.moveTo}
                                                onChange={(e) => setMode({ ...mode, moveTo: e.target.value })}
                                                className="w-full rounded-2xl border border-slate-200 bg-white px-4 py-2.5 text-base outline-none focus:border-slate-900"
                                            >
                                                <option value="">미분류 (다음 분석 때 다시 분류)</option>
                                                {others.map((o) => (
                                                    <option key={o.categoryId} value={o.categoryId}>
                                                        {o.name}
                                                    </option>
                                                ))}
                                            </select>
                                        </label>
                                    )}
                                    <ActionButtons
                                        onCancel={cancel}
                                        onConfirm={() => handleDelete(c.categoryId, mode.moveTo)}
                                        confirmLabel="삭제"
                                        danger
                                        disabled={saving}
                                    />
                                </div>
                            );
                        }

                        const expanded = expandedId === c.categoryId;
                        return (
                            <div key={c.categoryId} className={border}>
                                <div className="flex items-center gap-3 px-6 py-5">
                                    <button
                                        type="button"
                                        onClick={() => toggleExpand(c.categoryId)}
                                        aria-expanded={expanded}
                                        className="flex min-w-0 flex-1 items-center gap-2 text-left"
                                    >
                                        <span
                                            className={`text-xs text-slate-400 transition-transform ${expanded ? "rotate-90" : ""}`}
                                            aria-hidden
                                        >
                                            ▶
                                        </span>
                                        <span className="min-w-0">
                                            <span className="block truncate text-base font-semibold text-slate-900">{c.name}</span>
                                            <span className="block text-xs text-slate-400">계획 {c.planCount}개</span>
                                        </span>
                                    </button>
                                    <button
                                        type="button"
                                        onClick={() => {
                                            setErrorMessage("");
                                            setMode({ type: "rename", categoryId: c.categoryId, name: c.name });
                                        }}
                                        className="text-sm font-semibold text-slate-500 hover:text-slate-900"
                                    >
                                        이름 변경
                                    </button>
                                    <button
                                        type="button"
                                        onClick={() => {
                                            setErrorMessage("");
                                            setMode({ type: "delete", categoryId: c.categoryId, moveTo: "" });
                                        }}
                                        className="text-sm font-semibold text-red-500 hover:text-red-600"
                                    >
                                        삭제
                                    </button>
                                </div>
                                {expanded && (
                                    <CategoryPlanList
                                        plans={plansByCategory[c.categoryId]}
                                        error={plansError[c.categoryId]}
                                    />
                                )}
                            </div>
                        );
                    })}
                </section>
            )}

            {categories !== null && errorMessage && (
                <p className="text-center text-sm text-red-500">{errorMessage}</p>
            )}

            <button
                type="button"
                onClick={() => navigate("/mypage")}
                className="w-full rounded-2xl bg-slate-100 py-3 font-bold text-slate-700 transition hover:bg-slate-200"
            >
                마이페이지로
            </button>
        </div>
    );
}

const STATUS_STYLE = {
    done: { label: "완료", className: "bg-emerald-50 text-emerald-600" },
    missed: { label: "미완료", className: "bg-slate-100 text-slate-500" },
    upcoming: { label: "예정", className: "bg-sky-50 text-sky-600" },
};

// 펼친 카테고리의 계획 목록 - 최근 수행일부터, 어떤 계획이 이 이름으로 묶였는지 확인용
function CategoryPlanList({ plans, error }: { plans?: Plan[]; error?: string }) {
    if (error) return <p className="px-6 pb-5 text-sm text-red-500">{error}</p>;
    if (!plans) return <p className="px-6 pb-5 text-sm text-slate-400">불러오는 중...</p>;
    if (plans.length === 0) return <p className="px-6 pb-5 text-sm text-slate-400">이 카테고리에 계획이 없어요.</p>;

    const todayKey = toDateKey(new Date());
    return (
        <ul className="mx-6 mb-5 max-h-72 space-y-1 overflow-y-auto rounded-2xl bg-slate-50 p-2">
            {plans.map((p) => {
                const status =
                    p.status === "DONE" ? STATUS_STYLE.done : p.planDate < todayKey ? STATUS_STYLE.missed : STATUS_STYLE.upcoming;
                const [, month, day] = p.planDate.split("-");
                return (
                    <li key={p.planId} className="flex items-center gap-3 rounded-xl px-3 py-2">
                        <span className="w-11 shrink-0 text-xs tabular-nums text-slate-400">
                            {Number(month)}/{Number(day)}
                        </span>
                        <span className="min-w-0 flex-1 truncate text-sm text-slate-800" title={p.content}>
                            {p.content}
                        </span>
                        <span className={`shrink-0 rounded-full px-2 py-0.5 text-xs font-semibold ${status.className}`}>
                            {status.label}
                        </span>
                    </li>
                );
            })}
        </ul>
    );
}

interface ActionButtonsProps {
    onCancel: () => void;
    onConfirm: () => void;
    confirmLabel: string;
    disabled: boolean;
    danger?: boolean;
}

function ActionButtons({ onCancel, onConfirm, confirmLabel, disabled, danger }: ActionButtonsProps) {
    return (
        <div className="flex gap-2">
            <button
                type="button"
                onClick={onCancel}
                className="flex-1 rounded-2xl bg-slate-100 py-2.5 text-sm font-bold text-slate-700 transition hover:bg-slate-200"
            >
                취소
            </button>
            <button
                type="button"
                onClick={onConfirm}
                disabled={disabled}
                className={`flex-1 rounded-2xl py-2.5 text-sm font-bold text-white transition disabled:opacity-40 ${
                    danger ? "bg-red-500 hover:bg-red-600" : "bg-slate-900 hover:bg-slate-700"
                }`}
            >
                {confirmLabel}
            </button>
        </div>
    );
}
