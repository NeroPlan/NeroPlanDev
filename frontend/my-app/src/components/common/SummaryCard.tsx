// components/common/SummaryCard.tsx
// 마이페이지 요약 - 연속 달성, 이번 주 달성, 총 완료
import type { PlanSummary } from "../../api/plan";

interface SummaryCardProps {
    summary: PlanSummary | null; // 불러오는 중이면 null
    errorMessage?: string;
}

export default function SummaryCard({ summary, errorMessage }: SummaryCardProps) {
    if (errorMessage) {
        return <p className="text-center text-sm text-red-500">{errorMessage}</p>;
    }

    const weekRate =
        summary && summary.weekTotalCount > 0
            ? Math.round((summary.weekDoneCount / summary.weekTotalCount) * 100)
            : null;

    const items = [
        {
            label: "연속 달성",
            value: summary ? `${summary.streakDays}일` : "-",
            sub: summary && summary.streakDays > 0 ? "계속 이어가요" : "오늘 하나 끝내 볼까요",
        },
        {
            label: "이번 주",
            value: summary ? `${summary.weekDoneCount}/${summary.weekTotalCount}` : "-",
            sub: weekRate != null ? `${weekRate}% 완료` : "계획 없음",
        },
        {
            label: "총 완료",
            value: summary ? `${summary.totalDoneCount}개` : "-",
            sub: "지금까지",
        },
    ];

    return (
        <section className="grid grid-cols-3 overflow-hidden rounded-[28px] bg-white shadow-sm">
            {items.map((item, i) => (
                <div
                    key={item.label}
                    className={`px-3 py-5 text-center ${i < items.length - 1 ? "border-r border-slate-100" : ""}`}
                >
                    <p className="text-xs font-semibold text-slate-500">{item.label}</p>
                    <p className={`mt-1 text-2xl font-bold text-slate-900 ${summary ? "" : "text-slate-300"}`}>
                        {item.value}
                    </p>
                    <p className="mt-1 text-[11px] text-slate-400">{item.sub}</p>
                </div>
            ))}
        </section>
    );
}
