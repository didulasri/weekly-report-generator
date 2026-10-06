import { Link, useParams } from "react-router-dom";
import { Button } from "@/components/ui/button";
import EmptyState from "@/components/feedback/EmptyState";
import { RoleBadge, StatusBadge } from "./UsersList";
import { getUserById, userDetail } from "./mockUsers";

export default function UserDetail() {
  const { userId } = useParams();
  const user = getUserById(userId);

  if (!user) {
    return (
      <EmptyState
        title="User not found"
        description="This user doesn't exist or may have been removed."
        action={
          <Button asChild className="h-9 px-3.5 text-[13px] font-semibold">
            <Link to="/admin/users">Back to Users</Link>
          </Button>
        }
      />
    );
  }

  const detail = userDetail(user);

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <p className="flex flex-wrap items-center gap-2 text-[13px]">
          <Link to="/admin/users" className="font-medium text-slate-500 hover:text-slate-700">
            Users
          </Link>
          <span className="text-slate-400">/</span>
          <span className="font-semibold text-slate-900">{detail.name}</span>
        </p>

        <div className="flex flex-wrap gap-2">
          <Button type="button" variant="outline" className="h-8 px-3 text-[13px] font-semibold" asChild>
            <Link to={`/admin/users/${detail.id}/edit`}>Edit User</Link>
          </Button>
          <Button type="button" className="h-8 px-3 text-[13px] font-semibold">
            Change Role
          </Button>
          <Button
            type="button"
            variant="outline"
            className="h-8 border-red-300 px-3 text-[13px] font-semibold text-red-700 hover:bg-red-50"
          >
            Suspend User
          </Button>
        </div>
      </div>

      <div className="flex flex-col gap-6 lg:flex-row">
        <div className="flex min-w-0 flex-1 flex-col gap-6">
          <div className="flex flex-col gap-5 rounded-xl border border-slate-200 bg-white p-6">
            <div className="flex items-center gap-4">
              <div className="flex size-16 shrink-0 items-center justify-center rounded-full border border-slate-200 bg-slate-100 text-2xl font-semibold text-slate-600">
                {detail.initials}
              </div>
              <div className="flex flex-col gap-1">
                <p className="text-xl font-bold text-slate-900">{detail.name}</p>
                <p className="text-[13px] text-slate-600">{detail.email}</p>
              </div>
            </div>

            <div className="h-px w-full bg-slate-200" />

            <div className="grid grid-cols-2 gap-5 sm:grid-cols-4">
              <div className="flex flex-col gap-1.5">
                <p className="text-[11px] font-semibold tracking-wide text-slate-400 uppercase">System Role</p>
                <RoleBadge role={detail.role} />
              </div>
              <div className="flex flex-col gap-1.5">
                <p className="text-[11px] font-semibold tracking-wide text-slate-400 uppercase">Account Status</p>
                <StatusBadge status={detail.status} />
              </div>
              <div className="flex flex-col gap-1.5">
                <p className="text-[11px] font-semibold tracking-wide text-slate-400 uppercase">Created Date</p>
                <p className="text-sm font-semibold text-slate-900">{detail.created}</p>
              </div>
              <div className="flex flex-col gap-1.5">
                <p className="text-[11px] font-semibold tracking-wide text-slate-400 uppercase">Last Login</p>
                <p className="text-sm font-semibold text-slate-900">{detail.lastLoginDetail}</p>
              </div>
            </div>
          </div>

          <div className="flex flex-col gap-4 rounded-xl border border-slate-200 bg-white p-6">
            <p className="text-[15px] font-bold text-slate-900">Assigned Projects</p>
            {detail.projects.length === 0 ? (
              <p className="text-sm text-slate-500">No projects assigned.</p>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full min-w-[420px] border-collapse text-[13px]">
                  <thead>
                    <tr className="border-b border-slate-200 text-left text-xs font-semibold text-slate-600">
                      <th className="pb-2 font-semibold">Project Name</th>
                      <th className="w-[120px] pb-2 font-semibold">Project Role</th>
                      <th className="w-[100px] pb-2 text-right font-semibold">Status</th>
                    </tr>
                  </thead>
                  <tbody>
                    {detail.projects.map((project) => (
                      <tr key={project.name} className="border-b border-slate-100 last:border-none">
                        <td className="truncate py-3 pr-2 font-semibold text-slate-900">{project.name}</td>
                        <td className="py-3 text-slate-600">{project.role}</td>
                        <td className="py-3 text-right">
                          <StatusBadge status={project.status} />
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
                <p className="text-[22px] font-bold text-slate-900">{detail.stats.totalReports}</p>
                <p className="text-[11px] text-slate-600">Total Reports</p>
              </div>
              <div className="flex-1 rounded-lg bg-[#f8f8fa] p-3">
                <p className="text-[22px] font-bold text-emerald-700">{detail.stats.approvalRate}</p>
                <p className="text-[11px] text-slate-600">Approval Rate</p>
              </div>
              <div className="flex-1 rounded-lg bg-[#f8f8fa] p-3">
                <p className="text-[22px] font-bold text-slate-900">{detail.stats.avgHours}</p>
                <p className="text-[11px] text-slate-600">Avg Hours/Wk</p>
              </div>
            </div>
          </div>

          <div className="flex flex-col gap-4 rounded-xl border border-slate-200 bg-white p-5">
            <p className="text-[15px] font-bold text-slate-900">Account Activity</p>
            <div className="flex flex-col">
              {detail.activity.map((item, i) => (
                <div key={item.title + i} className="flex gap-3">
                  <div className="flex flex-col items-center">
                    <span className="mt-1.5 size-2 shrink-0 rounded-full bg-slate-300" />
                    {i < detail.activity.length - 1 && <span className="w-px flex-1 bg-slate-200" />}
                  </div>
                  <div className="flex flex-1 flex-col gap-0.5 pb-4">
                    <div className="flex items-center justify-between gap-2">
                      <p className="text-[13px] font-semibold text-slate-900">{item.title}</p>
                      <p className="shrink-0 text-[11px] text-slate-400">{item.time}</p>
                    </div>
                    <p className="text-[11px] text-slate-600">{item.detail}</p>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
