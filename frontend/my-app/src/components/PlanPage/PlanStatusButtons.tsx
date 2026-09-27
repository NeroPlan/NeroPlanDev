// components/PlanPage/PlanStatusButtons.tsx
import type { PlanStatus } from "../../api/plan";

interface PlanStatusButtonsProps {
    status: PlanStatus;
    onChange: (status: PlanStatus) => void;
}

export default function PlanStatusButtons({
    status,
    onChange,
}: PlanStatusButtonsProps) {
    // 이미 선택된 상태를 다시 누르면 기본값(FAILED)으로 되돌림
    const toggle = (target: PlanStatus) => {
        onChange(status === target ? "FAILED" : target);
    };

    return (
        <div className="flex gap-1 shrink-0">
            <button
                onClick={() => toggle("POSTPONED")}
                className={`
                    text-sm
                    font-semibold
                    px-2
                    py-1
                    rounded-lg
                    border
                    transition
                    ${status === "POSTPONED"
                        ? "bg-yellow-400 border-yellow-400 text-white"
                        : "border-yellow-400 text-yellow-500"}
                `}
            >
                연기
            </button>
            <button
                onClick={() => toggle("COMPLETED")}
                className={`
                    text-sm
                    font-semibold
                    px-2
                    py-1
                    rounded-lg
                    border
                    transition
                    ${status === "COMPLETED"
                        ? "bg-blue-600 border-blue-600 text-white"
                        : "border-blue-600 text-blue-600"}
                `}
            >
                완료
            </button>
        </div>
    );
}
