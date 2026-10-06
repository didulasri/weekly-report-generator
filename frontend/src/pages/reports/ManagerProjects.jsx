import { useState } from "react";
import { Download } from "lucide-react";
import PageHeader from "@/components/layout/PageHeader";
import { Button } from "@/components/ui/button";

// Static mock data -- UI-only checkpoint, no project service wired yet.
const PROJECTS = [
  {
    name: "Acme Platform Integration",
    description: "Connecting client SaaS system with external payment infrastructure.",
    status: "ACTIVE",
    team: ["MV", "AL", "GH"],
    extraMembers: 1,
    submissions: 3,
    compliance: 75,
  },
  {
    name: "Global Billing Redesign",
    description: "Revamping enterprise billing architecture to scale multicurrency support.",
    status: "ACTIVE",
    team: ["SC", "HR"],
    extraMembers: 0,
    submissions: 2,
    compliance: 100,
  },
  {
    name: "Security Audit Patching",
    description: "Addressing quarterly critical infrastructure security updates.",
    status: "ACTIVE",
    team: ["AM", "AL"],
    extraMembers: 0,
    submissions: 1,
    compliance: 50,
  },
  {
    name: "Kernel Upgrade Phase 2",
    description: "Upgrading primary cluster OS builds to LTS versions.",
    status: "ACTIVE",
    team: ["LT", "HR"],
    extraMembers: 0,
    submissions: 1,
    compliance: 50,
  },
  {
    name: "Analytical Engine UI",
    description: "Designing dashboard panels for high-performance data computation.",
    status: "ON_HOLD",
    team: ["AL", "SC"],
    extraMembers: 1,
    submissions: 2,
    compliance: 66,
  },
  {
    name: "MS-DOS Legacy Support",
    description: "Addressing emergency maintenance requests for legacy DOS pipeline.",
    status: "COMPLETED",
    team: ["BG", "GH"],
    extraMembers: 0,
    submissions: 2,
    compliance: 100,
  },
];

const STATUS_CONFIG = {
  ACTIVE: { label: "Active", className: "bg-sky-50 text-sky-600 border-sky-200" },
  ON_HOLD: { label: "On Hold", className: "bg-purple-50 text-purple-600 border-purple-200" },
  COMPLETED: { label: "Completed", className: "bg-emerald-50 text-emerald-700 border-emerald-200" },
};

const TABS = [
  { value: "ALL", label: "All" },
  { value: "ACTIVE", label: "Active" },
  { value: "ON_HOLD", label: "On Hold" },
  { value: "COMPLETED", label: "Completed" },
];

function tabCount(value) {
  if (value === "ALL") return PROJECTS.length;
  return PROJECTS.filter((p) => p.status === value).length;
}

function StatusBadge({ status }) {
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

function TeamAvatars({ team, extraMembers }) {
  return (
    <div className="flex items-center">
      {team.map((initials, i) => (
        <div
          key={initials + i}
          className="flex size-6 items-center justify-center rounded-full border-2 border-white bg-[#1a0b2e] text-[9px] font-bold text-[#ffb7a5]"
          style={{ marginLeft: i === 0 ? 0 : -6 }}
        >
          {initials}
        </div>
      ))}
      {extraMembers > 0 && (
        <div
          className="flex size-6 items-center justify-center rounded-full border-2 border-white bg-slate-100 text-[9px] font-bold text-slate-600"
          style={{ marginLeft: -6 }}
        >
          +{extraMembers}
        </div>
      )}
    </div>
  );
}

function ComplianceBar({ value }) {
  const isComplete = value >= 100;
  return (
    <div className="flex items-center gap-2">
      <div className="h-1.5 w-[70px] overflow-hidden rounded-full bg-slate-200">
        <div
          className={`h-full rounded-full ${isComplete ? "bg-emerald-500" : "bg-amber-500"}`}
          style={{ width: `${Math.min(value, 100)}%` }}
        />
      </div>
      <p className={`text-xs font-bold ${isComplete ? "text-emerald-500" : "text-amber-600"}`}>{value}%</p>
    </div>
  );
}

export default function ManagerProjects() {
  const [tab, setTab] = useState("ALL");

  const filtered = tab === "ALL" ? PROJECTS : PROJECTS.filter((p) => p.status === tab);

  return (
    <div className="flex flex-col gap-4">
      <PageHeader title="Projects" />

      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex flex-wrap items-center gap-2">
          {TABS.map((t) => (
            <button
              key={t.value}
              type="button"
              onClick={() => setTab(t.value)}
              className={`flex items-center gap-2 rounded-lg border px-4 py-2 text-[13px] transition-colors ${
                tab === t.value
                  ? "border-slate-200 bg-white font-bold text-slate-900"
                  : "border-transparent font-medium text-slate-600 hover:bg-white"
              }`}
            >
              {t.label}
              <span
                className={`rounded-full px-1.5 py-0.5 text-[10px] font-bold ${
                  tab === t.value ? "bg-[#1a0b2e] text-[#ffb7a5]" : "bg-slate-200 text-slate-600"
                }`}
              >
                {tabCount(t.value)}
              </span>
            </button>
          ))}
        </div>

        <Button type="button" variant="outline" className="h-9 gap-2 px-4 text-[13px] font-semibold">
          <Download className="size-3.5" />
          Export Overview
        </Button>
      </div>

      <div className="flex flex-col rounded-xl border border-slate-200 bg-white shadow-[0_1px_1.5px_rgba(0,0,0,0.04)]">
        <div className="overflow-x-auto">
          <table className="w-full min-w-[940px] table-fixed border-collapse text-[13px]">
            <thead>
              <tr className="bg-slate-50 text-left text-xs font-bold text-slate-600">
                <th className="w-[190px] px-4 py-3.5 font-bold">Project Name</th>
                <th className="px-4 py-3.5 font-bold">Description</th>
                <th className="w-[90px] px-4 py-3.5 font-bold">Status</th>
                <th className="w-[100px] px-4 py-3.5 font-bold">Team Members</th>
                <th className="w-[110px] px-4 py-3.5 text-center font-bold">Reports (W42)</th>
                <th className="w-[140px] px-4 py-3.5 font-bold">Compliance</th>
                <th className="w-[64px] px-4 py-3.5 text-right font-bold">Action</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((project) => (
                <tr key={project.name} className="border-t border-slate-200">
                  <td className="truncate px-4 py-4 font-bold text-slate-900">{project.name}</td>
                  <td className="truncate px-4 py-4 text-slate-600">{project.description}</td>
                  <td className="whitespace-nowrap px-4 py-4">
                    <StatusBadge status={project.status} />
                  </td>
                  <td className="px-4 py-4">
                    <TeamAvatars team={project.team} extraMembers={project.extraMembers} />
                  </td>
                  <td className="whitespace-nowrap px-4 py-4 text-center font-semibold text-slate-900">
                    {project.submissions} Submissions
                  </td>
                  <td className="whitespace-nowrap px-4 py-4">
                    <ComplianceBar value={project.compliance} />
                  </td>
                  <td className="px-4 py-4 text-right">
                    <button type="button" className="text-xs font-bold text-[#1a0b2e] underline">
                      View
                    </button>
                  </td>
                </tr>
              ))}
              {filtered.length === 0 && (
                <tr>
                  <td colSpan={7} className="px-4 py-10 text-center text-sm text-slate-500">
                    No projects in this status.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
