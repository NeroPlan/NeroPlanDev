// components/AnalysisPage/RateStats.tsx
import type { RateStat } from "../../api/plan";
import RateRow from "./RateRow";
import StatCard from "./StatCard";

// 구간 간 성공률 차이가 이보다 작으면 의미 있는 패턴으로 보지 않음
const MIN_RATE_GAP = 0.2;

interface RateStatsProps {
    title: string;
    stats: RateStat[];
    // 성공률이 가장 높은 구간과 가장 낮은 구간으로 요약 문장을 만듦
    describe: (best: RateStat, worst: RateStat) => string;
}

const getInsight = (stats: RateStat[], describe: RateStatsProps["describe"]) => {
    const rated = stats.filter((s) => s.successRate != null);
    if (rated.length < 2) return null;

    const best = rated.reduce((a, b) => ((b.successRate ?? 0) > (a.successRate ?? 0) ? b : a));
    const worst = rated.reduce((a, b) => ((b.successRate ?? 0) < (a.successRate ?? 0) ? b : a));
    if ((best.successRate ?? 0) - (worst.successRate ?? 0) < MIN_RATE_GAP) return null;
    return describe(best, worst);
};

export default function RateStats({ title, stats, describe }: RateStatsProps) {
    return (
        <StatCard title={title} insight={getInsight(stats, describe)}>
            <div className="space-y-4">
                {stats.map((s) => (
                    <RateRow key={s.label} label={s.label} recordCount={s.recordCount} rate={s.successRate} />
                ))}
            </div>
        </StatCard>
    );
}
