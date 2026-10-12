import { useMemo } from "react";
import { Link } from "react-router-dom";
import { Loader2 } from "lucide-react";
import PageHeader from "@/components/layout/PageHeader";
import EmptyState from "@/components/feedback/EmptyState";
import { Separator } from "@/components/ui/separator";
import { useTeamMembers } from "@/hooks/useManagerReports";
import { useProjects, useProjectsMembers } from "@/hooks/useProjects";
import { getApiErrorMessage } from "@/utils/apiError";

function initialsOf(name) {
  if (!name) return "?";
  const parts = name.trim().split(/\s+/);
  const first = parts[0]?.[0] ?? "";
  const last = parts.length > 1 ? parts[parts.length - 1][0] : "";
  return (first + last).toUpperCase();
}

function MemberCard({ member, projectNames }) {
  const counts = member.reportCountsByStatus ?? {};
  return (
    <div className="flex flex-col gap-4 rounded-xl border border-slate-200 bg-white p-5 shadow-[0_1px_1.5px_rgba(0,0,0,0.04)]">
      <div className="flex flex-wrap items-start justify-between gap-2">
        <div className="flex min-w-0 items-center gap-3">
          <div className="flex size-12 shrink-0 items-center justify-center rounded-full bg-[#1a0b2e] text-base font-bold text-[#ffb7a5]">
            {initialsOf(member.name)}
          </div>
          <div className="flex min-w-0 flex-col gap-0.5">
            <p className="truncate text-[15px] font-bold text-slate-900">{member.name}</p>
            <p className="truncate text-xs text-slate-600">{member.email}</p>
          </div>
        </div>
        <span
          className={`shrink-0 rounded-md px-2 py-1 text-[10px] font-semibold whitespace-nowrap ${
            member.active ? "bg-emerald-50 text-emerald-700" : "bg-red-50 text-red-700"
          }`}
        >
          {member.active ? "ACTIVE" : "SUSPENDED"}
        </span>
      </div>

      <Separator />

      <div className="flex flex-col gap-2">
        <p className="text-[11px] font-semibold text-slate-400">ASSIGNED PROJECTS</p>
        {projectNames.length === 0 ? (
          <p className="text-[11px] text-slate-500">No active project assignments.</p>
        ) : (
          <div className="flex flex-wrap gap-1.5">
            {projectNames.map((name) => (
              <span
                key={name}
                className="rounded border border-slate-200 bg-[#f8f8fa] px-2 py-1 text-[11px] text-slate-600"
              >
                {name}
              </span>
            ))}
          </div>
        )}
      </div>

      <div className="flex flex-wrap items-center gap-x-4 gap-y-1 border-t border-slate-200 pt-3 text-[11px] text-slate-600">
        <span className="font-semibold text-emerald-700">{counts.APPROVED ?? 0} approved</span>
        <span className="font-semibold text-blue-600">{counts.SUBMITTED ?? 0} pending</span>
        <span className="font-semibold text-amber-600">{counts.NEEDS_CORRECTION ?? 0} needs correction</span>
        <span className="font-semibold text-slate-500">{counts.DRAFT ?? 0} draft</span>
      </div>

      <div className="flex border-t border-slate-200 pt-2 text-[13px]">
        <Link to={`/team?member=${member.id}`} className="font-bold text-[#1a0b2e] underline">
          View Reports
        </Link>
      </div>
    </div>
  );
}

export default function TeamMembers() {
  const teamMembers = useTeamMembers({ size: 200 });
  const activeProjects = useProjects({ status: "ACTIVE", size: 100 });
  const projectRows = useMemo(() => activeProjects.data?.content ?? [], [activeProjects.data]);
  const projectIds = useMemo(() => projectRows.map((p) => p.id), [projectRows]);
  const memberQueries = useProjectsMembers(projectIds);

  const isPending = teamMembers.isPending || activeProjects.isPending || memberQueries.some((q) => q.isPending);

  // userId -> [project names] -- there's no "my projects" field on TeamMemberResponse, so this is
  // built from each active project's own roster rather than left blank.
  const projectsByUserId = useMemo(() => {
    const map = new Map();
    projectRows.forEach((project, i) => {
      const members = memberQueries[i]?.data ?? [];
      members.forEach((m) => {
        const existing = map.get(m.userId) ?? [];
        existing.push(project.name);
        map.set(m.userId, existing);
      });
    });
    return map;
  }, [projectRows, memberQueries]);

  const members = teamMembers.data ?? [];

  return (
    <div className="flex flex-col gap-6">
      <PageHeader title="Team Members" />

      <div className="flex flex-wrap items-center justify-between gap-2">
        <div className="flex items-center gap-2">
          <p className="text-base font-bold text-slate-900">Direct Reports</p>
          <span className="rounded-full bg-[#1a0b2e] px-2 py-0.5 text-[11px] font-bold text-[#ffb7a5]">
            {members.length} Members
          </span>
        </div>
        <p className="text-xs text-slate-600">* Members are added by HR Administrators</p>
      </div>

      {isPending ? (
        <div className="flex items-center justify-center gap-2 py-16 text-sm text-slate-500">
          <Loader2 className="size-4 animate-spin" />
          Loading team…
        </div>
      ) : teamMembers.isError ? (
        <EmptyState title="Couldn't load team members" description={getApiErrorMessage(teamMembers.error)} />
      ) : members.length === 0 ? (
        <EmptyState title="No team members" description="No team members have been assigned to you yet." />
      ) : (
        <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
          {members.map((member) => (
            <MemberCard key={member.id} member={member} projectNames={projectsByUserId.get(member.id) ?? []} />
          ))}
        </div>
      )}
    </div>
  );
}
