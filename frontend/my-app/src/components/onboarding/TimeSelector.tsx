interface Props {
  value: string;
  onChange: (value: string) => void;
}

export default function TimeSelector({
  value,
  onChange,
}: Props) {
  return (
    <input
      type="time"
      value={value}
      onChange={(e) =>
        onChange(e.target.value)
      }
      className="
        w-full
        p-3
        rounded-2xl
        border
        border-gray-300
      "
    />
  );
}