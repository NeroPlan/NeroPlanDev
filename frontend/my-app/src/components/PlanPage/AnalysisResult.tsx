// components/PlanPage/AnalysisResult.tsx
import type { Plan } from "../../api/plan";
import { getRateColor } from "../common/rateColor";

// aiService가 모델로 판정하기 시작하는 카테고리 기록 수
const MIN_RECORDS_FOR_PREDICTION = 10;

interface AnalysisResultProps {
    plans: Plan[]; // 오늘의 미완료 계획 - 서버가 AI 추천 순서로 정렬해서 줌
}

// aiService는 카테고리와 소요시간이 모두 있어야 분석할 수 있음
const getExcludedReason = (plan: Plan) => {
    if (plan.estimatedMinutes == null) return "소요시간을 입력하면 분석할 수 있어요";
    if (plan.categoryName == null) return "카테고리 분류에 실패했어요";
    return "분석 결과를 받지 못했어요";
};

// 판정 상태별 성공률 표시 - 모델 판정은 예측값, 기록 수집 중은 지금까지의 실제 완료율
const getRateInfo = (plan: Plan): { label: string; rate: number | null } => {
    if (plan.analysisStatus === "PREDICTED") {
        return { label: "예상 성공률", rate: plan.successRate ?? null };
    }
    if (plan.analysisStatus === "COLLECTING") {
        return {
            label: `지금까지 완료율 (기록 ${plan.recordCount ?? 0}/${MIN_RECORDS_FOR_PREDICTION})`,
            rate: plan.completionRate ?? null,
        };
    }
    return { label: "성공률 · 기록 없음", rate: null };
};

export default function AnalysisResult({
    plans: allPlans,
}: AnalysisResultProps) {
    const plans = allPlans.filter((p) => p.recommendedOrder != null);
    const excludedPlans = allPlans.filter((p) => p.recommendedOrder == null);

    return (
        <div className="bg-white rounded-3xl p-6 shadow-sm">
            <h2 className="text-xl font-bold text-center mb-3">
                추천 수행 순서
            </h2>

            <hr className="mb-4" />

            {plans.length === 0 ? (
                <p className="text-center text-gray-500">
                    분석할 수 있는 계획이 없습니다.
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
                                <RateBar plan={p} />
                                {p.priorityReason && (
                                    <p className={`text-sm ${p.isBiased ? "text-red-500" : "text-gray-500"}`}>
                                        {p.isBiased ? "⚠ " : "근거 · "}{p.priorityReason}
                                    </p>
                                )}
                            </div>
                        </li>
                    ))}
                </ol>
            )}

            {excludedPlans.length > 0 && (
                <div className="mt-6 space-y-2">
                    <p className="text-sm font-semibold text-gray-500">분석에서 제외된 계획</p>
                    <ul className="space-y-1">
                        {excludedPlans.map((p) => (
                            <li key={p.planId} className="text-sm text-gray-400">
                                {p.content} · {getExcludedReason(p)}
                            </li>
                        ))}
                    </ul>
                </div>
            )}
        </div>
    );
}

function RateBar({ plan }: { plan: Plan }) {
    const { label, rate } = getRateInfo(plan);

    return (
        <div className="space-y-1">
            <div className="flex justify-between text-sm text-gray-600">
                <span>{label}</span>
                {rate != null && <span className="font-semibold">{Math.round(rate * 100)}%</span>}
            </div>
            <div className="h-2 rounded-full bg-gray-100 overflow-hidden">
                {rate != null && (
                    <div
                        className={`h-full rounded-full ${getRateColor(rate)}`}
                        style={{ width: `${Math.round(rate * 100)}%` }}
                    />
                )}
            </div>
        </div>
    );
}
