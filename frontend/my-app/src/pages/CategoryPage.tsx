// pages/CategoryPage.tsx
// 카테고리 관리 - AI가 분류한 카테고리 목록, 이름 변경, 삭제(계획을 다른 카테고리로 옮기거나 미분류로)
import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { getErrorMessage } from "../api/api";
import { deleteCategory, getCategories, renameCategory } from "../api/plan";
import type { Category } from "../api/plan";

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

    useEffect(() => {
        getCategories()
            .then(setCategories)
            .catch((error) => {
                console.error("카테고리를 불러오지 못했습니다:", error);
                setErrorMessage(getErrorMessage(error, "카테고리를 불러오지 못했습니다."));
            });
    }, []);

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
            cancel();
        } catch (error) {
            console.error("카테고리를 삭제하지 못했습니다:", error);
            setErrorMessage(getErrorMessage(error, "카테고리를 삭제하지 못했습니다."));
        } finally {
            setSaving(false);
        }
    };

    return (
        <div className="mx-auto w-full max-w-[468px] space-y-6 pb-10">
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

                        return (
                            <div key={c.categoryId} className={`flex items-center gap-3 px-6 py-5 ${border}`}>
                                <div className="flex-1 min-w-0">
                                    <p className="truncate text-base font-semibold text-slate-900">{c.name}</p>
                                    <p className="text-xs text-slate-400">계획 {c.planCount}개</p>
                                </div>
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
