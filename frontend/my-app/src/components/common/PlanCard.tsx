// components/common/PlanCard.tsx

interface PlanCardProps {
    title: string;
    plans: string[];
}

export default function PlanCard({
    title,
    plans,
}: PlanCardProps) {
    return (
        <div className="bg-white rounded-3xl p-6 shadow-sm">
            <h2 className="text-xl font-bold text-center mb-3">
                {title}
            </h2>

            <hr className="mb-4" />

            <ul className="list-disc pl-6 space-y-2 text-lg font-semibold">
                {plans.map((plan) => (
                    <li key={plan}>
                        {plan}
                    </li>
                ))}
            </ul>
        </div>
    );
}