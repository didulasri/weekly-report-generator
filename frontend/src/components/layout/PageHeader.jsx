// Every page under the shell starts with one of these: a title, an optional one-line description,
// and an optional action slot on the right (a primary button, usually). Kept dumb on purpose --
// no data fetching, no layout beyond this row -- so every page's header looks and behaves the same.
export default function PageHeader({ title, description, action }) {
  return (
    <div className="flex flex-wrap items-start justify-between gap-4">
      <div className="flex flex-col gap-1">
        <h1 className="text-xl font-bold text-slate-900 sm:text-[22px]">{title}</h1>
        {description && <p className="text-sm text-slate-500">{description}</p>}
      </div>
      {action && <div className="shrink-0">{action}</div>}
    </div>
  );
}
