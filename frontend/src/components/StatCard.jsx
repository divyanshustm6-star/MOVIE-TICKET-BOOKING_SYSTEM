export default function StatCard({ label, value, detail }) {
  return (
    <div className="panel p-5">
      <p className="text-sm font-bold text-slate-400">{label}</p>
      <p className="mt-2 text-3xl font-black text-white">{value}</p>
      {detail && <p className="mt-2 text-xs font-semibold text-ember-300">{detail}</p>}
    </div>
  );
}
