// components/common/PlanInput.tsx
import { useEffect, useState } from "react";
import { completePlan, createPlan, deletePlan, getPlans, postponePlan, updatePlan } from "../../api/plan"; // 실제 위치에 맞게 경로 수정
import type { Plan } from "../../api/plan";
import { getErrorMessage } from "../../api/api";

export default function PlanInput() {
    const [plan, setPlan] = useState("");
    const [minutes, setMinutes] = useState(""); // 예상 소요 시간(분) - AI 재배열에 필요해서 필수
    const [plans, setPlans] = useState<Plan[]>([]); // 서버에 저장된 계획 목록
    const [editingId, setEditingId] = useState<number | null>(null);
    const [editingContent, setEditingContent] = useState("");
    const [editingMinutes, setEditingMinutes] = useState("");
    const [errorMessage, setErrorMessage] = useState("");

    useEffect(() => {
        getPlans()
            .then(setPlans)
            .catch((error) => {
                console.error("계획 목록을 불러오지 못했습니다:", error);
                setErrorMessage(getErrorMessage(error, "계획 목록을 불러오지 못했습니다."));
            });
    }, []);

    // 1 이상의 정수만 허용, 아니면 null
    const parseMinutes = (value: string) => {
        const parsed = Number(value);
        return Number.isInteger(parsed) && parsed > 0 ? parsed : null;
    };

    const handleAddPlan = async () => {
        const estimatedMinutes = parseMinutes(minutes);
        if (!plan.trim()) {
            setErrorMessage("계획을 입력하세요.");
            return;
        }
        if (estimatedMinutes === null) {
            setErrorMessage("소요 시간을 1분 이상의 숫자로 입력하세요.");
            return;
        }

        try {
            const createdPlan = await createPlan(plan, estimatedMinutes);

            setPlans((prev) => [...prev, createdPlan]); // 서버가 준 결과를 목록에 추가
            setPlan(""); // 입력창 초기화
            setMinutes("");
            setErrorMessage("");
        } catch (error) {
            console.error("계획을 추가하지 못했습니다:", error);
            setErrorMessage(getErrorMessage(error, "계획을 추가하지 못했습니다."));
        }
    };

    const handleStartEdit = (p: Plan) => {
        setEditingId(p.planId);
        setEditingContent(p.content);
        setEditingMinutes(p.estimatedMinutes?.toString() ?? "");
    };

    const handleCancelEdit = () => {
        setEditingId(null);
        setEditingContent("");
        setEditingMinutes("");
    };

    const handleSaveEdit = async (planId: number) => {
        const estimatedMinutes = parseMinutes(editingMinutes);
        if (!editingContent.trim()) {
            setErrorMessage("계획을 입력하세요.");
            return;
        }
        if (estimatedMinutes === null) {
            setErrorMessage("소요 시간을 1분 이상의 숫자로 입력하세요.");
            return;
        }

        try {
            const updated = await updatePlan(planId, {
                content: editingContent,
                estimatedMinutes,
            });

            setPlans((prev) =>
                prev.map((p) => (p.planId === planId ? updated : p))
            );
            handleCancelEdit();
            setErrorMessage("");
        } catch (error) {
            console.error("계획을 수정하지 못했습니다:", error);
            setErrorMessage(getErrorMessage(error, "계획을 수정하지 못했습니다."));
        }
    };

    const handleToggleComplete = async (planId: number, completed: boolean) => {
        try {
            const updated = await completePlan(planId, completed);

            setPlans((prev) =>
                prev.map((p) => (p.planId === planId ? updated : p))
            );
            setErrorMessage("");
        } catch (error) {
            console.error("완료 상태를 바꾸지 못했습니다:", error);
            setErrorMessage(getErrorMessage(error, "완료 상태를 바꾸지 못했습니다."));
        }
    };

    // 다음 날로 연기 - 오늘 목록에서는 빠짐
    const handlePostponePlan = async (planId: number) => {
        try {
            await postponePlan(planId);

            setPlans((prev) => prev.filter((p) => p.planId !== planId));
            if (editingId === planId) handleCancelEdit();
            setErrorMessage("");
        } catch (error) {
            console.error("계획을 미루지 못했습니다:", error);
            setErrorMessage(getErrorMessage(error, "계획을 미루지 못했습니다."));
        }
    };

    const handleDeletePlan = async (planId: number) => {
        try {
            await deletePlan(planId);

            setPlans((prev) => prev.filter((p) => p.planId !== planId));
            if (editingId === planId) handleCancelEdit();
            setErrorMessage("");
        } catch (error) {
            console.error("계획을 삭제하지 못했습니다:", error);
            setErrorMessage(getErrorMessage(error, "계획을 삭제하지 못했습니다."));
        }
    };

    return (
        <div className="bg-white rounded-3xl p-6 shadow-sm">
            <h2 className="text-xl font-bold text-center mb-3">
                계획 입력 란
            </h2>

            <hr className="mb-4" />

           <div className="space-y-3">
                <div className="flex items-center gap-2">
                    <input
                        type="text"
                        value={plan}
                        onChange={(e) => setPlan(e.target.value)}
                        placeholder="계획을 입력하세요"
                        className="
                            flex-1
                            min-w-0
                            border
                            rounded-xl
                            px-3
                            py-2
                            outline-none
                        "
                    />
                    <input
                        type="number"
                        min={1}
                        step={1}
                        value={minutes}
                        onChange={(e) => setMinutes(e.target.value)}
                        placeholder="소요 시간"
                        className="
                            w-24
                            border
                            rounded-xl
                            px-3
                            py-2
                            outline-none
                        "
                    />
                    <span className="text-gray-500">분</span>
                </div>
                <button
                    onClick={handleAddPlan}
                    className="
                        w-full
                        border
                        border-dashed
                        rounded-xl
                        py-2
                        text-xl
                        font-bold
                    "
                >
                    +
                </button>

                {errorMessage && (
                    <p className="text-sm text-red-500">{errorMessage}</p>
                )}

                <ul className="list-disc pl-6 space-y-1 text-lg font-semibold">
                    {plans.map((p) => (
                        <li key={p.planId} className="flex items-center gap-2">
                            {editingId === p.planId ? (
                                <>
                                    <input
                                        type="text"
                                        value={editingContent}
                                        onChange={(e) => setEditingContent(e.target.value)}
                                        className="border rounded-lg px-2 py-1 text-base font-normal flex-1 min-w-0"
                                    />
                                    <input
                                        type="number"
                                        min={1}
                                        step={1}
                                        value={editingMinutes}
                                        onChange={(e) => setEditingMinutes(e.target.value)}
                                        className="border rounded-lg px-2 py-1 text-base font-normal w-20"
                                    />
                                    <span className="text-sm text-gray-500 font-normal">분</span>
                                    <button
                                        onClick={() => handleSaveEdit(p.planId)}
                                        className="text-sm text-blue-600"
                                    >
                                        저장
                                    </button>
                                    <button
                                        onClick={handleCancelEdit}
                                        className="text-sm text-gray-500"
                                    >
                                        취소
                                    </button>
                                </>
                            ) : (
                                <>
                                    <label className="flex items-center gap-1 text-sm font-normal text-gray-600">
                                        <input
                                            type="checkbox"
                                            checked={p.status === "DONE"}
                                            onChange={(e) => handleToggleComplete(p.planId, e.target.checked)}
                                        />
                                        완료
                                    </label>
                                    <span
                                        className={`flex-1 ${p.status === "DONE" ? "line-through text-gray-400" : ""}`}
                                    >
                                        {p.content}
                                    </span>
                                    {p.estimatedMinutes != null && (
                                        <span className="text-sm text-gray-500 font-normal">
                                            {p.estimatedMinutes}분
                                        </span>
                                    )}
                                    {/* 완료된 계획은 서버에서 연기를 막으므로 비활성화 */}
                                    <label
                                        className={`flex items-center gap-1 text-sm font-normal ${p.status === "DONE" ? "text-gray-300" : "text-gray-600"}`}
                                    >
                                        <input
                                            type="checkbox"
                                            checked={false}
                                            disabled={p.status === "DONE"}
                                            onChange={() => handlePostponePlan(p.planId)}
                                        />
                                        미룸
                                    </label>
                                    <button
                                        onClick={() => handleStartEdit(p)}
                                        className="text-sm text-gray-500"
                                    >
                                        수정
                                    </button>
                                    <button
                                        onClick={() => handleDeletePlan(p.planId)}
                                        className="text-sm text-red-500"
                                    >
                                        삭제
                                    </button>
                                </>
                            )}
                        </li>
                    ))}
                </ul>
            </div>
        </div>
    );
}
