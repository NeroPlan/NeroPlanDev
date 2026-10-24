// components/PlanPage/AnalyzeButton.tsx

interface AnalyzeButtonProps {
    onClick: () => void;
    loading: boolean;
}

export default function AnalyzeButton({
    onClick,
    loading,
}: AnalyzeButtonProps) {
    return (
        <button
            onClick={onClick}
            disabled={loading}
            className="
                w-full
                bg-blue-600
                text-white
                font-bold
                py-3
                rounded-2xl
                shadow-sm
                hover:bg-blue-700
                transition
                disabled:opacity-50
            "
        >
            {loading ? "분석 결과 불러오는 중..." : "분석하기"}
        </button>
    );
}
