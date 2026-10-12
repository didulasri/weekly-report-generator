import { useMemo, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { Loader2, Plus, Search } from "lucide-react";
import PageHeader from "@/components/layout/PageHeader";
import EmptyState from "@/components/feedback/EmptyState";
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
import { useMyReports } from "@/hooks/useReports";
import { getApiErrorMessage } from "@/utils/apiError";
import { currentWeekRange } from "@/utils/weekDates";

const STATUS_FILTERS = [
  { value: "ALL", label: "All Statuses" },
  { value: "DRAFT", label: "Draft" },
  { value: "SUBMITTED", label: "Pending Review" },
  { value: "NEEDS_CORRECTION", label: "Needs Correction" },
  { value: "APPROVED", label: "Approved" },
];

const DATE_RANGES = ["Last 30 Days", "Last 90 Days", "This Year", "All Time"];

// No "search" param exists on GET /api/reports/my (only status/projectId/startDate/endDate), so
// search is applied client-side over a generously-sized fetch -- reasonable for a single user's
// own report history, which realistically never approaches this page size.
const FETCH_SIZE = 100;
const PAGE_SIZE = 10;

function dateRangeToBounds(label) {
  if (label === "All Time") return {};
  const end = new Date();
  const start = new Date();
  if (label === "Last 30 Days") start.setDate(end.getDate() - 30);
  else if (label === "Last 90 Days") start.setDate(end.getDate() - 90);
  else if (label === "This Year") start.setMonth(0, 1);
  return { startDate: start.toISOString().slice(0, 10), endDate: end.toISOString().slice(0, 10) };
}

function formatDate(isoString) {
  if (!isoString) return "—";
  return new Date(isoString).toLocaleDateString("en-US", { month: "short", day: "numeric", year: "numeric" });
}

function weekLabel(report) {
  return `${formatDate(report.weekStartDate)} - ${formatDate(report.weekEndDate)}`;
}

export default function MyReports() {
  const navigate = useNavigate();
  const [search, setSearch] = useState("");
  const [status, setStatus] = useState("ALL");
  const [dateRange, setDateRange] = useState("Last 90 Days");
  const [page, setPage] = useState(0);

  const bounds = dateRangeToBounds(dateRange);
  const { data, isPending, isError, error } = useMyReports({
    status: status === "ALL" ? undefined : status,
    ...bounds,
    size: FETCH_SIZE,
  });

  // A filter change invalidates the current page. Adjusted directly during render (guarded by
  // comparing against the filter key that produced the current page state) rather than through an
  // effect -- React's own recommended pattern for "reset state when some other state changes".
  const filterKey = `${search}|${status}|${dateRange}`;
  const [pageFilterKey, setPageFilterKey] = useState(filterKey);
  if (filterKey !== pageFilterKey) {
    setPageFilterKey(filterKey);
    setPage(0);
  }

  const thisWeekStart = useMemo(() => currentWeekRange().start, []);

  const filtered = useMemo(() => {
    const term = search.trim().toLowerCase();
    const reports = data?.content ?? [];
    if (!term) return reports;
    return reports.filter(
      (report) =>
        weekLabel(report).toLowerCase().includes(term) ||
        (report.projectName ?? "").toLowerCase().includes(term)
    );
  }, [data, search]);

  const pageRows = filtered.slice(page * PAGE_SIZE, page * PAGE_SIZE + PAGE_SIZE);
  const hasActiveFilters = search !== "" || status !== "ALL" || dateRange !== "Last 90 Days";

  function clearFilters() {
    setSearch("");
    setStatus("ALL");
    setDateRange("Last 90 Days");
  }

  const rangeStart = filtered.length === 0 ? 0 : page * PAGE_SIZE + 1;
  const rangeEnd = Math.min(filtered.length, page * PAGE_SIZE + pageRows.length);

  return (
    <div className="flex flex-col gap-4">
      <PageHeader
        title="My Reports"
        action={
          <Button className="h-9 gap-1.5 px-3.5 text-[13px] font-semibold" asChild>
            <Link to="/reports/new">
              <Plus className="size-3.5" />
              New Weekly Report
            </Link>
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
              <table className="w-full min-w-[840px] border-collapse text-[13px]">
                <thead>
                  <tr className="bg-slate-100 text-left text-xs font-semibold text-slate-600">
                    <th className="px-5 py-3 font-semibold">Week</th>
                    <th className="w-[150px] px-5 py-3 font-semibold">Project</th>
                    <th className="w-[160px] px-5 py-3 font-semibold">Status</th>
                    <th className="w-[80px] px-5 py-3 font-semibold">Hours</th>
                    <th className="w-[130px] px-5 py-3 font-semibold">Last Updated</th>
                    <th className="w-[150px] px-5 py-3 font-semibold">Reviewed By</th>
                  </tr>
                </thead>
                <tbody>
                  {pageRows.map((report) => (
                    <tr
                      key={report.id}
                      onClick={() => navigate(`/reports/${report.id}/edit`)}
                      className={`cursor-pointer border-t border-slate-200 transition-colors hover:bg-slate-50 ${
                        report.weekStartDate === thisWeekStart ? "bg-indigo-50 hover:bg-indigo-100/70" : "bg-white"
                      }`}
                    >
                      <td className="px-5 py-3 font-medium text-slate-900">{weekLabel(report)}</td>
                      <td className="truncate px-5 py-3 text-slate-600">{report.projectName}</td>
                      <td className="whitespace-nowrap px-5 py-3">
                        <StatusBadge status={report.status} />
                      </td>
                      <td className="px-5 py-3 font-semibold text-slate-900">
                        {Number(report.totalHours ?? 0).toFixed(1)}
                      </td>
                      <td className="whitespace-nowrap px-5 py-3 text-slate-600">{formatDate(report.updatedAt)}</td>
                      <td className="truncate px-5 py-3 text-slate-600">{report.latestReviewerName ?? "—"}</td>
                    </tr>
                  ))}
                  {pageRows.length === 0 && (
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
              <p className="text-xs text-slate-600">
                {filtered.length === 0 ? "No reports" : `Showing ${rangeStart}-${rangeEnd} of ${filtered.length} reports`}
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
                  disabled={rangeEnd >= filtered.length}
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
