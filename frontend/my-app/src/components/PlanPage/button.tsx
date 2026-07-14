interface AnalyzeButtonProps {
    onClick: () => void;
}

export default function AnalyzeButton({ onClick }: AnalyzeButtonProps) {
    return (
        <button
            onClick={onClick}
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
            "
        >
            분석하기
        </button>
    );
}