// components/AnalysisPage/RateRow.tsx
import { getMissingRateText, getRateColor, toPercent } from "../common/rateColor";

interface RateRowProps {
    label: string;
    recordCount: number;
    rate: number | null; // 기록이 부족하면 null
}

export default function RateRow({ label, recordCount, rate }: RateRowProps) {
    return (
        <div className="space-y-1">
            <div className="flex justify-between text-sm text-gray-600">
                <span>
                    {label}
                    <span className="ml-1 text-gray-400">· {recordCount}건</span>
                </span>
                {rate != null ? (
                    <span className="font-semibold">{toPercent(rate)}</span>
                ) : (
                    <span className="text-gray-400">{getMissingRateText(recordCount)}</span>
                )}
            </div>
            <div className="h-2 rounded-full bg-gray-100 overflow-hidden">
                {rate != null && (
                    <div
                        className={`h-full rounded-full ${getRateColor(rate)}`}
                        style={{ width: toPercent(rate) }}
                    />
                )}
            </div>
        </div>
    );
}
