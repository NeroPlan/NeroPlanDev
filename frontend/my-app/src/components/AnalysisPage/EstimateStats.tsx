// components/AnalysisPage/EstimateStats.tsx
// 예상 vs 실제 - 집중 타이머로 기록한 시간이 예상 소요시간과 얼마나 다른지 (계획 오류 확인)
import { Link } from "react-router-dom";
import type { EstimateStat } from "../../api/plan";
import { MIN_RECORDS_FOR_RATE, toPercent } from "../common/rateColor";
import StatCard from "./StatCard";

const getInsight = (stat: EstimateStat) => {
    if (stat.averageRatio == null) return null;
    if (stat.averageRatio >= 1.1) {
        return `예상보다 평균 ${stat.averageRatio.toFixed(1)}배 오래 걸려요. 계획할 때 시간을 넉넉히 잡아 보세요.`;
    }
    if (stat.averageRatio <= 0.9) {
        return `예상보다 평균 ${Math.round((1 - stat.averageRatio) * 100)}% 빨리 끝내요.`;
    }
    return "예상한 시간과 거의 비슷하게 끝내고 있어요.";
};

export default function EstimateStats({ stat }: { stat: EstimateStat }) {
    if (stat.sampleCount === 0) {
        return (
            <StatCard title="예상 vs 실제">
                <p className="text-sm text-center">
                    집중 타이머로 계획을 끝내면 예상한 시간과 실제로 걸린 시간을 비교해 드려요.
                </p>
                <Link
                    to="/focus"
                    className="mt-4 block w-full rounded-2xl bg-slate-900 py-3 text-center font-bold text-white transition hover:bg-slate-700"
                >
                    집중 타이머 열기
                </Link>
            </StatCard>
        );
    }

    const estimated = stat.averageEstimatedMinutes ?? 0;
    const actual = stat.averageActualMinutes ?? 0;
    const max = Math.max(estimated, actual, 1);

    return (
        <StatCard title="예상 vs 실제" insight={getInsight(stat)}>
            <div className="space-y-4">
                <CompareBar label="평균 예상 시간" minutes={estimated} width={estimated / max} className="bg-blue-200" />
                <CompareBar label="평균 실제 시간" minutes={actual} width={actual / max} className="bg-blue-500" />
            </div>
            <p className="mt-4 text-xs">
                집중 기록이 있는 완료 계획 {stat.sampleCount}건 기준
                {stat.overRate != null
                    ? ` · 그중 ${toPercent(stat.overRate)}가 예상보다 오래 걸렸어요.`
                    : ` · ${MIN_RECORDS_FOR_RATE}건부터 비율을 보여 드려요.`}
            </p>
        </StatCard>
    );
}

function CompareBar({ label, minutes, width, className }: { label: string; minutes: number; width: number; className: string }) {
    return (
        <div className="space-y-1">
            <div className="flex justify-between text-sm">
                <span>{label}</span>
                <span className="font-semibold">{minutes}분</span>
            </div>
            <div className="h-2 rounded-full bg-gray-100 overflow-hidden">
                <div className={`h-full rounded-full ${className}`} style={{ width: `${width * 100}%` }} />
            </div>
        </div>
    );
}
