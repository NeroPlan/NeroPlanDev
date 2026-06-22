interface Props {
  selected: boolean;
  label: string;
  onClick: () => void;
}

export default function SelectButton({
  selected,
  label,
  onClick,
}: Props) {
  return (
    <button
      onClick={onClick}
      className={`
        h-14
        rounded-2xl
        border
        transition-all

        ${
          selected
            ? "border-black bg-gray-100 font-semibold"
            : "border-gray-300 bg-white"
        }
      `}
    >
      {label}
    </button>
  );
}