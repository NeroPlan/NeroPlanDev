// components/common/PlanInput.tsx
import { useState } from "react";
import { Link } from "react-router-dom";
import { completePlan, createPlan, deletePlan, postponePlan, updatePlan } from "../../api/plan";
import type { Plan } from "../../api/plan";
import { getErrorMessage } from "../../api/api";

interface PlanInputProps {
    title: string;
    planDate: string; // 이 카드가 보여주는 날짜(YYYY-MM-DD) - 새 계획도 이 날짜로 생성
    plans: Plan[]; // 그 날짜의 계획 - 목록은 PlanPage가 달력 기간 단위로 불러옴
    readOnly: boolean; // 지난 날짜는 결과만 조회
    canFocus?: boolean; // 오늘 계획이면 집중 타이머로 바로 이동 가능
    onUpsert: (plan: Plan) => void; // 추가·수정·완료·연기 결과 반영 (연기는 planDate가 바뀐 계획)
    onRemove: (planId: number) => void;
}

export default function PlanInput({
    title,
    planDate,
    plans,
    readOnly,
    canFocus = false,
    onUpsert,
    onRemove,
}: PlanInputProps) {
    const [plan, setPlan] = useState("");
    const [minutes, setMinutes] = useState(""); // 예상 소요 시간(분) - AI 재배열에 필요해서 필수
    const [editingId, setEditingId] = useState<number | null>(null);
    const [editingContent, setEditingContent] = useState("");
    const [editingMinutes, setEditingMinutes] = useState("");
    const [errorMessage, setErrorMessage] = useState("");

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
            const createdPlan = await createPlan(plan, estimatedMinutes, planDate);

            onUpsert(createdPlan); // 서버가 준 결과를 목록에 추가
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

            onUpsert(updated);
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

            onUpsert(updated);
            setErrorMessage("");
        } catch (error) {
            console.error("완료 상태를 바꾸지 못했습니다:", error);
            setErrorMessage(getErrorMessage(error, "완료 상태를 바꾸지 못했습니다."));
        }
    };

    // 다음 날로 연기 - 이 날짜 목록에서 빠지고 달력의 다음 날 칸으로 옮겨감
    const handlePostponePlan = async (planId: number) => {
        try {
            const postponed = await postponePlan(planId);

            onUpsert(postponed);
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

            onRemove(planId);
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
                {title}
            </h2>

            <hr className="mb-4" />

           <div className="space-y-3">
                {!readOnly && (
                <>
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
                </>
                )}

                {errorMessage && (
                    <p className="text-sm text-red-500">{errorMessage}</p>
                )}

                {plans.length === 0 && (
                    <p className="text-center text-gray-500">
                        {readOnly ? "이 날은 계획이 없었어요." : "아직 계획이 없어요."}
                    </p>
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
                                            disabled={readOnly}
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
                                    {/* 집중 타이머로 기록한 실제 시간 */}
                                    {p.actualMinutes != null && (
                                        <span className="text-sm text-gray-400 font-normal" title="집중 타이머로 기록한 시간">
                                            (실제 {p.actualMinutes}분)
                                        </span>
                                    )}
                                    {readOnly ? (
                                        // 지난 날짜는 결과만 표시 - 끝내 안 한 계획은 미완료
                                        p.status !== "DONE" && (
                                            <span className="text-sm text-gray-500 font-normal">미완료</span>
                                        )
                                    ) : (
                                    <>
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
                                    {canFocus && p.status === "TODO" && (
                                        <Link
                                            to={`/focus?planId=${p.planId}`}
                                            className="text-sm text-gray-500"
                                        >
                                            집중
                                        </Link>
                                    )}
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
                                </>
                            )}
                        </li>
                    ))}
                </ul>
            </div>
        </div>
    );
}
