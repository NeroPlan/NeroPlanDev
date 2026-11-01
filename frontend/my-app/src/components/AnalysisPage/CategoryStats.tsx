// components/AnalysisPage/CategoryStats.tsx
import type { CategoryStat } from "../../api/plan";
import { getMissingRateText, toPercent } from "../common/rateColor";
import StatCard from "./StatCard";

// aiService가 모델로 성공률을 예측하기 시작하는 카테고리 기록 수
const MIN_RECORDS_FOR_PREDICTION = 10;

interface CategoryStatsProps {
    categories: CategoryStat[]; // 기록 많은 순
}

// 마지막 글자 받침에 맞춰 은/는을 붙임 - 한글이 아니면 둘 다 표기
const withTopic = (word: string) => {
    const code = word.charCodeAt(word.length - 1) - 0xac00;
    if (code < 0 || code > 11171) return `${word}은(는)`;
    return `${word}${code % 28 === 0 ? "는" : "은"}`;
};

const getInsight = (categories: CategoryStat[]) => {
    const rated = categories.filter((c) => c.categoryName != null && c.successRate != null);
    if (rated.length < 2) return null;

    const best = rated.reduce((a, b) => ((b.successRate ?? 0) > (a.successRate ?? 0) ? b : a));
    const worst = rated.reduce((a, b) => ((b.successRate ?? 0) < (a.successRate ?? 0) ? b : a));
    if (best === worst) return null;
    return `${withTopic(best.categoryName ?? "")} 잘 지키고 있고, ${withTopic(worst.categoryName ?? "")} 자주 놓치고 있어요.`;
};

export default function CategoryStats({ categories }: CategoryStatsProps) {
    return (
        <StatCard title="카테고리별 성공률" insight={getInsight(categories)}>
            <ul className="space-y-5">
                {categories.map((c) => (
                    <li key={c.categoryName ?? "uncategorized"} className="space-y-2">
                        <div className="flex items-center justify-between">
                            <span className="text-lg font-semibold">{c.categoryName ?? "미분류"}</span>
                            {c.successRate != null ? (
                                <span className="text-lg font-bold text-blue-600">{toPercent(c.successRate)}</span>
                            ) : (
                                <span className="text-sm text-gray-400">{getMissingRateText(c.recordCount)}</span>
                            )}
                        </div>

                        <OutcomeBar stat={c} />

                        <div className="flex gap-3 text-xs text-gray-500">
                            <Legend color="bg-green-500" label="완료" count={c.doneCount} />
                            <Legend color="bg-yellow-400" label="미룸" count={c.postponedCount} />
                            <Legend color="bg-red-500" label="실패" count={c.failedCount} />
                        </div>

                        {c.categoryName != null && <PredictionProgress aiRecordCount={c.aiRecordCount} />}
                    </li>
                ))}
            </ul>
        </StatCard>
    );
}

// 완료 / 미룸 / 실패 비율을 한 줄에 쌓아서 표시
function OutcomeBar({ stat }: { stat: CategoryStat }) {
    const segments = [
        { color: "bg-green-500", count: stat.doneCount },
        { color: "bg-yellow-400", count: stat.postponedCount },
        { color: "bg-red-500", count: stat.failedCount },
    ];

    return (
        <div className="flex h-2 rounded-full bg-gray-100 overflow-hidden">
            {segments.map((s) =>
                s.count > 0 ? (
                    <div
                        key={s.color}
                        className={`h-full ${s.color}`}
                        style={{ width: `${(s.count / stat.recordCount) * 100}%` }}
                    />
                ) : null
            )}
        </div>
    );
}

function Legend({ color, label, count }: { color: string; label: string; count: number }) {
    return (
        <span className="flex items-center gap-1">
            <span className={`h-2 w-2 rounded-full ${color}`} />
            {label} {count}
        </span>
    );
}

function PredictionProgress({ aiRecordCount }: { aiRecordCount: number }) {
    if (aiRecordCount >= MIN_RECORDS_FOR_PREDICTION) {
        return <p className="text-xs font-semibold text-blue-600">AI가 성공률을 예측하고 있어요</p>;
    }

    return (
        <p className="text-xs text-gray-400">
            AI 예측까지 기록 {aiRecordCount}/{MIN_RECORDS_FOR_PREDICTION} · {MIN_RECORDS_FOR_PREDICTION - aiRecordCount}건 더 필요해요
        </p>
    );
}
