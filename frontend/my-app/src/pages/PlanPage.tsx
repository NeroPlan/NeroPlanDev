// pages/PlanPage.tsx

import { useEffect } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import PlanInput from "../components/common/PlanInput";
import AnalyzeButton from "../components/PlanPage/button";
import { saveAccessToken } from "../api/auth";

export default function PlanPage() {
    const location = useLocation();
    const navigate = useNavigate();

    useEffect(() => {
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

    return (
            <div className="space-y-8">

                <PlanInput />
                
                <AnalyzeButton />
            </div>
    );
}