// components/common/PlanInput.tsx
import { useState } from "react";
import { createPlan } from "../../api/plan"; // 실제 위치에 맞게 경로 수정

export default function PlanInput() {
    const [plan, setPlan] = useState("");

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
                    onClick={() => {
                        // 여기에 계획을 제출하는 로직을 추가하세요.
                        console.log("제출된 계획:", plan);
                        createPlan(plan);
                    }}
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
            </div>
        </div>
    );
}