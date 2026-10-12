import { useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { Loader2 } from "lucide-react";
import PageHeader from "@/components/layout/PageHeader";
import EmptyState from "@/components/feedback/EmptyState";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { useTeamMembers, useTeamReports } from "@/hooks/useManagerReports";
import { useProjects } from "@/hooks/useProjects";
import { getApiErrorMessage } from "@/utils/apiError";
import { mondayOf } from "@/utils/weekDates";

// Deliberately NOT StatusBadge/REPORT_STATUSES here -- the manager-facing table uses different
// phrasing for the same ReportStatus values than the employee-facing one (e.g. blue "Submitted"
// here vs. the employee view's amber "Pending Review" for the same status), since the audience's
// question differs ("what needs my action" vs "where is my report in review").
const STATUS_CONFIG = {
  DRAFT: { label: "Draft", className: "bg-slate-100 text-slate-600 border-slate-200" },
  SUBMITTED: { label: "Submitted", className: "bg-blue-50 text-blue-700 border-blue-100" },
  NEEDS_CORRECTION: { label: "Needs Correction", className: "bg-amber-50 text-amber-700 border-amber-200" },
  APPROVED: { label: "Approved", className: "bg-emerald-50 text-emerald-700 border-emerald-200" },
};

const STATUS_FILTERS = [
  { value: "ALL", label: "Status: All" },
  { value: "DRAFT", label: "Draft" },
  { value: "SUBMITTED", label: "Submitted" },
  { value: "NEEDS_CORRECTION", label: "Needs Correction" },
  { value: "APPROVED", label: "Approved" },
];

const PAGE_SIZE = 10;

function ManagerStatusBadge({ status }) {
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

function formatDate(isoString) {
  if (!isoString) return "—";
  return new Date(isoString).toLocaleDateString("en-US", { month: "short", day: "numeric", year: "numeric" });
}

export default function TeamReports() {
  const [searchParams] = useSearchParams();
  const [member, setMember] = useState(() => searchParams.get("member") ?? "ALL");
  const [project, setProject] = useState("ALL");
  const [status, setStatus] = useState("ALL");
  const [weekInput, setWeekInput] = useState("");
  const [page, setPage] = useState(0);

  const teamMembers = useTeamMembers({ size: 200 });
  const projects = useProjects({ status: "ACTIVE", size: 100 });

  const week = weekInput ? mondayOf(weekInput) : null;

  const filterKey = `${member}|${project}|${status}|${weekInput}`;
  const [pageFilterKey, setPageFilterKey] = useState(filterKey);
  if (filterKey !== pageFilterKey) {
    setPageFilterKey(filterKey);
    setPage(0);
  }

  const { data, isPending, isError, error, isFetching } = useTeamReports({
    status: status === "ALL" ? undefined : status,
    projectId: project === "ALL" ? undefined : Number(project),
    userId: member === "ALL" ? undefined : Number(member),
    startDate: week?.start,
    endDate: week?.end,
    page,
    size: PAGE_SIZE,
  });

  const rows = data?.content ?? [];
  const totalElements = data?.totalElements ?? 0;
  const hasActiveFilters = member !== "ALL" || project !== "ALL" || status !== "ALL" || weekInput !== "";

  function resetFilters() {
    setMember("ALL");
    setProject("ALL");
    setStatus("ALL");
    setWeekInput("");
    setPage(0);
  }

  const rangeStart = totalElements === 0 ? 0 : page * PAGE_SIZE + 1;
  const rangeEnd = Math.min(totalElements, page * PAGE_SIZE + rows.length);

  return (
    <div className="flex flex-col gap-4">
      <PageHeader title="Team Reports" />

      <div className="flex flex-wrap items-center gap-3 rounded-xl border border-slate-200 bg-white p-4">
        <p className="text-[13px] font-semibold text-slate-600">Filters:</p>

        <Select value={member} onValueChange={setMember}>
          <SelectTrigger className="h-9 w-fit gap-1.5 text-[13px]">
            <SelectValue placeholder="All Team Members" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="ALL">All Team Members</SelectItem>
            {(teamMembers.data ?? []).map((m) => (
              <SelectItem key={m.id} value={String(m.id)}>
                {m.name}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>

        <Select value={project} onValueChange={setProject}>
          <SelectTrigger className="h-9 w-fit gap-1.5 text-[13px]">
            <SelectValue placeholder="All Projects" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="ALL">All Projects</SelectItem>
            {(projects.data?.content ?? []).map((p) => (
              <SelectItem key={p.id} value={String(p.id)}>
                {p.name}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>

        <Select value={status} onValueChange={setStatus}>
          <SelectTrigger className="h-9 w-fit gap-1.5 text-[13px]">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            {STATUS_FILTERS.map((option) => (
              <SelectItem key={option.value} value={option.value}>
                {option.label}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>

        <Input
          type="date"
          value={weekInput}
          onChange={(e) => setWeekInput(e.target.value)}
          className="h-9 w-fit text-[13px]"
          title="Filter by reporting week (snaps to that week's Monday)"
        />

        {hasActiveFilters && (
          <button
            type="button"
            onClick={resetFilters}
            className="ml-auto text-[13px] font-semibold text-slate-600 underline hover:text-slate-900"
          >
            Reset Filters
          </button>
        )}
      </div>

      <div className="flex flex-col rounded-xl border border-slate-200 bg-white shadow-[0_1px_1.5px_rgba(0,0,0,0.04)]">
        {isPending ? (
          <div className="flex items-center justify-center gap-2 px-4 py-16 text-sm text-slate-500">
            <Loader2 className="size-4 animate-spin" />
            Loading reports…
          </div>
        ) : isError ? (
          <EmptyState title="Couldn't load reports" description={getApiErrorMessage(error)} />
        ) : (
          <>
            <div className="overflow-x-auto">
              <table className="w-full min-w-[960px] border-collapse text-[13px]">
                <thead>
                  <tr className="bg-slate-50 text-left text-xs font-bold text-slate-600">
                    <th className="px-6 py-3.5 font-bold">Employee</th>
                    <th className="w-[260px] px-6 py-3.5 font-bold">Project</th>
                    <th className="w-[110px] whitespace-nowrap px-6 py-3.5 font-bold">Week</th>
                    <th className="w-[160px] px-6 py-3.5 font-bold">Status</th>
                    <th className="w-[140px] px-6 py-3.5 font-bold">Last Updated</th>
                    <th className="w-[120px] px-6 py-3.5 text-right font-bold">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {rows.map((row) => (
                    <tr key={row.report.id} className="border-t border-slate-200">
                      <td className="px-6 py-3">
                        <div className="flex items-center gap-2.5">
                          <div className="flex size-7 shrink-0 items-center justify-center rounded-full bg-[#1a0b2e] text-[11px] font-bold text-[#ffb7a5]">
                            {initialsOf(row.ownerName)}
                          </div>
                          <span className="truncate font-semibold text-slate-900">{row.ownerName}</span>
                        </div>
                      </td>
                      <td className="truncate px-6 py-3 text-slate-900">{row.report.projectName}</td>
                      <td className="whitespace-nowrap px-6 py-3 text-slate-600">{formatDate(row.report.weekStartDate)}</td>
                      <td className="whitespace-nowrap px-6 py-3">
                        <ManagerStatusBadge status={row.report.status} />
                      </td>
                      <td className="whitespace-nowrap px-6 py-3 text-slate-600">{formatDate(row.report.updatedAt)}</td>
                      <td className="px-6 py-3 text-right">
                        {row.report.status === "SUBMITTED" ? (
                          <Button type="button" size="sm" className="h-7 px-3 text-[12px] font-bold" asChild>
                            <Link to={`/team/reports/${row.report.id}`}>Review</Link>
                          </Button>
                        ) : (
                          <Button type="button" variant="outline" size="sm" className="h-7 px-3 text-[12px] font-semibold" asChild>
                            <Link to={`/team/reports/${row.report.id}`}>View</Link>
                          </Button>
                        )}
                      </td>
                    </tr>
                  ))}
                  {rows.length === 0 && (
                    <tr>
                      <td colSpan={6} className="px-6 py-10 text-center text-sm text-slate-500">
                        No reports match your filters.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>

            <div className="flex flex-wrap items-center justify-between gap-3 border-t border-slate-200 bg-slate-50 p-4">
              <p className="text-xs text-slate-600">
                {totalElements === 0 ? "No reports" : `Showing ${rangeStart}-${rangeEnd} of ${totalElements} reports`}
                {isFetching && !isPending && " · refreshing…"}
              </p>
              <div className="flex gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  disabled={page === 0}
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                  className="h-7 px-3 text-xs font-semibold"
                >
                  Previous
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  disabled={data?.last ?? true}
                  onClick={() => setPage((p) => p + 1)}
                  className="h-7 px-3 text-xs font-semibold text-slate-900"
                >
                  Next
                </Button>
              </div>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
