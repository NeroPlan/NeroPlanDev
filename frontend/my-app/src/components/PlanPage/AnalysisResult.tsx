// components/PlanPage/AnalysisResult.tsx
import type { Plan } from "../../api/plan";

interface AnalysisResultProps {
    plans: Plan[]; // AI 추천 순서로 정렬된 미완료 계획
}

export default function AnalysisResult({
    plans,
}: AnalysisResultProps) {
    return (
        <div className="bg-white rounded-3xl p-6 shadow-sm">
            <h2 className="text-xl font-bold text-center mb-3">
                추천 수행 순서
            </h2>

            <hr className="mb-4" />

            {plans.length === 0 ? (
                <p className="text-center text-gray-500">
                    아직 분석된 계획이 없습니다.
                </p>
            ) : (
                <ol className="space-y-4">
                    {plans.map((p) => (
                        <li key={p.planId} className="flex gap-3">
                            <span className="text-lg font-bold text-blue-600">
                                {p.recommendedOrder}
                            </span>
                            <div className="flex-1 space-y-1">
                                <div className="flex items-center gap-2 text-lg font-semibold">
                                    <span className="flex-1">{p.content}</span>
                                    {p.estimatedMinutes != null && (
                                        <span className="text-sm text-gray-500 font-normal">
                                            {p.estimatedMinutes}분
                                        </span>
                                    )}
                                </div>
                                {p.successRate != null && (
                                    <p className="text-sm text-gray-600">
                                        예상 성공률 {Math.round(p.successRate * 100)}%
                                    </p>
                                )}
                                {p.isBiased && (
                                    <p className="text-sm text-red-500">
                                        ⚠ {p.biasedReason ?? "미루거나 실패할 위험이 높은 계획입니다."}
                                    </p>
                                )}
                            </div>
                        </li>
                    ))}
                </ol>
            )}
        </div>
    );
}
