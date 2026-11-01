// components/AnalysisPage/StatCard.tsx
import type { ReactNode } from "react";

interface StatCardProps {
    title: string;
    insight?: string | null; // 카드 내용을 한 문장으로 요약 - 기록이 부족하면 생략
    children: ReactNode;
}

export default function StatCard({ title, insight, children }: StatCardProps) {
    return (
        <section className="bg-white rounded-3xl p-6 shadow-sm">
            <h2 className="text-xl font-bold text-center mb-3">{title}</h2>

            <hr className="mb-4" />

            {insight && (
                <p className="mb-4 rounded-2xl bg-blue-50 px-4 py-3 text-sm font-semibold text-blue-700">
                    {insight}
                </p>
            )}

            {children}
        </section>
    );
}
