import { Link } from "react-router-dom";
import { AlertCircle, ClipboardCheck, Loader2, TrendingUp, Users } from "lucide-react";
import PageHeader from "@/components/layout/PageHeader";
import { Button } from "@/components/ui/button";
import { useDashboardStatusSummary, useDashboardSummary } from "@/hooks/useDashboard";
import { useTeamMembers, useTeamReports } from "@/hooks/useManagerReports";

function Avatar({ name }) {
  const initials = (name ?? "?")
    .trim()
    .split(/\s+/)
    .map((p) => p[0])
    .slice(0, 2)
    .join("")
    .toUpperCase();
  return (
    <div className="flex size-6 shrink-0 items-center justify-center rounded-full bg-slate-100 text-[10px] font-semibold text-slate-900">
      {initials}
    </div>
  );
}

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

function formatDate(isoString) {
  if (!isoString) return "—";
  return new Date(isoString).toLocaleDateString("en-US", { month: "short", day: "numeric", year: "numeric" });
}

export default function ManagerDashboard() {
  const summary = useDashboardSummary();
  const statusSummary = useDashboardStatusSummary();
  const pendingReview = useTeamReports({ status: "SUBMITTED", size: 5 });
  // /api/manager/team-members returns a plain list, not a paged response -- size large enough to
  // cover every team member in one call so .length is an accurate count.
  const teamMembers = useTeamMembers({ size: 200 });

  const isLoading = summary.isPending || statusSummary.isPending || pendingReview.isPending || teamMembers.isPending;

  const stats = [
    {
      label: "Team Members",
      value: teamMembers.data?.length ?? 0,
      trend: "Active",
      tone: "positive",
      icon: Users,
    },
    {
      label: "Pending Review",
      value: summary.data?.submittedCount ?? 0,
      trend: "Awaiting Action",
      tone: (summary.data?.submittedCount ?? 0) > 0 ? "negative" : "positive",
      icon: AlertCircle,
    },
    {
      label: "Approved This Week",
      value: summary.data?.approvedCount ?? 0,
      trend: "On track",
      tone: "positive",
      icon: ClipboardCheck,
    },
    {
      label: "Compliance Rate",
      value: `${summary.data?.compliance?.ratePercent ?? 0}%`,
      trend: `${summary.data?.compliance?.onTime ?? 0} on time`,
      tone: "positive",
      icon: TrendingUp,
    },
  ];

  const pendingRows = pendingReview.data?.content ?? [];
  const members = statusSummary.data?.members ?? [];

  return (
    <div className="flex flex-col gap-6">
      <PageHeader title="Manager Control Center" description="Overview of team status and pending validations" />

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {stats.map((stat) => (
          <StatCard key={stat.label} {...stat} loading={isLoading} />
        ))}
      </div>

      <div className="flex flex-col gap-6 lg:flex-row">
        <Panel title="Reports Pending Review" className="flex min-w-0 flex-1 flex-col gap-4">
          {pendingReview.isPending ? (
            <div className="flex items-center justify-center gap-2 py-10 text-sm text-slate-500">
              <Loader2 className="size-4 animate-spin" />
              Loading…
            </div>
          ) : pendingRows.length === 0 ? (
            <p className="py-6 text-center text-sm text-slate-500">Nothing pending review.</p>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full min-w-[640px] border-collapse text-[13px]">
                <thead>
                  <tr className="border-b border-slate-200 text-left text-xs font-semibold text-slate-600">
                    <th className="w-[180px] pb-2.5 pr-4 font-semibold">Employee</th>
                    <th className="w-[110px] pb-2.5 pr-4 font-semibold">Week</th>
                    <th className="pb-2.5 pr-4 font-semibold">Project</th>
                    <th className="w-[110px] pb-2.5 pr-4 font-semibold">Submitted</th>
                    <th className="w-[80px] pb-2.5 text-right font-semibold">Action</th>
                  </tr>
                </thead>
                <tbody>
                  {pendingRows.map((row) => (
                    <tr key={row.report.id} className="border-b border-slate-100 last:border-none">
                      <td className="py-3 pr-4">
                        <div className="flex items-center gap-2">
                          <Avatar name={row.ownerName} />
                          <span className="truncate font-semibold text-slate-900">{row.ownerName}</span>
                        </div>
                      </td>
                      <td className="py-3 pr-4 text-slate-600">{formatDate(row.report.weekStartDate)}</td>
                      <td className="truncate py-3 pr-4 text-slate-900">{row.report.projectName}</td>
                      <td className="py-3 pr-4 text-slate-600">{formatDate(row.report.updatedAt)}</td>
                      <td className="py-3 text-right">
                        <Button
                          type="button"
                          size="sm"
                          className="h-7 border border-[#1a0b2e] bg-[#ffb7a5] px-2.5 text-[11px] font-bold text-[#1a0b2e] hover:bg-[#ffb7a5]/90"
                          asChild
                        >
                          <Link to={`/team/reports/${row.report.id}`}>Review</Link>
                        </Button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </Panel>

        <Panel
          title="Team Submission"
          action={<p className="text-xs font-semibold text-slate-600">This Week</p>}
          className="flex w-full flex-col gap-4 lg:w-[380px] lg:shrink-0"
        >
          {statusSummary.isPending ? (
            <div className="flex items-center justify-center gap-2 py-10 text-sm text-slate-500">
              <Loader2 className="size-4 animate-spin" />
              Loading…
            </div>
          ) : members.length === 0 ? (
            <p className="py-6 text-center text-sm text-slate-500">No team members found.</p>
          ) : (
            <div className="flex flex-col gap-3">
              {members.map((member) => {
                const submitted = member.notStarted === 0;
                return (
                  <div key={member.userId} className="flex items-center justify-between gap-2">
                    <div className="flex min-w-0 items-center gap-2">
                      <Avatar name={member.name} />
                      <span className="truncate text-[13px] font-medium text-slate-900">{member.name}</span>
                    </div>
                    <span
                      className={`shrink-0 rounded px-2 py-0.5 text-[10px] font-semibold ${
                        submitted ? "bg-emerald-50 text-emerald-700" : "bg-red-50 text-red-700"
                      }`}
                    >
                      {submitted ? "Submitted" : "Not Submitted"}
                    </span>
                  </div>
                );
              })}
            </div>
          )}
        </Panel>
      </div>
    </div>
  );
}
