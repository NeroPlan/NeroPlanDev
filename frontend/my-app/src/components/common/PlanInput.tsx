// components/common/PlanInput.tsx

export default function PlanInput() {
    return (
        <div className="bg-white rounded-3xl p-6 shadow-sm">
            <h2 className="text-xl font-bold text-center mb-3">
                계획 입력 란
            </h2>

            <hr className="mb-4" />

           <div className="space-y-3">
                <input
                    type="text"
                    placeholder="계획을 입력하세요"
                    className="
                        w-full
                        border
                        rounded-xl
                        px-3
                        py-2
                        outline-none
                    "
                />
            
                <button
                    className="
                        w-full
                        border
                        border-dashed
                        rounded-xl
                        py-2
                        text-xl
                        font-bold
                    "
                >
                    +
                </button>
            </div>
        </div>
    );
}