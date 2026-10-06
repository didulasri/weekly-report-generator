import { useMemo, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { Plus, Search } from "lucide-react";
import PageHeader from "@/components/layout/PageHeader";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { ALL_USERS } from "./mockUsers";

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

const STATUS_FILTERS = [
  { value: "ALL", label: "Status: All" },
  { value: "ACTIVE", label: "Active" },
  { value: "PENDING", label: "Pending" },
  { value: "SUSPENDED", label: "Suspended" },
];

export default function UsersList() {
  const navigate = useNavigate();
  const [search, setSearch] = useState("");
  const [role, setRole] = useState("ALL");
  const [status, setStatus] = useState("ALL");

  const filtered = useMemo(() => {
    const term = search.trim().toLowerCase();
    return ALL_USERS.filter((user) => {
      const matchesSearch =
        !term || user.name.toLowerCase().includes(term) || user.email.toLowerCase().includes(term);
      const matchesRole = role === "ALL" || user.role === role;
      const matchesStatus = status === "ALL" || user.status === status;
      return matchesSearch && matchesRole && matchesStatus;
    });
  }, [search, role, status]);

  const hasActiveFilters = search !== "" || role !== "ALL" || status !== "ALL";

  function clearFilters() {
    setSearch("");
    setRole("ALL");
    setStatus("ALL");
  }

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
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Search users..."
              className="h-8 pl-8 text-[13px]"
            />
          </div>

          <Select value={role} onValueChange={setRole}>
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
          <table className="w-full min-w-[900px] table-fixed border-collapse text-[13px]">
            <thead>
              <tr className="bg-slate-100 text-left text-xs font-semibold text-slate-600">
                <th className="w-[190px] px-4 py-3 font-semibold">Name</th>
                <th className="w-[190px] px-4 py-3 font-semibold">Email</th>
                <th className="w-[140px] px-4 py-3 font-semibold">Role</th>
                <th className="w-[100px] px-4 py-3 font-semibold">Status</th>
                <th className="w-[115px] whitespace-nowrap px-4 py-3 font-semibold">Created Date</th>
                <th className="w-[115px] whitespace-nowrap px-4 py-3 text-right font-semibold">Last Login</th>
                <th className="w-[56px] px-4 py-3 text-right font-semibold">Actions</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((user) => (
                <tr
                  key={user.email}
                  onClick={() => navigate(`/admin/users/${user.id}`)}
                  className="cursor-pointer border-t border-slate-200 transition-colors hover:bg-slate-50"
                >
                  <td className="px-4 py-3">
                    <div className="flex items-center gap-2">
                      <div className="flex size-7 shrink-0 items-center justify-center rounded-full border border-slate-200 bg-slate-100 text-[11px] font-semibold text-slate-600">
                        {user.initials}
                      </div>
                      <span className="truncate font-semibold text-slate-900">{user.name}</span>
                    </div>
                  </td>
                  <td className="truncate px-4 py-3 text-slate-600">{user.email}</td>
                  <td className="px-4 py-3">
                    <RoleBadge role={user.role} />
                  </td>
                  <td className="whitespace-nowrap px-4 py-3">
                    <StatusBadge status={user.status} />
                  </td>
                  <td className="whitespace-nowrap px-4 py-3 text-slate-600">{user.created}</td>
                  <td className="whitespace-nowrap px-4 py-3 text-right text-slate-600">{user.lastLogin}</td>
                  <td className="px-4 py-3 text-right">
                    <button
                      type="button"
                      onClick={(e) => e.stopPropagation()}
                      className="text-base font-bold text-slate-500"
                      aria-label="Row actions"
                    >
                      •••
                    </button>
                  </td>
                </tr>
              ))}
              {filtered.length === 0 && (
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
          <p className="text-xs text-slate-600">Showing 1-{filtered.length} of 47 users</p>
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
