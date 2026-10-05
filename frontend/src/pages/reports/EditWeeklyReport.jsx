import { useState } from "react";
import { useParams, Link } from "react-router-dom";
import { AlertTriangle, Plus, Trash2 } from "lucide-react";
import StatusBadge from "@/components/common/StatusBadge";
import EmptyState from "@/components/feedback/EmptyState";
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
import { getReportById, reportDetail } from "./mockReports";

const PROJECT_OPTIONS = [
  "Acme Core Platform Integration",
  "Global Billing Architecture Redesign",
  "Internal Security Audit Patching",
];

const TASK_STATUSES = ["Not Started", "In Progress", "Completed"];
const TASK_PRIORITIES = ["Low", "Medium", "High"];

function newTask() {
  return { id: crypto.randomUUID(), text: "", status: "Not Started", priority: "Medium", hours: "" };
}

export default function EditWeeklyReport() {
  const { reportId } = useParams();
  const report = getReportById(reportId);
  const detail = report ? reportDetail(report) : null;

  const [project, setProject] = useState(PROJECT_OPTIONS[0]);
  const [summary, setSummary] = useState(detail?.summary ?? "");
  const [tasks, setTasks] = useState(detail?.tasks ?? []);
  const [achievements, setAchievements] = useState(detail?.achievements ?? "");
  const [blockers, setBlockers] = useState(detail?.blockers ?? "");

  if (!report) {
    return (
      <EmptyState
        title="Report not found"
        description="This weekly report doesn't exist or may have been removed."
        action={
          <Button asChild className="h-9 px-3.5 text-[13px] font-semibold">
            <Link to="/reports">Back to My Reports</Link>
          </Button>
        }
      />
    );
  }

  function updateTask(index, patch) {
    setTasks((rows) => rows.map((row, i) => (i === index ? { ...row, ...patch } : row)));
  }

  function removeTask(index) {
    setTasks((rows) => rows.filter((_, i) => i !== index));
  }

  // No submit handler wired to a service yet -- this checkpoint is UI only.
  function handleSubmit(event) {
    event.preventDefault();
  }

  const shortWeek = detail.weekRange.split(",")[0];

  return (
    <form onSubmit={handleSubmit} className="flex flex-col gap-6">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="flex flex-col gap-1">
          <p className="flex flex-wrap items-center gap-2 text-[13px]">
            <Link to="/reports" className="font-medium text-slate-500 hover:text-slate-700">
              My Reports
            </Link>
            <span className="text-slate-400">/</span>
            <span className="font-semibold text-slate-900">Edit Report ({shortWeek})</span>
          </p>
        </div>
        <StatusBadge status={report.status} />
      </div>

      {detail.feedback && (
        <div className="flex gap-4 rounded border-l-4 border-amber-300 bg-amber-50/40 p-4">
          <AlertTriangle className="size-5 shrink-0 text-amber-600" />
          <div className="flex flex-1 flex-col gap-1 text-[13px]">
            <p className="font-bold text-amber-700">
              Changes Requested by {detail.feedback.by} on {detail.feedback.date}
            </p>
            <p className="leading-relaxed text-slate-600">&ldquo;{detail.feedback.comment}&rdquo;</p>
          </div>
        </div>
      )}

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

          <div className="flex w-full flex-col gap-1.5 sm:w-[280px] sm:shrink-0">
            <Label htmlFor="reportingWeek" className="text-xs font-semibold text-slate-600">
              Reporting Week
            </Label>
            <Input
              id="reportingWeek"
              value={detail.weekRange}
              disabled
              className="h-10 rounded-md bg-slate-50 text-[13px]"
            />
          </div>
        </div>

        <div className="flex flex-col gap-1.5">
          <Label htmlFor="summary" className="text-xs font-semibold text-slate-600">
            Weekly Summary
          </Label>
          <Textarea
            id="summary"
            value={summary}
            onChange={(e) => setSummary(e.target.value)}
            className="min-h-[90px] rounded-lg bg-slate-50 text-[13px]"
          />
        </div>

        <Separator />

        <div className="flex flex-col gap-3">
          <div className="flex items-center justify-between">
            <p className="text-[13px] font-bold text-slate-900">Completed Tasks</p>
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={() => setTasks((rows) => [...rows, newTask()])}
              className="h-7 gap-1 px-2.5 text-[11px] font-semibold"
            >
              <Plus className="size-3" />
              Add Task
            </Button>
          </div>

          <div className="flex flex-col gap-2.5">
            {tasks.map((task, index) => (
              <div key={task.id} className="flex flex-col gap-2 sm:flex-row sm:items-center">
                <Input
                  value={task.text}
                  onChange={(e) => updateTask(index, { text: e.target.value })}
                  placeholder="Task description..."
                  className={`h-10 min-w-0 flex-1 rounded-md text-[13px] ${
                    task.flagged ? "border-amber-400 bg-white ring-1 ring-amber-400/40" : "bg-slate-50"
                  }`}
                />
                <div className="flex gap-2">
                  <Select value={task.status} onValueChange={(value) => updateTask(index, { status: value })}>
                    <SelectTrigger className="h-10 w-[130px] rounded-md bg-slate-50 text-[13px]">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      {TASK_STATUSES.map((option) => (
                        <SelectItem key={option} value={option}>
                          {option}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>

                  <Select value={task.priority} onValueChange={(value) => updateTask(index, { priority: value })}>
                    <SelectTrigger className="h-10 w-[110px] rounded-md bg-slate-50 text-[13px]">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      {TASK_PRIORITIES.map((option) => (
                        <SelectItem key={option} value={option}>
                          {option}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>

                  <Input
                    value={task.hours}
                    onChange={(e) => updateTask(index, { hours: e.target.value })}
                    placeholder="0.0"
                    className={`h-10 w-[90px] rounded-md text-center text-[13px] ${
                      task.flagged ? "border-amber-400 bg-white font-semibold ring-1 ring-amber-400/40" : "bg-slate-50"
                    }`}
                  />

                  <Button
                    type="button"
                    variant="ghost"
                    size="icon"
                    onClick={() => removeTask(index)}
                    className="h-10 w-9 shrink-0 text-slate-400 hover:text-red-600"
                    aria-label="Remove task"
                  >
                    <Trash2 className="size-4" />
                  </Button>
                </div>
              </div>
            ))}
            {tasks.length === 0 && (
              <p className="rounded-md border border-dashed border-slate-200 p-4 text-center text-xs text-slate-500">
                No tasks yet -- use "Add Task" to log one.
              </p>
            )}
          </div>
        </div>

        <Separator />

        <div className="flex flex-col gap-6 lg:flex-row">
          <div className="flex min-w-0 flex-1 flex-col gap-1.5">
            <div className="flex items-center justify-between text-xs">
              <Label htmlFor="achievements" className="font-semibold text-slate-600">
                Achievements
              </Label>
            </div>
            <Textarea
              id="achievements"
              value={achievements}
              onChange={(e) => setAchievements(e.target.value)}
              className="h-[90px] rounded-lg bg-slate-50 text-[13px]"
            />
          </div>

          <div className="flex min-w-0 flex-1 flex-col gap-1.5">
            <div className="flex items-center justify-between text-xs">
              <Label htmlFor="blockers" className="font-semibold text-slate-600">
                Blockers &amp; Issues
              </Label>
            </div>
            <Textarea
              id="blockers"
              value={blockers}
              onChange={(e) => setBlockers(e.target.value)}
              className="h-[90px] rounded-lg bg-slate-50 text-[13px]"
            />
          </div>
        </div>

        <Separator />

        <div className="flex justify-end gap-3">
          <Button type="button" variant="outline" className="h-10 px-4 text-[13px] font-semibold">
            Save Changes
          </Button>
          <Button type="submit" className="h-10 px-4 text-[13px] font-semibold">
            Resubmit Report
          </Button>
        </div>
      </div>
    </form>
  );
}
