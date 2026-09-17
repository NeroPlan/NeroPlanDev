// components/common/PlanInput.tsx
import { useEffect, useState } from "react";
import { createPlan, getPlans, updatePlan } from "../../api/plan"; // 실제 위치에 맞게 경로 수정
import type { Plan } from "../../api/plan";

export default function PlanInput() {
    const [plan, setPlan] = useState("");
    const [plans, setPlans] = useState<Plan[]>([]); // 서버에 저장된 계획 목록
    const [editingId, setEditingId] = useState<number | null>(null);
    const [editingContent, setEditingContent] = useState("");

    useEffect(() => {
        getPlans()
            .then(setPlans)
            .catch((error) => {
                console.error("계획 목록을 불러오지 못했습니다:", error);
            });
    }, []);

    const handleAddPlan = async () => {
        if (!plan.trim()) return; // 빈 값 제출 방지

        const createdPlan = await createPlan(plan);

        setPlans((prev) => [...prev, createdPlan]); // 서버가 준 결과를 목록에 추가
        setPlan(""); // 입력창 초기화
    };

    const handleStartEdit = (p: Plan) => {
        setEditingId(p.planId);
        setEditingContent(p.content);
    };

    const handleCancelEdit = () => {
        setEditingId(null);
        setEditingContent("");
    };

    const handleSaveEdit = async (planId: number) => {
        if (!editingContent.trim()) return;

        const updated = await updatePlan(planId, { content: editingContent });

        setPlans((prev) =>
            prev.map((p) => (p.planId === planId ? updated : p))
        );
        setEditingId(null);
        setEditingContent("");
    };

    return (
        <div className="bg-white rounded-3xl p-6 shadow-sm">
            <h2 className="text-xl font-bold text-center mb-3">
                계획 입력 란
            </h2>

            <hr className="mb-4" />

           <div className="space-y-3">
                <input
                    type="text"
                    value={plan}
                    onChange={(e) => setPlan(e.target.value)}
                    placeholder="계획을 입력하세요"
                    className="
                        w-full
                        border
                        rounded-xl
                        px-3
                        py-2
                        outline-none
                    "
                />
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

                <ul className="list-disc pl-6 space-y-1 text-lg font-semibold">
                    {plans.map((p) => (
                        <li key={p.planId} className="flex items-center gap-2">
                            {editingId === p.planId ? (
                                <>
                                    <input
                                        type="text"
                                        value={editingContent}
                                        onChange={(e) => setEditingContent(e.target.value)}
                                        className="border rounded-lg px-2 py-1 text-base font-normal flex-1"
                                    />
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
                                    <span className="flex-1">{p.content}</span>
                                    <button
                                        onClick={() => handleStartEdit(p)}
                                        className="text-sm text-gray-500"
                                    >
                                        수정
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
