// Generic "nothing here" state -- icon, a message, and an optional action (e.g. "Create your
// first report"). Used instead of leaving a list/table area blank, per the UX baseline every
// screen follows (frontend-conventions skill: "Empty states are designed, not blank").
export default function EmptyState({ icon: Icon, title, description, action }) {
  return (
    <div className="flex flex-col items-center justify-center gap-3 px-6 py-12 text-center">
      {Icon && (
        <div className="flex size-12 items-center justify-center rounded-full bg-slate-100 text-slate-400">
          <Icon className="size-6" />
        </div>
      )}
      <div className="flex flex-col gap-1">
        <p className="text-sm font-semibold text-slate-900">{title}</p>
        {description && <p className="text-sm text-slate-500">{description}</p>}
      </div>
      {action && <div className="mt-2">{action}</div>}
    </div>
  );
}
