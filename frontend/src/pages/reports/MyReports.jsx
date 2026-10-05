import { useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Plus, Search } from "lucide-react";
import PageHeader from "@/components/layout/PageHeader";
import StatusBadge from "@/components/common/StatusBadge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { MOCK_REPORTS } from "./mockReports";

const STATUS_FILTERS = [
  { value: "ALL", label: "All Statuses" },
  { value: "DRAFT", label: "Draft" },
  { value: "SUBMITTED", label: "Pending Review" },
  { value: "NEEDS_CORRECTION", label: "Needs Correction" },
  { value: "APPROVED", label: "Approved" },
];

const DATE_RANGES = ["Last 30 Days", "Last 90 Days", "This Year", "All Time"];

export default function MyReports() {
  const navigate = useNavigate();
  const [search, setSearch] = useState("");
  const [status, setStatus] = useState("ALL");
  const [dateRange, setDateRange] = useState("Last 90 Days");

  const filtered = useMemo(() => {
    const term = search.trim().toLowerCase();
    return MOCK_REPORTS.filter((report) => {
      const matchesSearch =
        !term || report.week.toLowerCase().includes(term) || report.project.toLowerCase().includes(term);
      const matchesStatus = status === "ALL" || report.status === status;
      return matchesSearch && matchesStatus;
    });
  }, [search, status]);

  const hasActiveFilters = search !== "" || status !== "ALL";

  function clearFilters() {
    setSearch("");
    setStatus("ALL");
  }

  return (
    <div className="flex flex-col gap-4">
      <PageHeader
        title="My Reports"
        action={
          <Button className="h-9 gap-1.5 px-3.5 text-[13px] font-semibold">
            <Plus className="size-3.5" />
            New Weekly Report
          </Button>
        }
      />

      <div className="flex flex-wrap items-center justify-between gap-3 rounded-lg border border-slate-200 bg-white p-3">
        <div className="flex flex-wrap items-center gap-3">
          <div className="relative w-full sm:w-[220px]">
            <Search className="pointer-events-none absolute left-2.5 top-1/2 size-3.5 -translate-y-1/2 text-slate-400" />
            <Input
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Search reports..."
              className="h-8 pl-8 text-[13px]"
            />
          </div>

          <Select value={status} onValueChange={setStatus}>
            <SelectTrigger className="h-8 w-fit gap-1.5 text-[13px]">
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

          <Select value={dateRange} onValueChange={setDateRange}>
            <SelectTrigger className="h-8 w-fit gap-1.5 text-[13px]">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {DATE_RANGES.map((option) => (
                <SelectItem key={option} value={option}>
                  {option}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        {hasActiveFilters && (
          <button
            type="button"
            onClick={clearFilters}
            className="text-xs font-semibold text-indigo-600 hover:underline"
          >
            Clear Filters
          </button>
        )}
      </div>

      <div className="flex flex-col rounded-xl border border-slate-200 bg-white shadow-[0_1px_1.5px_rgba(0,0,0,0.04)]">
        <div className="overflow-x-auto">
          <table className="w-full min-w-[840px] border-collapse text-[13px]">
            <thead>
              <tr className="bg-slate-100 text-left text-xs font-semibold text-slate-600">
                <th className="px-5 py-3 font-semibold">Week</th>
                <th className="w-[150px] px-5 py-3 font-semibold">Project</th>
                <th className="w-[160px] px-5 py-3 font-semibold">Status</th>
                <th className="w-[80px] px-5 py-3 font-semibold">Hours</th>
                <th className="w-[130px] px-5 py-3 font-semibold">Submitted</th>
                <th className="w-[150px] px-5 py-3 font-semibold">Reviewed By</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((report) => (
                <tr
                  key={report.id}
                  onClick={() => navigate(`/reports/${report.id}/edit`)}
                  className={`cursor-pointer border-t border-slate-200 transition-colors hover:bg-slate-50 ${
                    report.current ? "bg-indigo-50 hover:bg-indigo-100/70" : "bg-white"
                  }`}
                >
                  <td className="px-5 py-3 font-medium text-slate-900">{report.week}</td>
                  <td className="truncate px-5 py-3 text-slate-600">{report.project}</td>
                  <td className="whitespace-nowrap px-5 py-3">
                    <StatusBadge status={report.status} />
                  </td>
                  <td className="px-5 py-3 font-semibold text-slate-900">{report.hours}</td>
                  <td className="whitespace-nowrap px-5 py-3 text-slate-600">{report.submitted}</td>
                  <td className="truncate px-5 py-3 text-slate-600">{report.reviewedBy}</td>
                </tr>
              ))}
              {filtered.length === 0 && (
                <tr>
                  <td colSpan={6} className="px-5 py-10 text-center text-sm text-slate-500">
                    No reports match your filters.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        <div className="flex flex-wrap items-center justify-between gap-3 p-4">
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
