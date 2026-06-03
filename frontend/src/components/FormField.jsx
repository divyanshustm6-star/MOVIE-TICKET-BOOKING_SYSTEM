export default function FormField({ label, error, children }) {
  return (
    <label className="label">
      <span>{label}</span>
      {children}
      {error && <span className="text-xs font-semibold text-red-300">{error}</span>}
    </label>
  );
}
