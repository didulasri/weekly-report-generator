import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { Download, Loader2 } from "lucide-react";
import { toast } from "sonner";
import PageHeader from "@/components/layout/PageHeader";
import EmptyState from "@/components/feedback/EmptyState";
import { Button } from "@/components/ui/button";
import { useWorkload } from "@/hooks/useDashboard";
import { useExportProjectsOverview, useProjects, useProjectsMembers } from "@/hooks/useProjects";
import { getApiErrorMessage } from "@/utils/apiError";

const STATUS_CONFIG = {
  ACTIVE: { label: "Active", className: "bg-sky-50 text-sky-600 border-sky-200" },
  INACTIVE: { label: "Inactive", className: "bg-slate-100 text-slate-600 border-slate-200" },
  COMPLETED: { label: "Completed", className: "bg-emerald-50 text-emerald-700 border-emerald-200" },
};

const TABS = [
  { value: "ALL", label: "All" },
  { value: "ACTIVE", label: "Active" },
  { value: "INACTIVE", label: "Inactive" },
  { value: "COMPLETED", label: "Completed" },
];

function StatusBadge({ status }) {
  const config = STATUS_CONFIG[status];
  return (
    <span
      className={`inline-flex items-center rounded border px-2 py-1 text-[11px] font-semibold uppercase ${
        config?.className ?? "bg-slate-50 text-slate-600 border-slate-200"
      }`}
    >
      {config?.label ?? status}
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

function TeamAvatars({ members }) {
  const shown = members.slice(0, 3);
  const extra = members.length - shown.length;
  return (
    <div className="flex items-center">
      {shown.map((m, i) => (
        <div
          key={m.userId}
          title={m.name}
          className="flex size-6 items-center justify-center rounded-full border-2 border-white bg-[#1a0b2e] text-[9px] font-bold text-[#ffb7a5]"
          style={{ marginLeft: i === 0 ? 0 : -6 }}
        >
          {initialsOf(m.name)}
        </div>
      ))}
      {extra > 0 && (
        <div
          className="flex size-6 items-center justify-center rounded-full border-2 border-white bg-slate-100 text-[9px] font-bold text-slate-600"
          style={{ marginLeft: -6 }}
        >
          +{extra}
        </div>
      )}
      {members.length === 0 && <span className="text-xs text-slate-400">No members</span>}
    </div>
  );
}

// Reports submitted this week against team size -- the closest real analog to "compliance" the
// available endpoints support. DashboardSummaryResponse.compliance is org-wide only; there's no
// per-project on-time/expected breakdown, so this is "how many of this project's members reported
// this week" rather than true on-time compliance.
function WeeklyProgressBar({ reportCount, memberCount }) {
  const percent = memberCount > 0 ? Math.min(100, Math.round((reportCount / memberCount) * 100)) : 0;
  const isComplete = percent >= 100;
  return (
    <div className="flex items-center gap-2">
      <div className="h-1.5 w-[70px] overflow-hidden rounded-full bg-slate-200">
        <div
          className={`h-full rounded-full ${isComplete ? "bg-emerald-500" : "bg-amber-500"}`}
          style={{ width: `${percent}%` }}
        />
      </div>
      <p className={`text-xs font-bold ${isComplete ? "text-emerald-500" : "text-amber-600"}`}>
        {reportCount}/{memberCount}
      </p>
    </div>
  );
}

function downloadBlob(blob, filename) {
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}

export default function ManagerProjects() {
  const [tab, setTab] = useState("ALL");

  const projectsQuery = useProjects({ size: 100 });
  const workloadQuery = useWorkload();
  const exportOverview = useExportProjectsOverview();

  const allProjects = useMemo(() => projectsQuery.data?.content ?? [], [projectsQuery.data]);
  const projectIds = useMemo(() => allProjects.map((p) => p.id), [allProjects]);
  const memberQueries = useProjectsMembers(projectIds);

  const isPending = projectsQuery.isPending || workloadQuery.isPending || memberQueries.some((q) => q.isPending);

  const workloadByProjectId = useMemo(() => {
    const map = new Map();
    (workloadQuery.data ?? []).forEach((row) => map.set(row.projectId, row));
    return map;
  }, [workloadQuery.data]);

  const membersByProjectId = useMemo(() => {
    const map = new Map();
    allProjects.forEach((project, i) => {
      map.set(project.id, memberQueries[i]?.data ?? []);
    });
    return map;
  }, [allProjects, memberQueries]);

  function tabCount(value) {
    if (value === "ALL") return allProjects.length;
    return allProjects.filter((p) => p.status === value).length;
  }

  const filtered = tab === "ALL" ? allProjects : allProjects.filter((p) => p.status === tab);

  function handleExport() {
    exportOverview.mutate(undefined, {
      onSuccess: (blob) => downloadBlob(blob, "projects-overview.csv"),
      onError: (err) => toast.error(getApiErrorMessage(err)),
    });
  }

  return (
    <div className="flex flex-col gap-4">
      <PageHeader title="Projects" />

      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex flex-wrap items-center gap-2">
          {TABS.map((t) => (
            <button
              key={t.value}
              type="button"
              onClick={() => setTab(t.value)}
              className={`flex items-center gap-2 rounded-lg border px-4 py-2 text-[13px] transition-colors ${
                tab === t.value
                  ? "border-slate-200 bg-white font-bold text-slate-900"
                  : "border-transparent font-medium text-slate-600 hover:bg-white"
              }`}
            >
              {t.label}
              <span
                className={`rounded-full px-1.5 py-0.5 text-[10px] font-bold ${
                  tab === t.value ? "bg-[#1a0b2e] text-[#ffb7a5]" : "bg-slate-200 text-slate-600"
                }`}
              >
                {tabCount(t.value)}
              </span>
            </button>
          ))}
        </div>

        <Button
          type="button"
          variant="outline"
          disabled={exportOverview.isPending}
          onClick={handleExport}
          className="h-9 gap-2 px-4 text-[13px] font-semibold"
        >
          {exportOverview.isPending ? <Loader2 className="size-3.5 animate-spin" /> : <Download className="size-3.5" />}
          Export Overview
        </Button>
      </div>

      <div className="flex flex-col rounded-xl border border-slate-200 bg-white shadow-[0_1px_1.5px_rgba(0,0,0,0.04)]">
        {isPending ? (
          <div className="flex items-center justify-center gap-2 px-4 py-16 text-sm text-slate-500">
            <Loader2 className="size-4 animate-spin" />
            Loading projects…
          </div>
        ) : projectsQuery.isError ? (
          <EmptyState title="Couldn't load projects" description={getApiErrorMessage(projectsQuery.error)} />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[940px] table-fixed border-collapse text-[13px]">
              <thead>
                <tr className="bg-slate-50 text-left text-xs font-bold text-slate-600">
                  <th className="w-[190px] px-4 py-3.5 font-bold">Project Name</th>
                  <th className="px-4 py-3.5 font-bold">Description</th>
                  <th className="w-[90px] px-4 py-3.5 font-bold">Status</th>
                  <th className="w-[100px] px-4 py-3.5 font-bold">Team Members</th>
                  <th className="w-[130px] px-4 py-3.5 text-center font-bold">Reports (This Week)</th>
                  <th className="w-[140px] px-4 py-3.5 font-bold">Weekly Progress</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map((project) => {
                  const workload = workloadByProjectId.get(project.id);
                  const members = membersByProjectId.get(project.id) ?? [];
                  return (
                    <tr key={project.id} className="border-t border-slate-200">
                      <td className="truncate px-4 py-4 font-bold text-slate-900">
                        <Link to={`/projects/${project.id}`} className="hover:underline">
                          {project.name}
                        </Link>
                      </td>
                      <td className="truncate px-4 py-4 text-slate-600">{project.description || "—"}</td>
                      <td className="whitespace-nowrap px-4 py-4">
                        <StatusBadge status={project.status} />
                      </td>
                      <td className="px-4 py-4">
                        <TeamAvatars members={members} />
                      </td>
                      <td className="whitespace-nowrap px-4 py-4 text-center font-semibold text-slate-900">
                        {workload?.reportCount ?? 0} Submissions
                      </td>
                      <td className="whitespace-nowrap px-4 py-4">
                        <WeeklyProgressBar reportCount={workload?.reportCount ?? 0} memberCount={project.memberCount} />
                      </td>
                    </tr>
                  );
                })}
                {filtered.length === 0 && (
                  <tr>
                    <td colSpan={6} className="px-4 py-10 text-center text-sm text-slate-500">
                      No projects in this status.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
