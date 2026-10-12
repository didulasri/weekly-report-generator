import { Link, useParams } from "react-router-dom";
import { Loader2 } from "lucide-react";
import EmptyState from "@/components/feedback/EmptyState";
import { Button } from "@/components/ui/button";
import { useCurrentUser } from "@/hooks/useAuth";
import { useProject, useProjectMembers } from "@/hooks/useProjects";
import { useTeamReports } from "@/hooks/useManagerReports";
import { useWorkload } from "@/hooks/useDashboard";
import { getApiErrorMessage } from "@/utils/apiError";

// Same project-status phrasing as ManagerProjects.jsx's table -- kept as its own local copy rather
// than a shared import, consistent with how this codebase treats status badges as page-local
// (see TeamReports.jsx's note on why its ManagerStatusBadge isn't REPORT_STATUSES either).
const STATUS_CONFIG = {
  ACTIVE: { label: "Active", className: "bg-sky-50 text-sky-600 border-sky-200" },
  INACTIVE: { label: "Inactive", className: "bg-slate-100 text-slate-600 border-slate-200" },
  COMPLETED: { label: "Completed", className: "bg-emerald-50 text-emerald-700 border-emerald-200" },
};

const REPORT_STATUS_CONFIG = {
  DRAFT: { label: "Draft", className: "bg-slate-100 text-slate-600 border-slate-200" },
  SUBMITTED: { label: "Submitted", className: "bg-blue-50 text-blue-700 border-blue-100" },
  NEEDS_CORRECTION: { label: "Needs Correction", className: "bg-amber-50 text-amber-700 border-amber-200" },
  APPROVED: { label: "Approved", className: "bg-emerald-50 text-emerald-700 border-emerald-200" },
};

function StatusBadge({ status, config }) {
  const entry = config[status];
  return (
    <span
      className={`inline-flex items-center rounded border px-2 py-1 text-[11px] font-semibold uppercase ${
        entry?.className ?? "bg-slate-50 text-slate-600 border-slate-200"
      }`}
    >
      {entry?.label ?? status}
    </span>
  );
}

function initialsOf(name) {
  if (!name) return "?";
  const parts = name.trim().split(/\s+/);
  const first = parts[0]?.[0] ?? "";
  const last = parts.length > 1 ? parts[parts.length - 1][0] : "";
  return (first + last).toUpperCase();
}

function formatDate(isoString) {
  if (!isoString) return "—";
  return new Date(isoString).toLocaleDateString("en-US", { month: "short", day: "numeric", year: "numeric" });
}

function Panel({ title, children }) {
  return (
    <div className="flex flex-col gap-3 rounded-xl border border-slate-200 bg-white p-6 shadow-[0_1px_1.5px_rgba(0,0,0,0.04)]">
      <p className="text-[15px] font-bold text-slate-900">{title}</p>
      {children}
    </div>
  );
}

export default function ProjectDetail() {
  const { projectId } = useParams();
  const currentUser = useCurrentUser();
  const projectQuery = useProject(projectId);
  const membersQuery = useProjectMembers(projectId);
  const workloadQuery = useWorkload({ projectId: Number(projectId) });
  const reportsQuery = useTeamReports({ projectId: Number(projectId), size: 5 });

  const isPending = projectQuery.isPending || membersQuery.isPending || workloadQuery.isPending;

  if (isPending) {
    return (
      <div className="flex items-center justify-center gap-2 py-16 text-sm text-slate-500">
        <Loader2 className="size-4 animate-spin" />
        Loading project…
      </div>
    );
  }

  if (projectQuery.isError) {
    const notFound = projectQuery.error?.response?.status === 404;
    return (
      <EmptyState
        title={notFound ? "Project not found" : "Couldn't load this project"}
        description={notFound ? "This project doesn't exist or may have been removed." : getApiErrorMessage(projectQuery.error)}
        action={
          <Button asChild className="h-9 px-3.5 text-[13px] font-semibold">
            <Link to="/projects">Back to Projects</Link>
          </Button>
        }
      />
    );
  }

  const project = projectQuery.data;
  const members = membersQuery.data ?? [];
  const workload = workloadQuery.data?.find((row) => row.projectId === Number(projectId));
  const reports = reportsQuery.data?.content ?? [];
  const isManager = currentUser.data?.role === "MANAGER";

  const reportCount = workload?.reportCount ?? 0;
  const progressPercent = project.memberCount > 0 ? Math.min(100, Math.round((reportCount / project.memberCount) * 100)) : 0;

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <p className="flex flex-wrap items-center gap-2 text-[13px]">
          <Link to="/projects" className="font-medium text-slate-500 hover:text-slate-700">
            Projects
          </Link>
          <span className="text-slate-400">/</span>
          <span className="font-semibold text-slate-900">{project.name}</span>
        </p>
        <StatusBadge status={project.status} config={STATUS_CONFIG} />
      </div>

      <div className="flex flex-col gap-6 lg:flex-row">
        <div className="flex min-w-0 flex-1 flex-col gap-6">
          <Panel title="Project Overview">
            <div className="grid grid-cols-2 gap-4 sm:grid-cols-3">
              <div className="flex flex-col gap-1">
                <p className="text-[11px] font-semibold tracking-wide text-slate-400 uppercase">Team Members</p>
                <p className="text-sm font-semibold text-slate-900">{project.memberCount}</p>
              </div>
              <div className="flex flex-col gap-1">
                <p className="text-[11px] font-semibold tracking-wide text-slate-400 uppercase">Reports This Week</p>
                <p className="text-sm font-semibold text-slate-900">{reportCount}</p>
              </div>
              <div className="flex flex-col gap-1">
                <p className="text-[11px] font-semibold tracking-wide text-slate-400 uppercase">Weekly Progress</p>
                <p className="text-sm font-semibold text-slate-900">
                  {reportCount}/{project.memberCount} ({progressPercent}%)
                </p>
              </div>
            </div>
            <div className="flex flex-col gap-1 border-t border-slate-200 pt-4">
              <p className="text-xs font-semibold text-slate-600">Description</p>
              <p className="text-sm whitespace-pre-wrap text-slate-900">{project.description || "—"}</p>
            </div>
          </Panel>

          <Panel title="Recent Reports">
            {reportsQuery.isError ? (
              <p className="text-sm text-slate-500">{getApiErrorMessage(reportsQuery.error)}</p>
            ) : reports.length === 0 ? (
              <p className="text-sm text-slate-500">No reports for this project yet.</p>
            ) : (
              <div className="flex flex-col">
                {reports.map((row, i) => {
                  const inner = (
                    <div className="flex flex-wrap items-center justify-between gap-2">
                      <div className="flex items-center gap-2.5">
                        <div className="flex size-7 shrink-0 items-center justify-center rounded-full bg-[#1a0b2e] text-[11px] font-bold text-[#ffb7a5]">
                          {initialsOf(row.ownerName)}
                        </div>
                        <div>
                          <p className="text-[13px] font-semibold text-slate-900">{row.ownerName}</p>
                          <p className="text-xs text-slate-500">Week of {formatDate(row.report.weekStartDate)}</p>
                        </div>
                      </div>
                      <StatusBadge status={row.report.status} config={REPORT_STATUS_CONFIG} />
                    </div>
                  );
                  return (
                    <div
                      key={row.report.id}
                      className={`py-3 ${i < reports.length - 1 ? "border-b border-slate-200" : ""}`}
                    >
                      {isManager ? (
                        <Link to={`/team/reports/${row.report.id}`} className="block hover:opacity-80">
                          {inner}
                        </Link>
                      ) : (
                        inner
                      )}
                    </div>
                  );
                })}
              </div>
            )}
          </Panel>
        </div>

        <div className="flex w-full flex-col gap-6 lg:w-[380px] lg:shrink-0">
          <Panel title={`Team (${members.length})`}>
            {members.length === 0 ? (
              <p className="text-sm text-slate-500">No active members assigned.</p>
            ) : (
              <div className="flex flex-col">
                {members.map((member, i) => (
                  <div
                    key={member.userId}
                    className={`flex items-center gap-2.5 py-2.5 ${i < members.length - 1 ? "border-b border-slate-200" : ""}`}
                  >
                    <div className="flex size-8 shrink-0 items-center justify-center rounded-full bg-[#1a0b2e] text-[11px] font-bold text-[#ffb7a5]">
                      {initialsOf(member.name)}
                    </div>
                    <div className="flex min-w-0 flex-col">
                      <p className="truncate text-[13px] font-semibold text-slate-900">{member.name}</p>
                      <p className="truncate text-xs text-slate-500">{member.email}</p>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </Panel>
        </div>
      </div>
    </div>
  );
}
