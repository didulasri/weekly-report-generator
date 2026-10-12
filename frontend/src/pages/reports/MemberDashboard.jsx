import { useMemo } from "react";
import { Link } from "react-router-dom";
import { AlertCircle, Clock, FileText, FolderGit2, Loader2, Plus } from "lucide-react";
import { Button } from "@/components/ui/button";
import PageHeader from "@/components/layout/PageHeader";
import StatusBadge from "@/components/common/StatusBadge";
import { useCurrentUser } from "@/hooks/useAuth";
import { useMyReports } from "@/hooks/useReports";
import { currentWeekRange, formatWeekLabel } from "@/utils/weekDates";

function StatCard({ label, value, trend, tone, icon: Icon, loading }) {
  return (
    <div className="flex flex-1 flex-col gap-3 rounded-xl border border-slate-200 bg-white p-5 shadow-[0_1px_1.5px_rgba(0,0,0,0.04)]">
      <div className="flex items-center justify-between">
        <p className="text-[13px] font-medium text-slate-600">{label}</p>
        <Icon className="size-4 text-slate-400" />
      </div>
      <div className="flex flex-col gap-1">
        <p className="text-[28px] font-bold text-slate-900">{loading ? "—" : value}</p>
        <div className="flex flex-wrap items-center gap-1 text-xs">
          <span className={tone === "negative" ? "font-semibold text-red-700" : "font-semibold text-emerald-700"}>
            {trend}
          </span>
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

function formatDate(isoString) {
  if (!isoString) return "—";
  return new Date(isoString).toLocaleDateString("en-US", { month: "short", day: "numeric", year: "numeric" });
}

export default function MemberDashboard() {
  const { data: currentUser } = useCurrentUser();
  const week = useMemo(() => currentWeekRange(), []);

  const recent = useMyReports({ size: 5 });
  const total = useMyReports({ status: undefined, size: 1 });
  const draft = useMyReports({ status: "DRAFT", size: 1 });
  const needsCorrection = useMyReports({ status: "NEEDS_CORRECTION", size: 1 });
  const thisWeek = useMyReports({ startDate: week.start, endDate: week.end, size: 20 });

  const isLoading = recent.isPending || total.isPending || draft.isPending || needsCorrection.isPending || thisWeek.isPending;

  const submittedCount = (total.data?.totalElements ?? 0) - (draft.data?.totalElements ?? 0);
  const needsCorrectionCount = needsCorrection.data?.totalElements ?? 0;
  const hoursThisWeek = (thisWeek.data?.content ?? []).reduce(
    (sum, report) => sum + Number(report.totalHours ?? 0),
    0
  );

  // Distinct projects from the most recently touched reports -- there's no "my projects" endpoint
  // scoped to the current user, so this is derived from real report data rather than left blank.
  const activeProjects = useMemo(() => {
    const seen = new Map();
    for (const report of recent.data?.content ?? []) {
      if (!report.projectName || seen.has(report.projectName)) continue;
      seen.set(report.projectName, report.status);
    }
    return Array.from(seen, ([name, status]) => ({ name, status }));
  }, [recent.data]);

  const recentReports = recent.data?.content ?? [];

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title={`Welcome back${currentUser?.name ? `, ${currentUser.name.split(" ")[0]}` : ""}`}
        description={`Reporting Week: ${formatWeekLabel(week.start, week.end)}`}
        action={
          <Button size="lg" className="h-9 gap-1.5 px-3.5 text-[13px] font-semibold" asChild>
            <Link to="/reports/new">
              <Plus className="size-3.5" />
              New Weekly Report
            </Link>
          </Button>
        }
      />

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard
          label="Reports Submitted"
          value={submittedCount}
          trend="All time"
          tone="positive"
          icon={FileText}
          loading={isLoading}
        />
        <StatCard
          label="Hours This Week"
          value={hoursThisWeek.toFixed(1)}
          trend="Target: 40"
          tone="positive"
          icon={Clock}
          loading={isLoading}
        />
        <StatCard
          label="Active Projects"
          value={activeProjects.length}
          trend="From recent reports"
          tone="positive"
          icon={FolderGit2}
          loading={isLoading}
        />
        <StatCard
          label="Needs Correction"
          value={needsCorrectionCount}
          trend={needsCorrectionCount > 0 ? "Requires Action" : "All clear"}
          tone={needsCorrectionCount > 0 ? "negative" : "positive"}
          icon={AlertCircle}
          loading={isLoading}
        />
      </div>

      <div className="flex flex-col gap-6 lg:flex-row">
        <Panel title="Recent Weekly Reports" className="flex min-w-0 flex-1 flex-col gap-4">
          {recent.isPending ? (
            <div className="flex items-center justify-center gap-2 py-10 text-sm text-slate-500">
              <Loader2 className="size-4 animate-spin" />
              Loading…
            </div>
          ) : recentReports.length === 0 ? (
            <p className="py-6 text-center text-sm text-slate-500">No reports yet.</p>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full min-w-[560px] border-collapse text-[13px]">
                <thead>
                  <tr className="border-b border-slate-200 text-left text-xs font-semibold text-slate-600">
                    <th className="pb-2.5 pr-4 font-semibold">Reporting Week</th>
                    <th className="w-[120px] pb-2.5 pr-4 font-semibold">Status</th>
                    <th className="w-[80px] pb-2.5 pr-4 font-semibold">Hours</th>
                    <th className="w-[110px] pb-2.5 font-semibold">Last Updated</th>
                  </tr>
                </thead>
                <tbody>
                  {recentReports.map((report) => (
                    <tr key={report.id} className="border-b border-slate-100 last:border-none">
                      <td className="py-3 pr-4 font-medium text-slate-900">
                        {formatDate(report.weekStartDate)} - {formatDate(report.weekEndDate)}
                      </td>
                      <td className="py-3 pr-4">
                        <StatusBadge status={report.status} />
                      </td>
                      <td className="py-3 pr-4 font-medium text-slate-900">
                        {Number(report.totalHours ?? 0).toFixed(1)}h
                      </td>
                      <td className="py-3 text-slate-600">{formatDate(report.updatedAt)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </Panel>

        <div className="flex w-full flex-col gap-6 lg:w-[380px] lg:shrink-0">
          <Panel title="Active Projects" className="flex flex-col gap-4">
            {activeProjects.length === 0 ? (
              <p className="text-sm text-slate-500">No projects from recent reports.</p>
            ) : (
              <div className="flex flex-col gap-3">
                {activeProjects.map((project) => (
                  <div key={project.name} className="flex items-center justify-between gap-2">
                    <p className="truncate text-[13px] font-semibold text-slate-900">{project.name}</p>
                    <StatusBadge status={project.status} />
                  </div>
                ))}
              </div>
            )}
          </Panel>

          <Panel title="Quick Actions" className="flex flex-col gap-3">
            <div className="flex flex-col gap-2">
              <Button className="h-10 w-full gap-1.5 text-[13px] font-semibold" asChild>
                <Link to="/reports/new">
                  <Plus className="size-3.5" />
                  New Report
                </Link>
              </Button>
              <Button variant="outline" className="h-10 w-full text-[13px] font-semibold" asChild>
                <Link to="/reports">View All Reports</Link>
              </Button>
            </div>
          </Panel>
        </div>
      </div>
    </div>
  );
}
