import PageHeader from "@/components/layout/PageHeader";
import { Separator } from "@/components/ui/separator";

// Static mock data -- UI-only checkpoint, no team service wired yet. Status labels/colors match
// TeamReports.jsx's manager-facing phrasing (blue "Submitted"), not the employee-facing
// StatusBadge/REPORT_STATUSES -- same reasoning as that screen: different audience, same
// underlying ReportStatus value.
const MEMBERS = [
  {
    name: "Marcus Vance",
    initials: "MV",
    email: "m.vance@company.com",
    projects: ["Acme Platform Integration", "Database Migration"],
    status: "SUBMITTED",
    active: "Active 2 hours ago",
  },
  {
    name: "Sarah Connor",
    initials: "SC",
    email: "s.connor@company.com",
    projects: ["Global Billing Redesign", "Analytical Engine UI"],
    status: "SUBMITTED",
    active: "Active 5 mins ago",
  },
  {
    name: "Alex Mercer",
    initials: "AM",
    email: "a.mercer@company.com",
    projects: ["Security Audit Patching"],
    status: "NEEDS_CORRECTION",
    active: "Active 1 day ago",
  },
  {
    name: "Helena Rostova",
    initials: "HR",
    email: "h.rostova@company.com",
    projects: ["Global Billing Redesign", "Kernel Upgrade Phase 2"],
    status: "APPROVED",
    active: "Active 3 hours ago",
  },
  {
    name: "Linus Torvalds",
    initials: "LT",
    email: "l.torvalds@company.com",
    projects: ["Kernel Upgrade Phase 2"],
    status: "DRAFT",
    active: "Active 3 days ago",
  },
  {
    name: "Ada Lovelace",
    initials: "AL",
    email: "a.lovelace@company.com",
    projects: ["Analytical Engine UI", "Security Audit Patching"],
    status: "SUBMITTED",
    active: "Active 10 mins ago",
  },
];

const STATUS_CONFIG = {
  DRAFT: { label: "Draft", className: "bg-slate-100 text-slate-600 border-slate-200" },
  SUBMITTED: { label: "Submitted", className: "bg-blue-50 text-blue-700 border-blue-100" },
  NEEDS_CORRECTION: { label: "Needs Correction", className: "bg-amber-50 text-amber-700 border-amber-200" },
  APPROVED: { label: "Approved", className: "bg-emerald-50 text-emerald-700 border-emerald-200" },
};

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

function MemberCard({ member }) {
  return (
    <div className="flex flex-col gap-4 rounded-xl border border-slate-200 bg-white p-5 shadow-[0_1px_1.5px_rgba(0,0,0,0.04)]">
      <div className="flex flex-wrap items-start justify-between gap-2">
        <div className="flex min-w-0 items-center gap-3">
          <div className="flex size-12 shrink-0 items-center justify-center rounded-full bg-[#1a0b2e] text-base font-bold text-[#ffb7a5]">
            {member.initials}
          </div>
          <div className="flex min-w-0 flex-col gap-0.5">
            <p className="truncate text-[15px] font-bold text-slate-900">{member.name}</p>
            <p className="truncate text-xs text-slate-600">{member.email}</p>
          </div>
        </div>
        <span className="shrink-0 rounded-md bg-slate-100 px-2 py-1 text-[10px] font-semibold whitespace-nowrap text-slate-600">
          TEAM MEMBER
        </span>
      </div>

      <Separator />

      <div className="flex flex-col gap-2">
        <p className="text-[11px] font-semibold text-slate-400">ASSIGNED PROJECTS</p>
        <div className="flex flex-wrap gap-1.5">
          {member.projects.map((project) => (
            <span
              key={project}
              className="rounded border border-slate-200 bg-[#f8f8fa] px-2 py-1 text-[11px] text-slate-600"
            >
              {project}
            </span>
          ))}
        </div>
      </div>

      <div className="flex flex-wrap items-center justify-between gap-2 pt-1">
        <div className="flex items-center gap-1.5">
          <p className="text-[11px] font-semibold text-slate-400">WEEK 42 REPORT</p>
          <ManagerStatusBadge status={member.status} />
        </div>
        <p className="text-[11px] text-slate-400">{member.active}</p>
      </div>

      <div className="flex gap-3 border-t border-slate-200 pt-2 text-[13px]">
        <button type="button" className="font-bold text-[#1a0b2e] underline">
          View Reports
        </button>
        <button type="button" className="font-semibold text-slate-600 underline">
          View Profile
        </button>
      </div>
    </div>
  );
}

export default function TeamMembers() {
  return (
    <div className="flex flex-col gap-6">
      <PageHeader title="Team Members" />

      <div className="flex flex-wrap items-center justify-between gap-2">
        <div className="flex items-center gap-2">
          <p className="text-base font-bold text-slate-900">Direct Reports</p>
          <span className="rounded-full bg-[#1a0b2e] px-2 py-0.5 text-[11px] font-bold text-[#ffb7a5]">
            {MEMBERS.length} Members
          </span>
        </div>
        <p className="text-xs text-slate-600">* Members are added by HR Administrators</p>
      </div>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        {MEMBERS.map((member) => (
          <MemberCard key={member.email} member={member} />
        ))}
      </div>
    </div>
  );
}
