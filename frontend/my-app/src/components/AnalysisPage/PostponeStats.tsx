// components/AnalysisPage/PostponeStats.tsx
import type { PlanStats } from "../../api/plan";
import { toPercent } from "../common/rateColor";
import RateRow from "./RateRow";
import StatCard from "./StatCard";

interface PostponeStatsProps {
    stat: PlanStats["postponeStat"];
    onTimeCount: number; // 미루지 않은 계획 수
}

const getInsight = ({ postponedCompletionRate, onTimeCompletionRate }: PlanStats["postponeStat"]) => {
    if (postponedCompletionRate == null) return null;
    if (onTimeCompletionRate == null) {
        return `미룬 계획은 결국 ${toPercent(postponedCompletionRate)}만 끝냈어요.`;
    }
    return `미룬 계획은 결국 ${toPercent(postponedCompletionRate)}만 끝냈어요. 미루지 않은 계획은 ${toPercent(onTimeCompletionRate)}를 끝냈어요.`;
};

export default function PostponeStats({ stat, onTimeCount }: PostponeStatsProps) {
    return (
        <StatCard title="미룸 패턴" insight={getInsight(stat)}>
            <div className="space-y-4">
                <RateRow label="미루지 않은 계획" recordCount={onTimeCount} rate={stat.onTimeCompletionRate} />
                <RateRow label="한 번 이상 미룬 계획" recordCount={stat.postponedCount} rate={stat.postponedCompletionRate} />
            </div>
            <p className="mt-4 text-xs text-gray-400">미룬 뒤에라도 완료한 비율이에요.</p>
        </StatCard>
    );
}
