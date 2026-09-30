import { REPORT_STATUSES } from "@/utils/constants";
import { cn } from "@/lib/utils";

// Renders one of the four ReportStatus values with its fixed colour (see utils/constants.js).
// An unrecognized status still renders (falls back to a neutral style + the raw value) rather
// than throwing, so a backend enum this component doesn't yet know about never blanks a whole row.
export default function StatusBadge({ status, className }) {
  const config = REPORT_STATUSES[status];

  return (
    <span
      className={cn(
        "inline-flex items-center rounded border px-2 py-0.5 text-[11px] font-semibold uppercase",
        config?.className ?? "bg-slate-50 text-slate-600 border-slate-200",
        className
      )}
    >
      {config?.label ?? status}
    </span>
  );
}
