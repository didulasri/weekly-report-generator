import { ClipboardList, Folder, Mail, UserCheck, Users } from "lucide-react";
import { FolderPlus, ShieldCheck, UserPlus } from "lucide-react";
import PageHeader from "@/components/layout/PageHeader";
import { Button } from "@/components/ui/button";

// Static mock data -- UI-only checkpoint, no admin/report/project service wired yet.
const STATS = [
  { label: "Total Users", value: "48", trend: "+4.2%", trendSuffix: " vs. last month", tone: "positive", icon: Users },
  { label: "Active Users", value: "43", trend: "89.6% of all users", tone: "neutral", icon: UserCheck },
  { label: "Pending Invitations", value: "5", trend: "Awaiting acceptance", tone: "warning", icon: Mail },
  { label: "Projects", value: "12", trend: "9 active projects", tone: "neutral", icon: Folder },
  { label: "Reports Pending Review", value: "35", trend: "Submitted for review", tone: "neutral", icon: ClipboardList },
];

const REPORT_STATUS_BARS = [
  { label: "Submitted", value: 35, className: "bg-blue-600" },
  { label: "Approved", value: 28, className: "bg-emerald-700" },
  { label: "Needs Correction", value: 7, className: "bg-red-700" },
  { label: "Draft", value: 5, className: "bg-slate-500" },
];
const REPORT_STATUS_TOTAL = REPORT_STATUS_BARS.reduce((sum, row) => sum + row.value, 0);

const USER_DISTRIBUTION = [
  { label: "Team Members", value: 35, color: "#4f46e5" },
  { label: "Managers", value: 6, color: "#7c3aed" },
  { label: "Admins", value: 2, color: "#ffb7a5" },
];
const USER_DISTRIBUTION_TOTAL = USER_DISTRIBUTION.reduce((sum, row) => sum + row.value, 0);

const USER_DISTRIBUTION_GRADIENT = USER_DISTRIBUTION.reduce(
  (acc, row) => {
    const start = acc.cursor;
    const end = start + (row.value / USER_DISTRIBUTION_TOTAL) * 100;
    return { cursor: end, stops: [...acc.stops, `${row.color} ${start}% ${end}%`] };
  },
  { cursor: 0, stops: [] }
).stops.join(", ");

const PENDING_INVITATIONS = [
  { name: "John Doe", email: "john@company.com", role: "Team Member", sent: "Oct 24, 2024" },
  { name: "Sarah Smith", email: "sarah@company.com", role: "Manager", sent: "Oct 23, 2024" },
];

const RECENT_ACTIVITY = [
  { icon: UserPlus, title: "User invited", detail: "Jane Doe invited John Doe", time: "10 minutes ago" },
  { icon: ShieldCheck, title: "Role changed", detail: "Alex Mercer is now a Manager", time: "1 hour ago" },
  { icon: FolderPlus, title: "Project created", detail: "Jane Doe created Partner API", time: "2 hours ago" },
];

const RECENT_REPORTS = [
  { employee: "Sarah Chen", project: "Acme Platform", week: "Week 43", status: "Submitted", className: "bg-blue-50 text-blue-600" },
  { employee: "Marcus Vance", project: "Billing Redesign", week: "Week 43", status: "Approved", className: "bg-emerald-50 text-emerald-700" },
  { employee: "Alex Mercer", project: "Partner API", week: "Week 43", status: "Needs Correction", className: "bg-red-50 text-red-700" },
  { employee: "Ada Lovelace", project: "Security Audit", week: "Week 43", status: "Draft", className: "bg-slate-100 text-slate-600" },
];

const PROJECTS = [
  { name: "Acme Platform", owner: "Grace Hopper", members: 8 },
  { name: "Billing Redesign", owner: "Bill Gates", members: 5 },
  { name: "Partner API", owner: "Alex Mercer", members: 4 },
];

function StatCard({ label, value, trend, trendSuffix, tone, icon: Icon }) {
  const toneClass =
    tone === "positive" ? "text-emerald-700" : tone === "warning" ? "text-amber-700" : "text-slate-500";
  return (
    <div className="flex flex-1 flex-col gap-2.5 rounded-[10px] border border-slate-200 bg-white p-4">
      <div className="flex items-center justify-between">
        <p className="text-xs font-medium text-slate-600">{label}</p>
        <Icon className="size-4 text-slate-400" />
      </div>
      <p className="text-[28px] leading-8 font-bold text-slate-900">{value}</p>
      <p className="text-[11px] text-slate-500">
        {tone === "positive" ? <span className={`font-semibold ${toneClass}`}>{trend}</span> : <span className={toneClass}>{trend}</span>}
        {trendSuffix}
      </p>
    </div>
  );
}

function SectionCard({ title, description, action, className, children }) {
  return (
    <div className={`flex flex-col gap-4 rounded-[10px] border border-slate-200 bg-white p-5 ${className ?? ""}`}>
      <div className="flex items-center justify-between gap-2">
        <div className="flex flex-col gap-1">
          <p className="text-[15px] font-bold text-slate-900">{title}</p>
          {description && <p className="text-xs text-slate-500">{description}</p>}
        </div>
        {action}
      </div>
      {children}
    </div>
  );
}

function ViewAllLink() {
  return (
    <button type="button" className="shrink-0 text-xs font-semibold text-[#1a0b2e] hover:underline">
      View All &rarr;
    </button>
  );
}

function DonutChart() {
  return (
    <div
      className="relative flex size-[116px] shrink-0 items-center justify-center rounded-full"
      style={{ background: `conic-gradient(${USER_DISTRIBUTION_GRADIENT})` }}
    >
      <div className="flex size-[82px] flex-col items-center justify-center rounded-full bg-white text-center">
        <p className="text-[22px] font-bold text-slate-900">{USER_DISTRIBUTION_TOTAL}</p>
        <p className="text-[10px] text-slate-500">active users</p>
      </div>
    </div>
  );
}

export default function AdminDashboard() {
  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-wrap items-end justify-between gap-2">
        <PageHeader title="Dashboard" description="Overview of your organization" />
        <p className="text-xs text-slate-500">Reporting period: October 2024</p>
      </div>

      <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-5">
        {STATS.map((stat) => (
          <StatCard key={stat.label} {...stat} />
        ))}
      </div>

      <div className="flex flex-col gap-4 lg:flex-row">
        <SectionCard
          title="Report Status Overview"
          description={`${REPORT_STATUS_TOTAL} reports across all projects · October 2024`}
          className="min-w-0 flex-1"
        >
          <div className="flex flex-col gap-3">
            {REPORT_STATUS_BARS.map((row) => (
              <div key={row.label} className="flex items-center gap-4">
                <p className="w-[120px] shrink-0 text-xs text-slate-600">{row.label}</p>
                <div className="h-2.5 flex-1 overflow-hidden rounded-full bg-slate-100">
                  <div
                    className={`h-full rounded-full ${row.className}`}
                    style={{ width: `${(row.value / REPORT_STATUS_TOTAL) * 100}%` }}
                  />
                </div>
                <p className="w-7 shrink-0 text-right text-[13px] font-semibold text-slate-900">{row.value}</p>
              </div>
            ))}
          </div>
        </SectionCard>

        <SectionCard
          title="User Distribution"
          description="By role · active users only"
          className="w-full lg:w-[400px] lg:shrink-0"
        >
          <div className="flex items-center gap-6">
            <DonutChart />
            <div className="flex flex-1 flex-col gap-3.5">
              {USER_DISTRIBUTION.map((row) => (
                <div key={row.label} className="flex items-center gap-2">
                  <span className="size-2 shrink-0 rounded-full" style={{ backgroundColor: row.color }} />
                  <p className="flex-1 text-xs text-slate-600">{row.label}</p>
                  <p className="text-[13px] font-semibold text-slate-900">{row.value}</p>
                </div>
              ))}
            </div>
          </div>
        </SectionCard>
      </div>

      <div className="flex flex-col rounded-[10px] border border-slate-200 bg-white">
        <div className="flex items-center justify-between gap-2 px-5 py-4">
          <div className="flex flex-col gap-1">
            <p className="text-[15px] font-bold text-slate-900">Pending Invitations</p>
            <p className="text-xs text-slate-500">5 invitations awaiting acceptance</p>
          </div>
          <ViewAllLink />
        </div>
        <div className="overflow-x-auto">
          <table className="w-full min-w-[640px] table-fixed border-collapse text-[13px]">
            <thead>
              <tr className="bg-slate-100 text-left text-[11px] font-semibold text-slate-600">
                <th className="w-[200px] px-5 py-2.5 font-semibold">Name</th>
                <th className="px-5 py-2.5 font-semibold">Email</th>
                <th className="w-[160px] px-5 py-2.5 font-semibold">Role</th>
                <th className="w-[130px] px-5 py-2.5 font-semibold">Invitation Sent</th>
                <th className="w-[80px] px-5 py-2.5 text-center font-semibold">Action</th>
              </tr>
            </thead>
            <tbody>
              {PENDING_INVITATIONS.map((invite) => (
                <tr key={invite.email} className="border-t border-slate-200">
                  <td className="truncate px-5 py-3 font-semibold text-slate-900">{invite.name}</td>
                  <td className="truncate px-5 py-3 text-slate-600">{invite.email}</td>
                  <td className="truncate px-5 py-3 text-slate-600">{invite.role}</td>
                  <td className="whitespace-nowrap px-5 py-3 text-slate-500">{invite.sent}</td>
                  <td className="px-5 py-3 text-center">
                    <Button type="button" variant="outline" size="sm" className="h-7 px-2.5 text-[11px] font-semibold">
                      Resend
                    </Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      <div className="flex flex-col gap-4 lg:flex-row">
        <div className="flex w-full flex-col gap-1 rounded-[10px] border border-slate-200 bg-white p-5 lg:w-[360px] lg:shrink-0">
          <p className="text-[15px] font-bold text-slate-900">Recent Activity</p>
          {RECENT_ACTIVITY.map((item, i) => (
            <div
              key={item.title + i}
              className={`flex gap-3 py-3 ${i < RECENT_ACTIVITY.length - 1 ? "border-b border-slate-200" : ""}`}
            >
              <div className="flex size-[30px] shrink-0 items-center justify-center rounded-full bg-slate-100">
                <item.icon className="size-4 text-slate-500" />
              </div>
              <div className="flex flex-col gap-0.5">
                <p className="text-xs font-semibold text-slate-900">{item.title}</p>
                <p className="text-xs text-slate-600">{item.detail}</p>
                <p className="text-[11px] text-slate-400">{item.time}</p>
              </div>
            </div>
          ))}
        </div>

        <div className="flex min-w-0 flex-1 flex-col rounded-[10px] border border-slate-200 bg-white">
          <div className="flex items-center justify-between gap-2 p-5">
            <p className="text-[15px] font-bold text-slate-900">Recent Reports</p>
            <ViewAllLink />
          </div>
          <div className="overflow-x-auto">
            <table className="w-full min-w-[480px] table-fixed border-collapse text-xs">
              <thead>
                <tr className="bg-slate-100 text-left text-[11px] font-semibold text-slate-600">
                  <th className="w-[150px] px-5 py-2.5 font-semibold">Employee</th>
                  <th className="px-5 py-2.5 font-semibold">Project</th>
                  <th className="w-[80px] px-5 py-2.5 font-semibold">Week</th>
                  <th className="w-[150px] px-5 py-2.5 font-semibold">Status</th>
                </tr>
              </thead>
              <tbody>
                {RECENT_REPORTS.map((report) => (
                  <tr key={report.employee} className="border-t border-slate-200">
                    <td className="truncate px-5 py-3 font-semibold text-slate-900">{report.employee}</td>
                    <td className="truncate px-5 py-3 text-slate-600">{report.project}</td>
                    <td className="whitespace-nowrap px-5 py-3 text-slate-600">{report.week}</td>
                    <td className="px-5 py-3">
                      <span className={`inline-flex items-center gap-1.5 whitespace-nowrap rounded px-2 py-1 text-[11px] font-semibold ${report.className}`}>
                        <span className="size-1.5 rounded-full bg-current" />
                        {report.status}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <p className="px-5 py-3 text-[11px] text-slate-400">Latest reports for Oct 21–27, 2024</p>
        </div>
      </div>

      <div className="flex flex-col gap-4 rounded-[10px] border border-slate-200 bg-white p-5">
        <div className="flex items-center justify-between gap-2">
          <p className="text-[15px] font-bold text-slate-900">Project Overview</p>
          <ViewAllLink />
        </div>
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center">
          <div className="flex w-full flex-col gap-2 sm:w-[200px] sm:shrink-0">
            <p className="text-[13px] font-semibold text-slate-900">12 total projects</p>
            <div className="flex items-center gap-2 text-xs">
              <span className="size-1.5 rounded-full bg-emerald-600" />
              <span className="text-emerald-700">9 active</span>
              <span className="text-slate-500">· 3 archived</span>
            </div>
          </div>
          {PROJECTS.map((project) => (
            <div
              key={project.name}
              className="flex min-w-0 flex-1 flex-col gap-2 border-t border-slate-200 pt-4 sm:border-t-0 sm:border-l sm:pt-0 sm:pl-5"
            >
              <div className="flex items-center justify-between gap-2">
                <p className="truncate text-xs font-semibold text-slate-900">{project.name}</p>
                <span className="inline-flex shrink-0 items-center gap-1.5 rounded bg-emerald-50 px-2 py-1 text-[11px] font-semibold text-emerald-700">
                  <span className="size-1.5 rounded-full bg-current" />
                  Active
                </span>
              </div>
              <p className="truncate text-[11px] text-slate-500">
                {project.owner} · {project.members} members
              </p>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
