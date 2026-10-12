import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { Loader2, Plus, Search } from "lucide-react";
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
import { useUsers } from "@/hooks/useUsers";
import { getApiErrorMessage } from "@/utils/apiError";

const ROLE_BADGE = {
  TEAM_MEMBER: { label: "Team Member", className: "bg-slate-100 text-slate-600 border-slate-300" },
  MANAGER: { label: "Manager", className: "bg-white text-[#1a0b2e] border-[#1a0b2e]" },
  ADMIN: { label: "Admin", className: "bg-[#1a0b2e] text-white border-[#1a0b2e]" },
};

const STATUS_BADGE = {
  ACTIVE: { label: "Active", className: "bg-emerald-50 text-emerald-700 border-emerald-200" },
  PENDING: { label: "Pending", className: "bg-amber-50 text-amber-700 border-amber-200" },
  SUSPENDED: { label: "Suspended", className: "bg-red-50 text-red-700 border-red-200" },
};

// Exported -- the user detail screen reuses the exact same role/status badges so the two screens
// never drift on what a given role/status looks like.
export function RoleBadge({ role }) {
  const config = ROLE_BADGE[role];
  return (
    <span
      className={`inline-flex items-center rounded border px-2 py-0.5 text-[11px] font-semibold ${
        config?.className ?? "bg-slate-50 text-slate-600 border-slate-200"
      }`}
    >
      {config?.label ?? role}
    </span>
  );
}

export function StatusBadge({ status }) {
  const config = STATUS_BADGE[status];
  return (
    <span
      className={`inline-flex items-center rounded border px-2 py-0.5 text-[11px] font-semibold uppercase ${
        config?.className ?? "bg-slate-50 text-slate-600 border-slate-200"
      }`}
    >
      {config?.label ?? status}
    </span>
  );
}

const ROLE_FILTERS = [
  { value: "ALL", label: "Role: All" },
  { value: "TEAM_MEMBER", label: "Team Member" },
  { value: "MANAGER", label: "Manager" },
  { value: "ADMIN", label: "Admin" },
];

// The backend only tracks a boolean active flag -- there is no PENDING user state (an invitation
// that hasn't been accepted yet is a row in the invitations table, not a user row), so this filter
// has two real options rather than the three the mock data used to have.
const STATUS_FILTERS = [
  { value: "ALL", label: "Status: All" },
  { value: "ACTIVE", label: "Active" },
  { value: "SUSPENDED", label: "Suspended" },
];

const PAGE_SIZE = 10;

export default function UsersList() {
  const navigate = useNavigate();
  const [searchInput, setSearchInput] = useState("");
  const [search, setSearch] = useState("");
  const [role, setRole] = useState("ALL");
  const [status, setStatus] = useState("ALL");
  const [page, setPage] = useState(0);

  // Debounce the search box so every keystroke doesn't fire a request -- 350ms matches the
  // typical pause between a user finishing typing and expecting results. Resets the page here too
  // (rather than in a separate effect keyed on every filter) so a filter change and its page reset
  // land in the same render instead of cascading into a second one.
  useEffect(() => {
    const timeout = setTimeout(() => {
      setSearch(searchInput.trim());
      setPage(0);
    }, 350);
    return () => clearTimeout(timeout);
  }, [searchInput]);

  function handleRoleChange(value) {
    setRole(value);
    setPage(0);
  }

  function handleStatusChange(value) {
    setStatus(value);
    setPage(0);
  }

  const { data, isPending, isError, error, isFetching } = useUsers({
    role: role === "ALL" ? undefined : role,
    active: status === "ALL" ? undefined : status === "ACTIVE",
    search: search || undefined,
    page,
    size: PAGE_SIZE,
  });

  const users = data?.content ?? [];
  const totalElements = data?.totalElements ?? 0;
  const hasActiveFilters = searchInput !== "" || role !== "ALL" || status !== "ALL";

  function clearFilters() {
    setSearchInput("");
    setSearch("");
    setRole("ALL");
    setStatus("ALL");
    setPage(0);
  }

  const rangeStart = totalElements === 0 ? 0 : page * PAGE_SIZE + 1;
  const rangeEnd = Math.min(totalElements, page * PAGE_SIZE + users.length);

  return (
    <div className="flex flex-col gap-4">
      <PageHeader
        title="Users"
        action={
          <Button className="h-9 gap-1.5 px-3.5 text-[13px] font-semibold" asChild>
            <Link to="/admin/users/invite">
              <Plus className="size-3.5" />
              Invite User
            </Link>
          </Button>
        }
      />

      <div className="flex flex-wrap items-center justify-between gap-3 rounded-lg border border-slate-200 bg-white p-3">
        <div className="flex flex-wrap items-center gap-3">
          <div className="relative w-full sm:w-[220px]">
            <Search className="pointer-events-none absolute left-2.5 top-1/2 size-3.5 -translate-y-1/2 text-slate-400" />
            <Input
              value={searchInput}
              onChange={(e) => setSearchInput(e.target.value)}
              placeholder="Search users..."
              className="h-8 pl-8 text-[13px]"
            />
          </div>

          <Select value={role} onValueChange={handleRoleChange}>
            <SelectTrigger className="h-8 w-fit gap-1.5 text-[13px]">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {ROLE_FILTERS.map((option) => (
                <SelectItem key={option.value} value={option.value}>
                  {option.label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>

          <Select value={status} onValueChange={handleStatusChange}>
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
            Loading users…
          </div>
        ) : isError ? (
          <EmptyState
            title="Couldn't load users"
            description={getApiErrorMessage(error)}
          />
        ) : (
          <>
            <div className="overflow-x-auto">
              <table className="w-full min-w-[900px] table-fixed border-collapse text-[13px]">
                <thead>
                  <tr className="bg-slate-100 text-left text-xs font-semibold text-slate-600">
                    <th className="w-[200px] px-4 py-3 font-semibold">Name</th>
                    <th className="w-[220px] px-4 py-3 font-semibold">Email</th>
                    <th className="w-[130px] px-4 py-3 font-semibold">Role</th>
                    <th className="w-[100px] px-4 py-3 font-semibold">Status</th>
                    <th className="w-[115px] whitespace-nowrap px-4 py-3 font-semibold">Created Date</th>
                    <th className="w-[85px] whitespace-nowrap px-4 py-3 text-right font-semibold">Projects</th>
                    <th className="w-[85px] whitespace-nowrap px-4 py-3 text-right font-semibold">Reports</th>
                  </tr>
                </thead>
                <tbody>
                  {users.map((user) => (
                    <tr
                      key={user.id}
                      onClick={() => navigate(`/admin/users/${user.id}`)}
                      className="cursor-pointer border-t border-slate-200 transition-colors hover:bg-slate-50"
                    >
                      <td className="px-4 py-3">
                        <div className="flex items-center gap-2">
                          <div className="flex size-7 shrink-0 items-center justify-center rounded-full border border-slate-200 bg-slate-100 text-[11px] font-semibold text-slate-600">
                            {initialsOf(user.name)}
                          </div>
                          <span className="truncate font-semibold text-slate-900">{user.name}</span>
                        </div>
                      </td>
                      <td className="truncate px-4 py-3 text-slate-600">{user.email}</td>
                      <td className="px-4 py-3">
                        <RoleBadge role={user.role} />
                      </td>
                      <td className="whitespace-nowrap px-4 py-3">
                        <StatusBadge status={user.active ? "ACTIVE" : "SUSPENDED"} />
                      </td>
                      <td className="whitespace-nowrap px-4 py-3 text-slate-600">
                        {formatDate(user.createdAt)}
                      </td>
                      <td className="whitespace-nowrap px-4 py-3 text-right text-slate-600">
                        {user.projectCount}
                      </td>
                      <td className="whitespace-nowrap px-4 py-3 text-right text-slate-600">
                        {user.reportCount}
                      </td>
                    </tr>
                  ))}
                  {users.length === 0 && (
                    <tr>
                      <td colSpan={7} className="px-4 py-10 text-center text-sm text-slate-500">
                        No users match your filters.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>

            <div className="flex flex-wrap items-center justify-between gap-3 p-4">
              <p className="text-xs text-slate-600">
                {totalElements === 0
                  ? "No users"
                  : `Showing ${rangeStart}-${rangeEnd} of ${totalElements} users`}
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

function initialsOf(name) {
  if (!name) return "?";
  const parts = name.trim().split(/\s+/);
  const first = parts[0]?.[0] ?? "";
  const last = parts.length > 1 ? parts[parts.length - 1][0] : "";
  return (first + last).toUpperCase();
}

function formatDate(isoString) {
  if (!isoString) return "—";
  return new Date(isoString).toLocaleDateString("en-US", {
    month: "short",
    day: "numeric",
    year: "numeric",
  });
}
