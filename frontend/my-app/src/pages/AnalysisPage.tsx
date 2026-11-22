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
import CompletionHeatmap from "../components/AnalysisPage/CompletionHeatmap";
import EstimateStats from "../components/AnalysisPage/EstimateStats";

// 통계 기간 - days가 없으면 전체
const PERIODS = [
    { label: "전체", days: undefined },
    { label: "최근 30일", days: 30 },
    { label: "최근 7일", days: 7 },
] as const;

type PeriodLabel = (typeof PERIODS)[number]["label"];

export default function AnalysisPage() {
    const [period, setPeriod] = useState<PeriodLabel>("전체");
    // 응답이 어느 기간 것인지 함께 저장 - 기간을 바꾸면 새 응답이 올 때까지 불러오는 중으로 표시
    const [loaded, setLoaded] = useState<{ period: PeriodLabel; stats: PlanStats } | null>(null);
    const [errorMessage, setErrorMessage] = useState("");

    useEffect(() => {
        let ignore = false;
        const days = PERIODS.find((p) => p.label === period)?.days;
        getPlanStats(days)
            .then((stats) => {
                if (ignore) return;
                setLoaded({ period, stats });
                setErrorMessage("");
            })
            .catch((error) => {
                if (ignore) return;
                console.error("통계를 불러오지 못했습니다:", error);
                setErrorMessage(getErrorMessage(error, "통계를 불러오지 못했습니다."));
            });
        return () => {
            ignore = true;
        };
    }, [period]);

    const stats = loaded?.period === period ? loaded.stats : null;

    return (
        <div className="space-y-8">
            {/* 기간 필터 - 통계 카드에만 적용, 잔디는 항상 최근 12주 */}
            <div className="grid grid-cols-3 rounded-xl bg-white p-1 text-sm font-semibold shadow-sm lg:ml-auto lg:w-96">
                {PERIODS.map((p) => (
                    <button
                        key={p.label}
                        onClick={() => setPeriod(p.label)}
                        className={`rounded-lg py-2 transition ${
                            period === p.label ? "bg-slate-900 text-white" : "text-gray-400 hover:text-gray-700"
                        }`}
                    >
                        {p.label}
                    </button>
                ))}
            </div>

            {errorMessage ? (
                <>
                    <CompletionHeatmap />
                    <p className="text-sm text-red-500 text-center">{errorMessage}</p>
                </>
            ) : !stats ? (
                <>
                    <CompletionHeatmap />
                    <p className="text-center text-black">통계를 불러오는 중...</p>
                </>
            ) : (
                <PeriodStats stats={stats} period={period} />
            )}
        </div>
    );
}

function PeriodStats({ stats, period }: { stats: PlanStats; period: PeriodLabel }) {
    return (
        <div className="space-y-8 lg:grid lg:grid-cols-2 lg:items-start lg:gap-8 lg:space-y-0">
            <Summary stats={stats} period={period} />

            <CompletionHeatmap />

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

                    <EstimateStats stat={stats.estimateStat} />

                    <PostponeStats
                        stat={stats.postponeStat}
                        onTimeCount={stats.recordCount - stats.postponeStat.postponedCount}
                    />
                </>
            )}
        </div>
    );
}

function Summary({ stats, period }: { stats: PlanStats; period: PeriodLabel }) {
    if (stats.recordCount === 0) {
        return (
            <section className="bg-white rounded-3xl p-6 shadow-sm text-center space-y-2 text-black">
                <h2 className="text-xl font-bold">
                    {period === "전체" ? "아직 분석할 기록이 없어요" : `${period} 동안의 기록이 없어요`}
                </h2>
                <p className="text-sm">
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
        <section className="bg-white rounded-3xl p-6 shadow-sm text-center text-black">
            <p className="text-sm font-semibold">{period === "전체" ? "전체 성공률" : `${period} 성공률`}</p>
            <p className="mt-1 text-5xl font-bold">
                {stats.successRate != null ? toPercent(stats.successRate) : "-"}
            </p>
            <p className="mt-3 text-sm">
                결과가 나온 계획 {stats.recordCount}건 중 계획한 날 완료한 비율이에요
            </p>
        </section>
    );
}
