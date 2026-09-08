type MenuItem = {
  label: string;
  onClick: () => void;
  danger?: boolean;
};

type MyPageMenuProps = {
  items: MenuItem[];
};

export default function MyPageMenu({ items }: MyPageMenuProps) {
  return (
    <section className="overflow-hidden rounded-[28px] bg-white shadow-sm">
      {items.map((item, index) => (
        <button
          key={item.label}
          type="button"
          onClick={item.onClick}
          className={`flex w-full items-center justify-between px-6 py-5 text-left text-base font-semibold transition hover:bg-slate-50 ${
            index !== items.length - 1 ? "border-b border-slate-100" : ""
          } ${item.danger ? "text-red-500 hover:bg-red-50" : "text-slate-900"}`}
        >
          {item.label}
          <span className="text-xl font-normal text-slate-400">›</span>
        </button>
      ))}
    </section>
  );
}