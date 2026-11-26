// components/PlanPage/analysisCache.ts
// 분석 결과는 PlanPage state에만 있어서 다른 페이지에 다녀오면 사라짐 → 탭 세션에 오늘 날짜와 함께 보관
// 날짜가 바뀌면 버리고, 로그아웃하면 지움(같은 탭에서 다른 계정으로 로그인해도 보이지 않도록)
import type { Plan } from "../../api/plan";

const ANALYSIS_KEY = "neroplan.todayAnalysis";

interface CachedAnalysis {
    date: string; // YYYY-MM-DD
    plans: Plan[];
}

// 브라우저 저장소는 막혀 있을 수 있으므로 실패해도 분석은 동작
export const loadAnalysis = (date: string): Plan[] | null => {
    try {
        const raw = sessionStorage.getItem(ANALYSIS_KEY);
        if (!raw) return null;
        const cached = JSON.parse(raw) as CachedAnalysis;
        return cached.date === date && Array.isArray(cached.plans) ? cached.plans : null;
    } catch {
        return null;
    }
};

export const saveAnalysis = (date: string, plans: Plan[]) => {
    try {
        sessionStorage.setItem(ANALYSIS_KEY, JSON.stringify({ date, plans } satisfies CachedAnalysis));
    } catch {
        // 저장하지 못하면 이 페이지에 있는 동안만 유지
    }
};

export const clearAnalysis = () => {
    try {
        sessionStorage.removeItem(ANALYSIS_KEY);
    } catch {
        // 무시
    }
};
