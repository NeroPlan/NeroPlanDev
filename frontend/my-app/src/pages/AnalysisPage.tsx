// pages/AnalysisPage.tsx

import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { getErrorMessage } from "../api/api";
import { getPlanStats } from "../api/plan";
import type { PlanStats } from "../api/plan";
import { toPercent } from "../components/common/rateColor";
import CategoryStats from "../components/AnalysisPage/CategoryStats";
import RateStats from "../components/AnalysisPage/RateStats";
import PostponeStats from "../components/AnalysisPage/PostponeStats";

export default function AnalysisPage() {
    const [stats, setStats] = useState<PlanStats | null>(null);
    const [errorMessage, setErrorMessage] = useState("");

    useEffect(() => {
        getPlanStats()
            .then(setStats)
            .catch((error) => {
                console.error("통계를 불러오지 못했습니다:", error);
                setErrorMessage(getErrorMessage(error, "통계를 불러오지 못했습니다."));
            });
    }, []);

    if (errorMessage) {
        return <p className="text-sm text-red-500 text-center">{errorMessage}</p>;
    }

    if (!stats) {
        return <p className="text-center text-gray-500">통계를 불러오는 중...</p>;
    }

    return (
        <div className="space-y-8 pb-24">
            <Summary stats={stats} />

            {stats.recordCount > 0 && (
                <>
                    <CategoryStats categories={stats.categories} />

                    <RateStats
                        title="소요시간별 성공률"
                        stats={stats.durationStats}
                        describe={(best, worst) =>
                            `${worst.label} 계획은 성공률이 ${toPercent(worst.successRate ?? 0)}로 가장 낮아요. ${best.label} 계획은 ${toPercent(best.successRate ?? 0)}예요.`
                        }
                    />

                    <RateStats
                        title="하루 계획량별 성공률"
                        stats={stats.dailyLoadStats}
                        describe={(best, worst) =>
                            `하루에 ${best.label} 세운 날 성공률이 ${toPercent(best.successRate ?? 0)}로 가장 높아요. ${worst.label} 세운 날은 ${toPercent(worst.successRate ?? 0)}예요.`
                        }
                    />

                    <PostponeStats
                        stat={stats.postponeStat}
                        onTimeCount={stats.recordCount - stats.postponeStat.postponedCount}
                    />
                </>
            )}
        </div>
    );
}

function Summary({ stats }: { stats: PlanStats }) {
    if (stats.recordCount === 0) {
        return (
            <section className="bg-white rounded-3xl p-6 shadow-sm text-center space-y-2">
                <h2 className="text-xl font-bold">아직 분석할 기록이 없어요</h2>
                <p className="text-sm text-gray-500">
                    계획을 완료하거나 하루가 지나면 결과가 쌓이고, 여기에서 내 계획 패턴을 볼 수 있어요.
                </p>
                <Link
                    to="/plan"
                    className="mt-3 block w-full bg-blue-600 text-white font-bold py-3 rounded-2xl shadow-sm hover:bg-blue-700 transition"
                >
                    계획 세우러 가기
                </Link>
            </section>
        );
    }

    return (
        <section className="bg-white rounded-3xl p-6 shadow-sm text-center">
            <p className="text-sm font-semibold text-gray-500">전체 성공률</p>
            <p className="mt-1 text-5xl font-bold text-blue-600">
                {stats.successRate != null ? toPercent(stats.successRate) : "-"}
            </p>
            <p className="mt-3 text-sm text-gray-500">
                결과가 나온 계획 {stats.recordCount}건 중 계획한 날 완료한 비율이에요
            </p>
        </section>
    );
}
