import { useMemo } from "react";
import { Link } from "react-router-dom";
import { ClipboardList, Folder, Loader2, Mail, UserCheck, Users } from "lucide-react";
import { FolderPlus, ShieldCheck, UserPlus } from "lucide-react";
import PageHeader from "@/components/layout/PageHeader";
import { Button } from "@/components/ui/button";
import { useInvitations, useResendInvitation } from "@/hooks/useInvitations";
import { useDashboardSummary, useActivityFeed } from "@/hooks/useDashboard";
import { useTeamReports } from "@/hooks/useManagerReports";
import { useProjects } from "@/hooks/useProjects";
import { useUsers } from "@/hooks/useUsers";
import { REPORT_STATUSES, ROLE_LABELS } from "@/utils/constants";
import { getApiErrorMessage } from "@/utils/apiError";
import { toast } from "sonner";

const ROLE_COLORS = {
  TEAM_MEMBER: "#4f46e5",
  MANAGER: "#7c3aed",
  ADMIN: "#ffb7a5",
};

// Activity feed `action` -> icon, mirroring ActivityFeedEntryResponse's documented values
// ("SUBMITTED", "APPROVED", "REQUEST_CHANGES") plus the invitation-domain events this feed
// doesn't actually carry yet (kept as a fallback, not fabricated data).
const ACTIVITY_ICON = {
  SUBMITTED: UserPlus,
  APPROVED: ShieldCheck,
  REQUEST_CHANGES: FolderPlus,
};

function StatCard({ label, value, trend, trendSuffix, tone, icon: Icon, loading }) {
  const toneClass =
    tone === "positive" ? "text-emerald-700" : tone === "warning" ? "text-amber-700" : "text-slate-500";
  return (
    <div className="flex flex-1 flex-col gap-2.5 rounded-[10px] border border-slate-200 bg-white p-4">
      <div className="flex items-center justify-between">
        <p className="text-xs font-medium text-slate-600">{label}</p>
        <Icon className="size-4 text-slate-400" />
      </div>
      <p className="text-[28px] leading-8 font-bold text-slate-900">{loading ? "—" : value}</p>
      <p className="text-[11px] text-slate-500">
        {tone === "positive" ? <span className={`font-semibold ${toneClass}`}>{trend}</span> : <span className={toneClass}>{trend}</span>}
        {trendSuffix}
      </p>
    </div>
  );
}

function SectionCard({ title, description, action, className, children }) {
  return (
    <div className={`flex flex-col gap-4 rounded-[10px] border border-slate-200 bg-white p-5 ${className ?? ""}`}>
      <div className="flex items-center justify-between gap-2">
        <div className="flex flex-col gap-1">
          <p className="text-[15px] font-bold text-slate-900">{title}</p>
          {description && <p className="text-xs text-slate-500">{description}</p>}
        </div>
        {action}
      </div>
      {children}
    </div>
  );
}

function ViewAllLink({ to }) {
  return (
    <Link to={to} className="shrink-0 text-xs font-semibold text-[#1a0b2e] hover:underline">
      View All &rarr;
    </Link>
  );
}

function formatDate(isoString) {
  if (!isoString) return "—";
  return new Date(isoString).toLocaleDateString("en-US", { month: "short", day: "numeric", year: "numeric" });
}

function timeAgo(isoString) {
  if (!isoString) return "—";
  const diffMs = Date.now() - new Date(isoString).getTime();
  const minutes = Math.round(diffMs / 60000);
  if (minutes < 1) return "just now";
  if (minutes < 60) return `${minutes}m ago`;
  const hours = Math.round(minutes / 60);
  if (hours < 24) return `${hours}h ago`;
  return `${Math.round(hours / 24)}d ago`;
}

function DonutChart({ gradient, total }) {
  return (
    <div
      className="relative flex size-[116px] shrink-0 items-center justify-center rounded-full"
      style={{ background: gradient || "#e2e8f0" }}
    >
      <div className="flex size-[82px] flex-col items-center justify-center rounded-full bg-white text-center">
        <p className="text-[22px] font-bold text-slate-900">{total}</p>
        <p className="text-[10px] text-slate-500">active users</p>
      </div>
    </div>
  );
}

export default function AdminDashboard() {
  const totalUsers = useUsers({ size: 1 });
  const activeUsers = useUsers({ active: true, size: 1 });
  const teamMemberCount = useUsers({ role: "TEAM_MEMBER", active: true, size: 1 });
  const managerCount = useUsers({ role: "MANAGER", active: true, size: 1 });
  const adminCount = useUsers({ role: "ADMIN", active: true, size: 1 });

  const pendingInvitations = useInvitations({ status: "PENDING", size: 5 });
  const resendInvitation = useResendInvitation();

  const totalProjects = useProjects({ size: 1 });
  const activeProjects = useProjects({ status: "ACTIVE", size: 3 });

  const summary = useDashboardSummary();
  const activityFeed = useActivityFeed({ size: 5 });
  // Sorted by submittedAt DESC server-side, and Postgres puts NULLs first in a DESC sort -- a
  // DRAFT report (submittedAt is always null) would otherwise crowd out everything actually
  // recent. Over-fetch and drop drafts client-side rather than surfacing reports nobody submitted.
  const recentReportsQuery = useTeamReports({ size: 20 });
  const recentReports = {
    ...recentReportsQuery,
    data: recentReportsQuery.data && {
      ...recentReportsQuery.data,
      content: recentReportsQuery.data.content.filter((row) => row.report.status !== "DRAFT").slice(0, 4),
    },
  };

  const userDistribution = useMemo(() => {
    const rows = [
      { label: "Team Members", role: "TEAM_MEMBER", value: teamMemberCount.data?.totalElements ?? 0 },
      { label: "Managers", role: "MANAGER", value: managerCount.data?.totalElements ?? 0 },
      { label: "Admins", role: "ADMIN", value: adminCount.data?.totalElements ?? 0 },
    ];
    const total = rows.reduce((sum, row) => sum + row.value, 0);
    let cursor = 0;
    const stops = [];
    for (const row of rows) {
      const start = cursor;
      const end = total > 0 ? start + (row.value / total) * 100 : start;
      stops.push(`${ROLE_COLORS[row.role]} ${start}% ${end}%`);
      cursor = end;
    }
    return { rows, total, gradient: `conic-gradient(${stops.join(", ")})` };
  }, [teamMemberCount.data, managerCount.data, adminCount.data]);

  const statusBreakdown = summary.data;
  const statusBars = statusBreakdown
    ? [
        { label: "Submitted", value: statusBreakdown.submittedCount, className: "bg-blue-600" },
        { label: "Approved", value: statusBreakdown.approvedCount, className: "bg-emerald-700" },
        { label: "Needs Correction", value: statusBreakdown.needsCorrectionCount, className: "bg-red-700" },
        { label: "Draft", value: statusBreakdown.draftCount, className: "bg-slate-500" },
      ]
    : [];
  const statusTotal = statusBars.reduce((sum, row) => sum + row.value, 0);

  const statsLoading = totalUsers.isPending || activeUsers.isPending || pendingInvitations.isPending || totalProjects.isPending || summary.isPending;

  const stats = [
    { label: "Total Users", value: totalUsers.data?.totalElements ?? 0, trend: "All roles", tone: "neutral", icon: Users },
    {
      label: "Active Users",
      value: activeUsers.data?.totalElements ?? 0,
      trend:
        totalUsers.data?.totalElements > 0
          ? `${Math.round(((activeUsers.data?.totalElements ?? 0) / totalUsers.data.totalElements) * 100)}% of all users`
          : "—",
      tone: "neutral",
      icon: UserCheck,
    },
    {
      label: "Pending Invitations",
      value: pendingInvitations.data?.totalElements ?? 0,
      trend: "Awaiting acceptance",
      tone: "warning",
      icon: Mail,
    },
    {
      label: "Projects",
      value: totalProjects.data?.totalElements ?? 0,
      trend: `${activeProjects.data?.totalElements ?? 0} active projects`,
      tone: "neutral",
      icon: Folder,
    },
    {
      label: "Reports Pending Review",
      value: summary.data?.submittedCount ?? 0,
      trend: "Submitted for review",
      tone: "neutral",
      icon: ClipboardList,
    },
  ];

  function handleResend(id, email) {
    resendInvitation.mutate(id, {
      onSuccess: () => toast.success(`Invitation resent to ${email}`),
      onError: (err) => toast.error(getApiErrorMessage(err)),
    });
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-wrap items-end justify-between gap-2">
        <PageHeader title="Dashboard" description="Overview of your organization" />
      </div>

      <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-5">
        {stats.map((stat) => (
          <StatCard key={stat.label} {...stat} loading={statsLoading} />
        ))}
      </div>

      <div className="flex flex-col gap-4 lg:flex-row">
        <SectionCard
          title="Report Status Overview"
          description={statusTotal > 0 ? `${statusTotal} reports this week` : "No reports yet this week"}
          className="min-w-0 flex-1"
        >
          {summary.isPending ? (
            <div className="flex items-center justify-center gap-2 py-6 text-sm text-slate-500">
              <Loader2 className="size-4 animate-spin" />
              Loading…
            </div>
          ) : (
            <div className="flex flex-col gap-3">
              {statusBars.map((row) => (
                <div key={row.label} className="flex items-center gap-4">
                  <p className="w-[120px] shrink-0 text-xs text-slate-600">{row.label}</p>
                  <div className="h-2.5 flex-1 overflow-hidden rounded-full bg-slate-100">
                    <div
                      className={`h-full rounded-full ${row.className}`}
                      style={{ width: statusTotal > 0 ? `${(row.value / statusTotal) * 100}%` : "0%" }}
                    />
                  </div>
                  <p className="w-7 shrink-0 text-right text-[13px] font-semibold text-slate-900">{row.value}</p>
                </div>
              ))}
            </div>
          )}
        </SectionCard>

        <SectionCard
          title="User Distribution"
          description="By role · active users only"
          className="w-full lg:w-[400px] lg:shrink-0"
        >
          <div className="flex items-center gap-6">
            <DonutChart gradient={userDistribution.gradient} total={userDistribution.total} />
            <div className="flex flex-1 flex-col gap-3.5">
              {userDistribution.rows.map((row) => (
                <div key={row.label} className="flex items-center gap-2">
                  <span className="size-2 shrink-0 rounded-full" style={{ backgroundColor: ROLE_COLORS[row.role] }} />
                  <p className="flex-1 text-xs text-slate-600">{row.label}</p>
                  <p className="text-[13px] font-semibold text-slate-900">{row.value}</p>
                </div>
              ))}
            </div>
          </div>
        </SectionCard>
      </div>

      <div className="flex flex-col rounded-[10px] border border-slate-200 bg-white">
        <div className="flex items-center justify-between gap-2 px-5 py-4">
          <div className="flex flex-col gap-1">
            <p className="text-[15px] font-bold text-slate-900">Pending Invitations</p>
            <p className="text-xs text-slate-500">
              {pendingInvitations.data?.totalElements ?? 0} invitations awaiting acceptance
            </p>
          </div>
          <ViewAllLink to="/admin/users" />
        </div>
        <div className="overflow-x-auto">
          <table className="w-full min-w-[640px] table-fixed border-collapse text-[13px]">
            <thead>
              <tr className="bg-slate-100 text-left text-[11px] font-semibold text-slate-600">
                <th className="px-5 py-2.5 font-semibold">Email</th>
                <th className="w-[160px] px-5 py-2.5 font-semibold">Role</th>
                <th className="w-[130px] px-5 py-2.5 font-semibold">Invitation Sent</th>
                <th className="w-[80px] px-5 py-2.5 text-center font-semibold">Action</th>
              </tr>
            </thead>
            <tbody>
              {(pendingInvitations.data?.content ?? []).map((invite) => (
                <tr key={invite.id} className="border-t border-slate-200">
                  <td className="truncate px-5 py-3 font-semibold text-slate-900">{invite.email}</td>
                  <td className="truncate px-5 py-3 text-slate-600">{ROLE_LABELS[invite.role] ?? invite.role}</td>
                  <td className="whitespace-nowrap px-5 py-3 text-slate-500">{formatDate(invite.createdAt)}</td>
                  <td className="px-5 py-3 text-center">
                    <Button
                      type="button"
                      variant="outline"
                      size="sm"
                      disabled={resendInvitation.isPending}
                      onClick={() => handleResend(invite.id, invite.email)}
                      className="h-7 px-2.5 text-[11px] font-semibold"
                    >
                      Resend
                    </Button>
                  </td>
                </tr>
              ))}
              {!pendingInvitations.isPending && (pendingInvitations.data?.content ?? []).length === 0 && (
                <tr>
                  <td colSpan={4} className="px-5 py-8 text-center text-sm text-slate-500">
                    No pending invitations.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      <div className="flex flex-col gap-4 lg:flex-row">
        <div className="flex w-full flex-col gap-1 rounded-[10px] border border-slate-200 bg-white p-5 lg:w-[360px] lg:shrink-0">
          <p className="text-[15px] font-bold text-slate-900">Recent Activity</p>
          {activityFeed.isPending ? (
            <div className="flex items-center justify-center gap-2 py-6 text-sm text-slate-500">
              <Loader2 className="size-4 animate-spin" />
              Loading…
            </div>
          ) : (activityFeed.data?.content ?? []).length === 0 ? (
            <p className="py-6 text-center text-sm text-slate-500">No recent activity.</p>
          ) : (
            (activityFeed.data?.content ?? []).map((item, i, arr) => {
              const Icon = ACTIVITY_ICON[item.action] ?? UserPlus;
              const detail =
                item.type === "SUBMISSION"
                  ? `${item.memberName} submitted ${item.projectName} (Week of ${formatDate(item.weekStartDate)})`
                  : `${item.actorName} ${item.action === "APPROVED" ? "approved" : "requested changes on"} ${item.memberName}'s report`;
              return (
                <div
                  key={`${item.reportId}-${item.occurredAt}-${i}`}
                  className={`flex gap-3 py-3 ${i < arr.length - 1 ? "border-b border-slate-200" : ""}`}
                >
                  <div className="flex size-[30px] shrink-0 items-center justify-center rounded-full bg-slate-100">
                    <Icon className="size-4 text-slate-500" />
                  </div>
                  <div className="flex flex-col gap-0.5">
                    <p className="text-xs font-semibold text-slate-900">
                      {item.type === "SUBMISSION" ? "Report submitted" : "Report reviewed"}
                    </p>
                    <p className="text-xs text-slate-600">{detail}</p>
                    <p className="text-[11px] text-slate-400">{timeAgo(item.occurredAt)}</p>
                  </div>
                </div>
              );
            })
          )}
        </div>

        <div className="flex min-w-0 flex-1 flex-col rounded-[10px] border border-slate-200 bg-white">
          <div className="flex items-center justify-between gap-2 p-5">
            <p className="text-[15px] font-bold text-slate-900">Recent Reports</p>
            <ViewAllLink to="/admin/reports" />
          </div>
          {recentReports.isPending ? (
            <div className="flex items-center justify-center gap-2 py-6 text-sm text-slate-500">
              <Loader2 className="size-4 animate-spin" />
              Loading…
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full min-w-[480px] table-fixed border-collapse text-xs">
                <thead>
                  <tr className="bg-slate-100 text-left text-[11px] font-semibold text-slate-600">
                    <th className="w-[150px] px-5 py-2.5 font-semibold">Employee</th>
                    <th className="px-5 py-2.5 font-semibold">Project</th>
                    <th className="w-[100px] px-5 py-2.5 font-semibold">Week</th>
                    <th className="w-[150px] px-5 py-2.5 font-semibold">Status</th>
                  </tr>
                </thead>
                <tbody>
                  {(recentReports.data?.content ?? []).map((row) => (
                    <tr key={row.report.id} className="border-t border-slate-200">
                      <td className="truncate px-5 py-3 font-semibold text-slate-900">{row.ownerName}</td>
                      <td className="truncate px-5 py-3 text-slate-600">{row.report.projectName}</td>
                      <td className="whitespace-nowrap px-5 py-3 text-slate-600">
                        {formatDate(row.report.weekStartDate)}
                      </td>
                      <td className="px-5 py-3">
                        <StatusPill status={row.report.status} />
                      </td>
                    </tr>
                  ))}
                  {(recentReports.data?.content ?? []).length === 0 && (
                    <tr>
                      <td colSpan={4} className="px-5 py-8 text-center text-sm text-slate-500">
                        No reports yet.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>

      <div className="flex flex-col gap-4 rounded-[10px] border border-slate-200 bg-white p-5">
        <div className="flex items-center justify-between gap-2">
          <p className="text-[15px] font-bold text-slate-900">Project Overview</p>
          <ViewAllLink to="/projects" />
        </div>
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center">
          <div className="flex w-full flex-col gap-2 sm:w-[200px] sm:shrink-0">
            <p className="text-[13px] font-semibold text-slate-900">
              {totalProjects.data?.totalElements ?? 0} total projects
            </p>
            <div className="flex items-center gap-2 text-xs">
              <span className="size-1.5 rounded-full bg-emerald-600" />
              <span className="text-emerald-700">{activeProjects.data?.totalElements ?? 0} active</span>
              <span className="text-slate-500">
                · {(totalProjects.data?.totalElements ?? 0) - (activeProjects.data?.totalElements ?? 0)} other
              </span>
            </div>
          </div>
          {(activeProjects.data?.content ?? []).map((project) => (
            <div
              key={project.id}
              className="flex min-w-0 flex-1 flex-col gap-2 border-t border-slate-200 pt-4 sm:border-t-0 sm:border-l sm:pt-0 sm:pl-5"
            >
              <div className="flex items-center justify-between gap-2">
                <p className="truncate text-xs font-semibold text-slate-900">{project.name}</p>
                <span className="inline-flex shrink-0 items-center gap-1.5 rounded bg-emerald-50 px-2 py-1 text-[11px] font-semibold text-emerald-700">
                  <span className="size-1.5 rounded-full bg-current" />
                  Active
                </span>
              </div>
              <p className="truncate text-[11px] text-slate-500">{project.memberCount} members</p>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

const STATUS_PILL = {
  SUBMITTED: "bg-blue-50 text-blue-600",
  APPROVED: "bg-emerald-50 text-emerald-700",
  NEEDS_CORRECTION: "bg-red-50 text-red-700",
  DRAFT: "bg-slate-100 text-slate-600",
};

function StatusPill({ status }) {
  return (
    <span
      className={`inline-flex items-center gap-1.5 whitespace-nowrap rounded px-2 py-1 text-[11px] font-semibold ${
        STATUS_PILL[status] ?? "bg-slate-100 text-slate-600"
      }`}
    >
      <span className="size-1.5 rounded-full bg-current" />
      {REPORT_STATUSES[status]?.label ?? status}
    </span>
  );
}
