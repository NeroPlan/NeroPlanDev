interface Props {
  title: string;
  children: React.ReactNode;
}

export default function OnboardingCard({
  title,
  children,
}: Props) {
  return (
    <div className="bg-white rounded-3xl p-6 shadow-sm">
      <h3 className="text-xl font-bold text-center border-b pb-2 mb-4">
        {title}
      </h3>

      {children}
    </div>
  );
}