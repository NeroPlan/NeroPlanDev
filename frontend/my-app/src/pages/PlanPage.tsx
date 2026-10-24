// pages/PlanPage.tsx

import { useLayoutEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import PlanInput from "../components/common/PlanInput";
import AnalyzeButton from "../components/PlanPage/button";
import AnalysisResult from "../components/PlanPage/AnalysisResult";
import { saveAccessToken } from "../api/auth";
import { getErrorMessage } from "../api/api";
import { getPlans } from "../api/plan";
import type { Plan } from "../api/plan";

export default function PlanPage() {
    const location = useLocation();
    const navigate = useNavigate();
    const [analyzedPlans, setAnalyzedPlans] = useState<Plan[] | null>(null); // 분석하기 전에는 null
    const [analyzing, setAnalyzing] = useState(false);
    const [analyzeError, setAnalyzeError] = useState("");

    // PlanInput의 계획 조회(useEffect)보다 먼저 토큰을 저장해야 하므로 useLayoutEffect 사용
    useLayoutEffect(() => {
        const params = new URLSearchParams(location.search);
        const accessToken = params.get("accessToken");

        if (accessToken) {
            saveAccessToken(accessToken);

            params.delete("accessToken");
            const nextSearch = params.toString();
            const nextPath = `${location.pathname}${nextSearch ? `?${nextSearch}` : ""}`;

            navigate(nextPath, { replace: true });
        }
    }, [location.pathname, location.search, navigate]);

    // AI 재배열은 계획이 바뀔 때마다 서버에서 미리 계산되므로, 오늘 계획을 다시 불러와 결과만 보여줌
    const handleAnalyze = async () => {
        setAnalyzing(true);
        setAnalyzeError("");

        try {
            const plans = await getPlans();

            setAnalyzedPlans(
                plans
                    .filter((p) => p.status === "TODO" && p.recommendedOrder != null)
                    .sort((a, b) => (a.recommendedOrder ?? 0) - (b.recommendedOrder ?? 0))
            );
        } catch (error) {
            console.error("분석 결과를 불러오지 못했습니다:", error);
            setAnalyzeError(getErrorMessage(error, "분석 결과를 불러오지 못했습니다."));
        } finally {
            setAnalyzing(false);
        }
    };

    return (
            <div className="space-y-8">

                <PlanInput />
                
                <AnalyzeButton onClick={handleAnalyze} loading={analyzing} />

                {analyzeError && (
                    <p className="text-sm text-red-500 text-center">{analyzeError}</p>
                )}

                {analyzedPlans && <AnalysisResult plans={analyzedPlans} />}
            </div>
    );
}