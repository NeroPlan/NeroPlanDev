// components/common/PlanInput.tsx

import { useState } from "react";
import { createPlan } from "../../api/plan";
import AnalyzeButton from "../PlanPage/button";

export default function PlanInput() {
    const [plan, setPlan] = useState("");
    const [submittedPlans, setSubmittedPlans] = useState<string[]>([]);

    const handleAnalyze = async () => {
        const trimmedPlan = plan.trim();

        if (!trimmedPlan) return;

        setSubmittedPlans((prev) => [...prev, trimmedPlan]);
        setPlan("");

        await createPlan(trimmedPlan);
    };

    return (
        <div className="bg-white rounded-3xl p-6 shadow-sm">
            <h2 className="text-xl font-bold text-center mb-3">
                계획 입력 란
            </h2>

            <hr className="mb-4" />

            <div className="space-y-3">
                {submittedPlans.map((submittedPlan, index) => (
                    <div
                        key={`${submittedPlan}-${index}`}
                        className="
                            w-full
                            border
                            rounded-xl
                            px-3
                            py-2
                            bg-gray-100
                            text-gray-700
                        "
                    >
                        {submittedPlan}
                    </div>
                ))}

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

                <AnalyzeButton onClick={handleAnalyze} />
            </div>
        </div>
    );
}