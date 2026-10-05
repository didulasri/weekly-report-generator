import { AlertCircle, ClipboardCheck, TrendingUp, Users } from "lucide-react";
import PageHeader from "@/components/layout/PageHeader";
import { Button } from "@/components/ui/button";

// Static mock data -- UI-only checkpoint, no report/team service wired yet.
const STATS = [
  { label: "Team Members", value: "8", trend: "Fully staffed", tone: "positive", icon: Users },
  { label: "Pending Review", value: "5", trend: "Awaiting Action", tone: "negative", icon: AlertCircle },
  { label: "Approved This Week", value: "12", trend: "On track", tone: "positive", icon: ClipboardCheck },
  { label: "Average Hours", value: "41.2 hrs", trend: "+1.2 hrs deviation", tone: "positive", icon: TrendingUp },
];

const PENDING_REVIEWS = [
  { name: "Marcus Vance", initials: "MV", week: "Week 42", project: "Acme Platform Integration", submitted: "Oct 20, 2024" },
  { name: "Sarah Connor", initials: "SC", week: "Week 42", project: "Global Billing Redesign", submitted: "Oct 19, 2024" },
  { name: "Alex Mercer", initials: "AM", week: "Week 42", project: "Security Audit Patching", submitted: "Oct 18, 2024" },
  { name: "Helena Rostova", initials: "HR", week: "Week 41", project: "Billing Redesign", submitted: "Oct 15, 2024" },
  { name: "Linus Torvald", initials: "LT", week: "Week 41", project: "Acme Platform Integration", submitted: "Oct 14, 2024" },
];

const TEAM_SUBMISSION = [
  { name: "Marcus Vance", initials: "MV", submitted: true, hours: "38.5h" },
  { name: "Sarah Connor", initials: "SC", submitted: true, hours: "40.0h" },
  { name: "Alex Mercer", initials: "AM", submitted: true, hours: "35.5h" },
  { name: "Helena Rostova", initials: "HR", submitted: true, hours: "42.0h" },
  { name: "Linus Torvald", initials: "LT", submitted: true, hours: "45.0h" },
  { name: "Bill Gates", initials: "BG", submitted: false, hours: "0.0h" },
  { name: "Ada Lovelace", initials: "AL", submitted: false, hours: "12.5h" },
  { name: "Grace Hopper", initials: "GH", submitted: true, hours: "39.5h" },
];

function Avatar({ initials }) {
  return (
    <div className="flex size-6 shrink-0 items-center justify-center rounded-full bg-slate-100 text-[10px] font-semibold text-slate-900">
      {initials}
    </div>
  );
}

function StatCard({ label, value, trend, tone, icon: Icon }) {
  return (
    <div className="flex flex-1 flex-col gap-3 rounded-xl border border-slate-200 bg-white p-5 shadow-[0_1px_1.5px_rgba(0,0,0,0.04)]">
      <div className="flex items-center justify-between">
        <p className="text-[13px] font-medium text-slate-600">{label}</p>
        <Icon className="size-4 text-slate-400" />
      </div>
      <div className="flex flex-col gap-1">
        <p className="text-[28px] font-bold text-slate-900">{value}</p>
        <div className="flex flex-wrap items-center gap-1 text-xs">
          <span className={tone === "negative" ? "font-semibold text-red-700" : "font-semibold text-emerald-700"}>
            {trend}
          </span>
          <span className="font-medium text-slate-400">vs last week</span>
        </div>
      </div>
    </div>
  );
}

function Panel({ title, action, className, children }) {
  return (
    <div className={`rounded-xl border border-slate-200 bg-white p-5 shadow-[0_1px_1.5px_rgba(0,0,0,0.04)] ${className ?? ""}`}>
      <div className="flex items-center justify-between">
        <p className="text-[15px] font-bold text-slate-900">{title}</p>
        {action}
      </div>
      {children}
    </div>
  );
}

export default function ManagerDashboard() {
  return (
    <div className="flex flex-col gap-6">
      <PageHeader title="Manager Control Center" description="Overview of team status and pending validations" />

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {STATS.map((stat) => (
          <StatCard key={stat.label} {...stat} />
        ))}
      </div>

      <div className="flex flex-col gap-6 lg:flex-row">
        <Panel title="Reports Pending Review" className="flex min-w-0 flex-1 flex-col gap-4">
          <div className="overflow-x-auto">
            <table className="w-full min-w-[640px] border-collapse text-[13px]">
              <thead>
                <tr className="border-b border-slate-200 text-left text-xs font-semibold text-slate-600">
                  <th className="w-[180px] pb-2.5 pr-4 font-semibold">Employee</th>
                  <th className="w-[80px] pb-2.5 pr-4 font-semibold">Week</th>
                  <th className="pb-2.5 pr-4 font-semibold">Project</th>
                  <th className="w-[110px] pb-2.5 pr-4 font-semibold">Submitted</th>
                  <th className="w-[80px] pb-2.5 text-right font-semibold">Action</th>
                </tr>
              </thead>
              <tbody>
                {PENDING_REVIEWS.map((row) => (
                  <tr key={row.name + row.week} className="border-b border-slate-100 last:border-none">
                    <td className="py-3 pr-4">
                      <div className="flex items-center gap-2">
                        <Avatar initials={row.initials} />
                        <span className="truncate font-semibold text-slate-900">{row.name}</span>
                      </div>
                    </td>
                    <td className="py-3 pr-4 text-slate-600">{row.week}</td>
                    <td className="truncate py-3 pr-4 text-slate-900">{row.project}</td>
                    <td className="py-3 pr-4 text-slate-600">{row.submitted}</td>
                    <td className="py-3 text-right">
                      <Button
                        type="button"
                        size="sm"
                        className="h-7 border border-[#1a0b2e] bg-[#ffb7a5] px-2.5 text-[11px] font-bold text-[#1a0b2e] hover:bg-[#ffb7a5]/90"
                      >
                        Review
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Panel>

        <Panel
          title="Team Submission"
          action={<p className="text-xs font-semibold text-slate-600">Week 42</p>}
          className="flex w-full flex-col gap-4 lg:w-[380px] lg:shrink-0"
        >
          <div className="flex flex-col gap-3">
            {TEAM_SUBMISSION.map((row) => (
              <div key={row.name} className="flex items-center justify-between gap-2">
                <div className="flex min-w-0 items-center gap-2">
                  <Avatar initials={row.initials} />
                  <span className="truncate text-[13px] font-medium text-slate-900">{row.name}</span>
                </div>
                <div className="flex shrink-0 items-center gap-2.5">
                  <span
                    className={`rounded px-2 py-0.5 text-[10px] font-semibold ${
                      row.submitted ? "bg-emerald-50 text-emerald-700" : "bg-red-50 text-red-700"
                    }`}
                  >
                    {row.submitted ? "Submitted" : "Not Submitted"}
                  </span>
                  <span className="text-[13px] font-semibold text-slate-600">{row.hours}</span>
                </div>
              </div>
            ))}
          </div>
        </Panel>
      </div>
    </div>
  );
}
