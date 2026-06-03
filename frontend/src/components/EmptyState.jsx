export default function EmptyState({ title = 'Nothing here yet', message = 'Try changing filters or creating a new record.' }) {
  return (
    <div className="panel grid place-items-center px-6 py-14 text-center">
      <div>
        <div className="mx-auto mb-4 grid h-14 w-14 place-items-center rounded-2xl bg-white/10 text-2xl">?</div>
        <h2 className="text-xl font-black text-white">{title}</h2>
        <p className="mt-2 max-w-md text-sm leading-6 text-slate-400">{message}</p>
      </div>
    </div>
  );
}
