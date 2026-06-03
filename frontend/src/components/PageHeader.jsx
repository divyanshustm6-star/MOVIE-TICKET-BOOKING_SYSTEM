export default function PageHeader({ eyebrow, title, description, actions }) {
  return (
    <div className="flex flex-col gap-5 md:flex-row md:items-end md:justify-between">
      <div className="max-w-3xl">
        {eyebrow && <p className="mb-3 text-xs font-black uppercase tracking-[0.3em] text-ember-400">{eyebrow}</p>}
        <h1 className="text-3xl font-black tracking-tight text-white md:text-5xl">{title}</h1>
        {description && <p className="mt-3 text-base leading-7 text-slate-400">{description}</p>}
      </div>
      {actions && <div className="flex flex-wrap gap-3">{actions}</div>}
    </div>
  );
}
