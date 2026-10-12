import { Link, useParams } from "react-router-dom";
import { Loader2 } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import EmptyState from "@/components/feedback/EmptyState";
import { RoleBadge, StatusBadge } from "./UsersList";
import { useActivateUser, useDeactivateUser, useUser } from "@/hooks/useUsers";
import { getApiErrorMessage } from "@/utils/apiError";

const PROJECT_STATUS_BADGE = {
  ACTIVE: "bg-emerald-50 text-emerald-700 border-emerald-200",
  INACTIVE: "bg-slate-50 text-slate-600 border-slate-200",
  COMPLETED: "bg-blue-50 text-blue-700 border-blue-200",
};

export default function UserDetail() {
  const { userId } = useParams();
  const { data: user, isPending, isError, error } = useUser(userId);
  const deactivateUser = useDeactivateUser();
  const activateUser = useActivateUser();

  if (isPending) {
    return (
      <div className="flex items-center justify-center gap-2 py-16 text-sm text-slate-500">
        <Loader2 className="size-4 animate-spin" />
        Loading user…
      </div>
    );
  }

  if (isError) {
    const notFound = error?.response?.status === 404;
    return (
      <EmptyState
        title={notFound ? "User not found" : "Couldn't load this user"}
        description={notFound ? "This user doesn't exist or may have been removed." : getApiErrorMessage(error)}
        action={
          <Button asChild className="h-9 px-3.5 text-[13px] font-semibold">
            <Link to="/admin/users">Back to Users</Link>
          </Button>
        }
      />
    );
  }

  const breakdown = user.reportStatusBreakdown ?? {};
  const approvalRate =
    user.reportCount > 0 ? `${Math.round(((breakdown.approved ?? 0) / user.reportCount) * 100)}%` : "—";

  function handleToggleActive() {
    const mutation = user.active ? deactivateUser : activateUser;
    mutation.mutate(user.id, {
      onSuccess: () => toast.success(user.active ? "User suspended" : "User reactivated"),
      onError: (err) => toast.error(getApiErrorMessage(err)),
    });
  }

  const togglePending = deactivateUser.isPending || activateUser.isPending;

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <p className="flex flex-wrap items-center gap-2 text-[13px]">
          <Link to="/admin/users" className="font-medium text-slate-500 hover:text-slate-700">
            Users
          </Link>
          <span className="text-slate-400">/</span>
          <span className="font-semibold text-slate-900">{user.name}</span>
        </p>

        <div className="flex flex-wrap gap-2">
          <Button type="button" variant="outline" className="h-8 px-3 text-[13px] font-semibold" asChild>
            <Link to={`/admin/users/${user.id}/edit`}>Edit User</Link>
          </Button>
          <Button type="button" variant="outline" className="h-8 px-3 text-[13px] font-semibold">
            Change Role
          </Button>
          <Button
            type="button"
            variant="outline"
            disabled={togglePending}
            onClick={handleToggleActive}
            className="h-8 border-red-300 px-3 text-[13px] font-semibold text-red-700 hover:bg-red-50"
          >
            {togglePending ? (
              <Loader2 className="size-3.5 animate-spin" />
            ) : user.active ? (
              "Suspend User"
            ) : (
              "Reactivate User"
            )}
          </Button>
        </div>
      </div>

      <div className="flex flex-col gap-6 lg:flex-row">
        <div className="flex min-w-0 flex-1 flex-col gap-6">
          <div className="flex flex-col gap-5 rounded-xl border border-slate-200 bg-white p-6">
            <div className="flex items-center gap-4">
              <div className="flex size-16 shrink-0 items-center justify-center rounded-full border border-slate-200 bg-slate-100 text-2xl font-semibold text-slate-600">
                {initialsOf(user.name)}
              </div>
              <div className="flex flex-col gap-1">
                <p className="text-xl font-bold text-slate-900">{user.name}</p>
                <p className="text-[13px] text-slate-600">{user.email}</p>
              </div>
            </div>

            <div className="h-px w-full bg-slate-200" />

            <div className="grid grid-cols-2 gap-5 sm:grid-cols-3">
              <div className="flex flex-col gap-1.5">
                <p className="text-[11px] font-semibold tracking-wide text-slate-400 uppercase">System Role</p>
                <RoleBadge role={user.role} />
              </div>
              <div className="flex flex-col gap-1.5">
                <p className="text-[11px] font-semibold tracking-wide text-slate-400 uppercase">Account Status</p>
                <StatusBadge status={user.active ? "ACTIVE" : "SUSPENDED"} />
              </div>
              <div className="flex flex-col gap-1.5">
                <p className="text-[11px] font-semibold tracking-wide text-slate-400 uppercase">Created Date</p>
                <p className="text-sm font-semibold text-slate-900">{formatDate(user.createdAt)}</p>
              </div>
            </div>
          </div>

          <div className="flex flex-col gap-4 rounded-xl border border-slate-200 bg-white p-6">
            <p className="text-[15px] font-bold text-slate-900">Assigned Projects</p>
            {user.projects.length === 0 ? (
              <p className="text-sm text-slate-500">No projects assigned.</p>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full min-w-[420px] border-collapse text-[13px]">
                  <thead>
                    <tr className="border-b border-slate-200 text-left text-xs font-semibold text-slate-600">
                      <th className="pb-2 font-semibold">Project Name</th>
                      <th className="w-[120px] pb-2 font-semibold">Assigned</th>
                      <th className="w-[100px] pb-2 text-right font-semibold">Status</th>
                    </tr>
                  </thead>
                  <tbody>
                    {user.projects.map((project) => (
                      <tr key={project.id} className="border-b border-slate-100 last:border-none">
                        <td className="truncate py-3 pr-2 font-semibold text-slate-900">{project.name}</td>
                        <td className="py-3 text-slate-600">{formatDate(project.assignedAt)}</td>
                        <td className="py-3 text-right">
                          <span
                            className={`inline-flex items-center rounded border px-2 py-0.5 text-[11px] font-semibold uppercase ${
                              PROJECT_STATUS_BADGE[project.status] ?? "bg-slate-50 text-slate-600 border-slate-200"
                            }`}
                          >
                            {project.status}
                          </span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>

        <div className="flex w-full flex-col gap-6 lg:w-[400px] lg:shrink-0">
          <div className="flex flex-col gap-4 rounded-xl border border-slate-200 bg-white p-5">
            <p className="text-[15px] font-bold text-slate-900">Report Statistics</p>
            <div className="flex gap-3">
              <div className="flex-1 rounded-lg bg-[#f8f8fa] p-3">
                <p className="text-[22px] font-bold text-slate-900">{user.reportCount}</p>
                <p className="text-[11px] text-slate-600">Total Reports</p>
              </div>
              <div className="flex-1 rounded-lg bg-[#f8f8fa] p-3">
                <p className="text-[22px] font-bold text-emerald-700">{approvalRate}</p>
                <p className="text-[11px] text-slate-600">Approval Rate</p>
              </div>
              <div className="flex-1 rounded-lg bg-[#f8f8fa] p-3">
                <p className="text-[22px] font-bold text-slate-900">{user.projectCount}</p>
                <p className="text-[11px] text-slate-600">Projects</p>
              </div>
            </div>
          </div>

          <div className="flex flex-col gap-3 rounded-xl border border-slate-200 bg-white p-5">
            <p className="text-[15px] font-bold text-slate-900">Report Status Breakdown</p>
            <div className="flex flex-col gap-2 text-[13px]">
              <BreakdownRow label="Approved" value={breakdown.approved ?? 0} className="text-emerald-700" />
              <BreakdownRow label="Pending Review" value={breakdown.submitted ?? 0} className="text-amber-700" />
              <BreakdownRow label="Needs Correction" value={breakdown.needsCorrection ?? 0} className="text-red-700" />
              <BreakdownRow label="Draft" value={breakdown.draft ?? 0} className="text-slate-600" />
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

function BreakdownRow({ label, value, className }) {
  return (
    <div className="flex items-center justify-between">
      <span className="text-slate-600">{label}</span>
      <span className={`font-semibold ${className}`}>{value}</span>
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
