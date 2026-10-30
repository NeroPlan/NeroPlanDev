// pages/PlanPage.tsx

import { useLayoutEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import PlanInput from "../components/common/PlanInput";
import AnalyzeButton from "../components/PlanPage/button";
import AnalysisResult from "../components/PlanPage/AnalysisResult";
import { saveAccessToken } from "../api/auth";
import { getErrorMessage } from "../api/api";
import { analyzePlans } from "../api/plan";
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

    // 서버가 aiService로 오늘 계획을 다시 분석하고 추천 순서대로 돌려줌
    const handleAnalyze = async () => {
        setAnalyzing(true);
        setAnalyzeError("");

        try {
            const plans = await analyzePlans();

            setAnalyzedPlans(plans.filter((p) => p.status === "TODO"));
        } catch (error) {
            console.error("계획을 분석하지 못했습니다:", error);
            setAnalyzeError(getErrorMessage(error, "계획을 분석하지 못했습니다."));
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