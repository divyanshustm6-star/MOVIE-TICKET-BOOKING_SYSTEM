export default function LoadingSkeleton({ rows = 6, className = '' }) {
  return (
    <div className={`grid gap-4 ${className}`}>
      {Array.from({ length: rows }).map((_, index) => (
        <div key={index} className="panel animate-pulse p-5">
          <div className="h-4 w-2/3 rounded bg-white/10" />
          <div className="mt-4 h-3 w-full rounded bg-white/10" />
          <div className="mt-2 h-3 w-4/5 rounded bg-white/10" />
        </div>
      ))}
    </div>
  );
}
