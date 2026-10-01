import { useState } from "react";
import PageHeader from "@/components/layout/PageHeader";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Separator } from "@/components/ui/separator";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

// Static mock options -- UI-only checkpoint, no project/report service wired yet. Project names
// match the mock "Active Projects" list on the member dashboard so the two screens feel consistent.
const PROJECT_OPTIONS = [
  "Acme Core Platform Integration",
  "Global Billing Architecture Redesign",
  "Internal Security Audit Patching",
];

const DAYS = ["Mon", "Tue", "Wed", "Thu", "Fri"];

const INITIAL_BREAKDOWN = [
  { project: "Acme Core Platform", hours: [4, 4, 6, 4, 2] },
  { project: "Global Billing Redesign", hours: [3, 2, 2, 4, 4] },
  { project: "Security Audit Patching", hours: [1, 2, 0, 0, 0.5] },
];

function rowTotal(hours) {
  return hours.reduce((sum, h) => sum + (Number(h) || 0), 0);
}

export default function CreateWeeklyReport() {
  const [project, setProject] = useState(PROJECT_OPTIONS[0]);
  const [totalHours, setTotalHours] = useState("38.5");
  const [completedWork, setCompletedWork] = useState(
    "Finalized GraphQL API schema definition for Acme billing integration.\n" +
      "Drafted technical audit runbook and completed pre-compliance assessments.\n" +
      "Integrated dynamic progress tracking modules in Core UI design package."
  );
  const [blockers, setBlockers] = useState(
    "Pending sandbox API credentials from Acme client. Delayed staging tests."
  );
  const [nextWeekPlan, setNextWeekPlan] = useState(
    "1. Deploy patching configurations to staging environment.\n" +
      "2. Initiate full end-to-end integration mapping."
  );
  const [breakdown, setBreakdown] = useState(INITIAL_BREAKDOWN);

  function updateDayHours(rowIndex, dayIndex, value) {
    setBreakdown((rows) =>
      rows.map((row, i) =>
        i === rowIndex
          ? { ...row, hours: row.hours.map((h, d) => (d === dayIndex ? value : h)) }
          : row
      )
    );
  }

  // No submit handler wired to a service yet -- this checkpoint is UI only.
  function handleSubmit(event) {
    event.preventDefault();
  }

  return (
    <form onSubmit={handleSubmit} className="flex flex-col gap-6">
      <PageHeader
        title="New Weekly Report"
        description="Reporting Week: Week 42 (Oct 14 - Oct 20, 2024)"
        action={
          <div className="flex flex-wrap gap-3">
            <Button type="button" variant="outline" className="h-9 px-3.5 text-[13px] font-semibold">
              Save Draft
            </Button>
            <Button type="submit" className="h-9 px-3.5 text-[13px] font-semibold">
              Submit for Review
            </Button>
          </div>
        }
      />

      <div className="flex flex-col gap-6 rounded-xl border border-slate-200 bg-white p-6 shadow-[0_1px_1.5px_rgba(0,0,0,0.04)]">
        <div className="flex flex-col gap-6 sm:flex-row">
          <div className="flex min-w-0 flex-1 flex-col gap-1.5">
            <Label htmlFor="project" className="text-xs font-semibold text-slate-600">
              Primary Project Assignment
            </Label>
            <Select value={project} onValueChange={setProject}>
              <SelectTrigger id="project" className="h-10 w-full justify-between rounded-md bg-slate-50 px-3 text-[13px]">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {PROJECT_OPTIONS.map((option) => (
                  <SelectItem key={option} value={option}>
                    {option}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="flex w-full flex-col gap-1.5 sm:w-[200px] sm:shrink-0">
            <Label htmlFor="totalHours" className="text-xs font-semibold text-slate-600">
              Total Hours Logged
            </Label>
            <Input
              id="totalHours"
              type="number"
              step="0.5"
              min="0"
              value={totalHours}
              onChange={(e) => setTotalHours(e.target.value)}
              className="h-10 rounded-md bg-slate-50 text-[13px] font-semibold"
            />
          </div>
        </div>

        <Separator />

        <div className="flex flex-col gap-2">
          <Label htmlFor="completedWork" className="text-xs font-semibold text-slate-600">
            Completed Work (Bullets)
          </Label>
          <Textarea
            id="completedWork"
            value={completedWork}
            onChange={(e) => setCompletedWork(e.target.value)}
            placeholder="One item per line..."
            className="min-h-[110px] rounded-lg bg-slate-50 text-[13px]"
          />
        </div>

        <Separator />

        <div className="flex flex-col gap-6 lg:flex-row">
          <div className="flex min-w-0 flex-1 flex-col gap-2">
            <Label htmlFor="blockers" className="text-xs font-semibold text-slate-600">
              Blockers &amp; Issues
            </Label>
            <Textarea
              id="blockers"
              value={blockers}
              onChange={(e) => setBlockers(e.target.value)}
              className="h-[90px] rounded-lg bg-slate-50 text-[13px]"
            />
          </div>

          <div className="flex min-w-0 flex-1 flex-col gap-2">
            <Label htmlFor="nextWeekPlan" className="text-xs font-semibold text-slate-600">
              Plan for Next Week
            </Label>
            <Textarea
              id="nextWeekPlan"
              value={nextWeekPlan}
              onChange={(e) => setNextWeekPlan(e.target.value)}
              className="h-[90px] rounded-lg bg-slate-50 text-[13px]"
            />
          </div>
        </div>

        <Separator />

        <div className="flex flex-col gap-3">
          <p className="text-xs font-semibold text-slate-600">Daily Hours Breakdown</p>
          <div className="overflow-x-auto rounded-lg border border-slate-200">
            <table className="w-full min-w-[560px] border-collapse text-[13px]">
              <thead>
                <tr className="bg-slate-100 text-[11px] font-semibold text-slate-600">
                  <th className="px-3 py-2 text-left">Project</th>
                  {DAYS.map((day) => (
                    <th key={day} className="w-[60px] px-2 py-2 text-center">
                      {day}
                    </th>
                  ))}
                  <th className="w-[80px] px-3 py-2 text-right">Total</th>
                </tr>
              </thead>
              <tbody>
                {breakdown.map((row, rowIndex) => (
                  <tr key={row.project} className="border-t border-slate-200 bg-white">
                    <td className="truncate px-3 py-2 text-slate-900">{row.project}</td>
                    {row.hours.map((hours, dayIndex) => (
                      <td key={DAYS[dayIndex]} className="px-2 py-1.5 text-center">
                        <input
                          type="number"
                          step="0.5"
                          min="0"
                          value={hours}
                          onChange={(e) => updateDayHours(rowIndex, dayIndex, e.target.value)}
                          className="h-7 w-12 rounded border border-slate-200 bg-slate-50 text-center text-[13px] outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50"
                        />
                      </td>
                    ))}
                    <td className="px-3 py-2 text-right font-semibold text-slate-900">
                      {rowTotal(row.hours)} hrs
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </form>
  );
}
