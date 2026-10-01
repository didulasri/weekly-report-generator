import { AlertCircle, Clock, FileText, FolderGit2, Plus } from "lucide-react";
import { Button } from "@/components/ui/button";
import PageHeader from "@/components/layout/PageHeader";
import StatusBadge from "@/components/common/StatusBadge";

// Static mock data -- UI-only checkpoint, no service/API wiring yet. Shapes mirror the eventual
// backend response (ReportStatus enum values, not display labels) so StatusBadge/REPORT_STATUSES
// can be reused as-is once this is wired to real data.
const STATS = [
  { label: "Reports Submitted", value: "12", trend: "+1 this month", tone: "positive", icon: FileText },
  { label: "Hours This Week", value: "38.5", trend: "Target: 40", tone: "positive", icon: Clock },
  { label: "Active Projects", value: "3", trend: "Consistent", tone: "positive", icon: FolderGit2 },
  { label: "Pending Reviews", value: "1", trend: "Requires Action", tone: "negative", icon: AlertCircle },
];

const RECENT_REPORTS = [
  { week: "Week 42 (Oct 14 - Oct 20)", status: "APPROVED", hours: "38.5h", submitted: "Oct 20, 2024" },
  { week: "Week 41 (Oct 07 - Oct 13)", status: "APPROVED", hours: "40.0h", submitted: "Oct 13, 2024" },
  { week: "Week 40 (Sep 30 - Oct 06)", status: "SUBMITTED", hours: "39.0h", submitted: "Oct 06, 2024" },
  { week: "Week 39 (Sep 23 - Sep 29)", status: "APPROVED", hours: "42.5h", submitted: "Sep 29, 2024" },
  { week: "Week 38 (Sep 16 - Sep 22)", status: "DRAFT", hours: "12.0h", submitted: "Not Submitted" },
];

const ACTIVE_PROJECTS = [
  { name: "Acme Core Platform Integration", roleLabel: "Technical Lead", progress: 75 },
  { name: "Global Billing Architecture Redesign", roleLabel: "Contributor", progress: 40 },
  { name: "Internal Security Audit Patching", roleLabel: "Reporter", progress: 95 },
];

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

function Panel({ title, className, children }) {
  return (
    <div className={`rounded-xl border border-slate-200 bg-white p-5 shadow-[0_1px_1.5px_rgba(0,0,0,0.04)] ${className ?? ""}`}>
      <p className="text-[15px] font-bold text-slate-900">{title}</p>
      {children}
    </div>
  );
}

export default function MemberDashboard() {
  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title="Welcome back, Marcus"
        description="Reporting Week: Oct 14 - Oct 20, 2024 (Week 42)"
        action={
          <Button size="lg" className="h-9 gap-1.5 px-3.5 text-[13px] font-semibold">
            <Plus className="size-3.5" />
            New Weekly Report
          </Button>
        }
      />

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {STATS.map((stat) => (
          <StatCard key={stat.label} {...stat} />
        ))}
      </div>

      <div className="flex flex-col gap-6 lg:flex-row">
        <Panel title="Recent Weekly Reports" className="flex min-w-0 flex-1 flex-col gap-4">
          <div className="overflow-x-auto">
            <table className="w-full min-w-[560px] border-collapse text-[13px]">
              <thead>
                <tr className="border-b border-slate-200 text-left text-xs font-semibold text-slate-600">
                  <th className="pb-2.5 pr-4 font-semibold">Reporting Week</th>
                  <th className="w-[120px] pb-2.5 pr-4 font-semibold">Status</th>
                  <th className="w-[80px] pb-2.5 pr-4 font-semibold">Hours</th>
                  <th className="w-[110px] pb-2.5 font-semibold">Submitted Date</th>
                </tr>
              </thead>
              <tbody>
                {RECENT_REPORTS.map((report) => (
                  <tr key={report.week} className="border-b border-slate-100 last:border-none">
                    <td className="py-3 pr-4 font-medium text-slate-900">{report.week}</td>
                    <td className="py-3 pr-4">
                      <StatusBadge status={report.status} />
                    </td>
                    <td className="py-3 pr-4 font-medium text-slate-900">{report.hours}</td>
                    <td className="py-3 text-slate-600">{report.submitted}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Panel>

        <div className="flex w-full flex-col gap-6 lg:w-[380px] lg:shrink-0">
          <Panel title="Active Projects" className="flex flex-col gap-4">
            <div className="flex flex-col gap-3.5">
              {ACTIVE_PROJECTS.map((project) => (
                <div key={project.name} className="flex flex-col gap-1.5">
                  <div className="flex items-start justify-between gap-2">
                    <p className="truncate text-[13px] font-semibold text-slate-900">{project.name}</p>
                    <p className="shrink-0 text-[11px] font-medium text-slate-600">{project.roleLabel}</p>
                  </div>
                  <div className="flex items-center gap-2">
                    <div className="h-1.5 flex-1 overflow-hidden rounded-full bg-slate-100">
                      <div className="h-full rounded-full bg-indigo-600" style={{ width: `${project.progress}%` }} />
                    </div>
                    <p className="text-xs font-semibold text-slate-600">{project.progress}%</p>
                  </div>
                </div>
              ))}
            </div>
          </Panel>

          <Panel title="Quick Actions" className="flex flex-col gap-3">
            <div className="flex flex-col gap-2">
              <Button className="h-10 w-full gap-1.5 text-[13px] font-semibold">
                <Plus className="size-3.5" />
                New Report
              </Button>
              <Button variant="outline" className="h-10 w-full text-[13px] font-semibold">
                View All Reports
              </Button>
            </div>
          </Panel>
        </div>
      </div>
    </div>
  );
}
