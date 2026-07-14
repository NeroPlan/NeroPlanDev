// pages/PlanPage.tsx

import { useEffect } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import PlanCard from "../components/common/PlanCard";
import PlanInput from "../components/common/PlanInput";
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

                <PlanCard title="출근 전 계획" plans={[""]} />
                <PlanCard title="출근 후 오전 계획" plans={[""]} />
                <PlanCard title="출근 후 오후 계획" plans={[""]} />
                <PlanCard title="퇴근 후 계획" plans={[""]} />

                <PlanInput />
            </div>
    );
}