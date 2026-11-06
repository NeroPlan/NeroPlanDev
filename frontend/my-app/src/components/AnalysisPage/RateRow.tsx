// components/AnalysisPage/RateRow.tsx
import { getMissingRateText, toPercent } from "../common/rateColor";

interface RateRowProps {
    label: string;
    recordCount: number;
    rate: number | null; // 기록이 부족하면 null
}

export default function RateRow({ label, recordCount, rate }: RateRowProps) {
    return (
        <div className="space-y-1">
            <div className="flex justify-between text-sm">
                <span>
                    {label}
                    <span className="ml-1">· {recordCount}건</span>
                </span>
                {rate != null ? (
                    <span className="font-semibold">{toPercent(rate)}</span>
                ) : (
                    <span>{getMissingRateText(recordCount)}</span>
                )}
            </div>
            <div className="h-2 rounded-full bg-gray-100 overflow-hidden">
                {rate != null && (
                    <div
                        className="h-full rounded-full bg-blue-600"
                        style={{ width: toPercent(rate) }}
                    />
                )}
            </div>
        </div>
    );
}
