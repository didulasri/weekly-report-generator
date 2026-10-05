import { useMemo, useState } from "react";
import PageHeader from "@/components/layout/PageHeader";
import { Button } from "@/components/ui/button";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

// Static mock rows -- UI-only checkpoint, no team/report service wired yet. Status labels/colors
// are deliberately NOT StatusBadge/REPORT_STATUSES here -- the Figma manager-facing table uses
// different phrasing for the same ReportStatus values than the employee-facing one (e.g. blue
// "SUBMITTED" here vs. the employee view's amber "PENDING REVIEW" for the same status), since the
// audience's question differs ("what needs my action" vs "where is my report in review").
const ALL_REPORTS = [
  { id: "r1", employee: "Marcus Vance", initials: "MV", project: "Acme Platform Integration", week: "Week 42", status: "SUBMITTED", submitted: "Oct 20, 2024" },
  { id: "r2", employee: "Sarah Connor", initials: "SC", project: "Global Billing Redesign", week: "Week 42", status: "APPROVED", submitted: "Oct 19, 2024" },
  { id: "r3", employee: "Alex Mercer", initials: "AM", project: "Security Audit Patching", week: "Week 42", status: "NEEDS_CORRECTION", submitted: "Oct 18, 2024" },
  { id: "r4", employee: "Helena Rostova", initials: "HR", project: "Global Billing Redesign", week: "Week 41", status: "APPROVED", submitted: "Oct 15, 2024" },
  { id: "r5", employee: "Linus Torvalds", initials: "LT", project: "Kernel Upgrade Phase 2", week: "Week 42", status: "DRAFT", submitted: "---" },
  { id: "r6", employee: "Ada Lovelace", initials: "AL", project: "Analytical Engine UI", week: "Week 42", status: "SUBMITTED", submitted: "Oct 20, 2024" },
  { id: "r7", employee: "Bill Gates", initials: "BG", project: "MS-DOS Legacy Support", week: "Week 42", status: "NEEDS_CORRECTION", submitted: "Oct 19, 2024" },
  { id: "r8", employee: "Grace Hopper", initials: "GH", project: "COBOL Compiler Audit", week: "Week 42", status: "APPROVED", submitted: "Oct 20, 2024" },
];

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

const WEEKS = ["Week 42", "Week 41", "Week 40"];

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

export default function TeamReports() {
  const [member, setMember] = useState("ALL");
  const [project, setProject] = useState("ALL");
  const [status, setStatus] = useState("ALL");
  const [week, setWeek] = useState(WEEKS[0]);

  const members = useMemo(() => [...new Set(ALL_REPORTS.map((r) => r.employee))], []);
  const projects = useMemo(() => [...new Set(ALL_REPORTS.map((r) => r.project))], []);

  const filtered = useMemo(() => {
    return ALL_REPORTS.filter((report) => {
      const matchesMember = member === "ALL" || report.employee === member;
      const matchesProject = project === "ALL" || report.project === project;
      const matchesStatus = status === "ALL" || report.status === status;
      return matchesMember && matchesProject && matchesStatus;
    });
  }, [member, project, status]);

  function resetFilters() {
    setMember("ALL");
    setProject("ALL");
    setStatus("ALL");
    setWeek(WEEKS[0]);
  }

  const hasActiveFilters = member !== "ALL" || project !== "ALL" || status !== "ALL";

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
            {members.map((name) => (
              <SelectItem key={name} value={name}>
                {name}
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
            {projects.map((name) => (
              <SelectItem key={name} value={name}>
                {name}
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

        <Select value={week} onValueChange={setWeek}>
          <SelectTrigger className="h-9 w-fit gap-1.5 text-[13px]">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            {WEEKS.map((option) => (
              <SelectItem key={option} value={option}>
                {option}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>

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
        <div className="overflow-x-auto">
          <table className="w-full min-w-[960px] border-collapse text-[13px]">
            <thead>
              <tr className="bg-slate-50 text-left text-xs font-bold text-slate-600">
                <th className="px-6 py-3.5 font-bold">Employee</th>
                <th className="w-[260px] px-6 py-3.5 font-bold">Project</th>
                <th className="w-[110px] whitespace-nowrap px-6 py-3.5 font-bold">Week</th>
                <th className="w-[160px] px-6 py-3.5 font-bold">Status</th>
                <th className="w-[140px] px-6 py-3.5 font-bold">Submitted Date</th>
                <th className="w-[120px] px-6 py-3.5 text-right font-bold">Actions</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((report) => (
                <tr key={report.id} className="border-t border-slate-200">
                  <td className="px-6 py-3">
                    <div className="flex items-center gap-2.5">
                      <div className="flex size-7 shrink-0 items-center justify-center rounded-full bg-[#1a0b2e] text-[11px] font-bold text-[#ffb7a5]">
                        {report.initials}
                      </div>
                      <span className="truncate font-semibold text-slate-900">{report.employee}</span>
                    </div>
                  </td>
                  <td className="truncate px-6 py-3 text-slate-900">{report.project}</td>
                  <td className="whitespace-nowrap px-6 py-3 text-slate-600">{report.week}</td>
                  <td className="whitespace-nowrap px-6 py-3">
                    <ManagerStatusBadge status={report.status} />
                  </td>
                  <td className="whitespace-nowrap px-6 py-3 text-slate-600">{report.submitted}</td>
                  <td className="px-6 py-3 text-right">
                    {report.status === "SUBMITTED" ? (
                      <Button type="button" size="sm" className="h-7 px-3 text-[12px] font-bold">
                        Review
                      </Button>
                    ) : (
                      <Button type="button" variant="outline" size="sm" className="h-7 px-3 text-[12px] font-semibold">
                        View
                      </Button>
                    )}
                  </td>
                </tr>
              ))}
              {filtered.length === 0 && (
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
          <p className="text-xs text-slate-600">Showing 1-{filtered.length} of 34 reports</p>
          <div className="flex gap-2">
            <Button variant="outline" size="sm" disabled className="h-7 px-3 text-xs font-semibold">
              Previous
            </Button>
            <Button variant="outline" size="sm" className="h-7 px-3 text-xs font-semibold text-slate-900">
              Next
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
}
