type ProfileCardProps = {
  name: string;
  imageUrl?: string;
};

export default function ProfileCard({ name, imageUrl }: ProfileCardProps) {
  const initial = name.trim().charAt(0) || "U";

  return (
    <section className="rounded-[28px] bg-white px-6 py-8 text-center shadow-sm">
      {imageUrl ? (
        <img
          src={imageUrl}
          alt={`${name} 프로필`}
          className="mx-auto h-24 w-24 rounded-full object-cover"
        />
      ) : (
        <div className="mx-auto flex h-24 w-24 items-center justify-center rounded-full bg-slate-900 text-3xl font-bold text-white">
          {initial}
        </div>
      )}

      <h2 className="mt-5 text-xl font-bold text-slate-900">{name}</h2>
      <p className="mt-1 text-sm text-slate-500">내 계정</p>
    </section>
  );
}