const interestList = [
  "개발",
  "운동",
  "독서",
  "영어",
  "음악",
  "재테크",
  "게임",
  "여행",
];

interface Props {
  selected: string[];
  onChange: (value: string[]) => void;
}

export default function InterestSelector({
  selected,
  onChange,
}: Props) {
  const toggleInterest = (
    interest: string
  ) => {
    if (selected.includes(interest)) {
      onChange(
        selected.filter(
          (item) => item !== interest
        )
      );
    } else {
      onChange([
        ...selected,
        interest,
      ]);
    }
  };

  return (
    <div className="grid grid-cols-2 gap-3">
      {interestList.map((interest) => (
        <button
          key={interest}
          onClick={() =>
            toggleInterest(interest)
          }
          className={`
            h-12
            rounded-xl
            border

            ${
              selected.includes(
                interest
              )
                ? "border-black bg-gray-100"
                : "border-gray-300"
            }
          `}
        >
          {interest}
        </button>
      ))}
    </div>
  );
}