import { useState } from "react";
import { AlertTriangle, Loader2, Plus, Trash2 } from "lucide-react";
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
import { useProjects } from "@/hooks/useProjects";
import { mondayOf, formatWeekLabel } from "@/utils/weekDates";

const TASK_STATUS_OPTIONS = ["NOT_STARTED", "IN_PROGRESS", "COMPLETED", "BLOCKED"];
const PRIORITY_OPTIONS = ["LOW", "MEDIUM", "HIGH"];
const BLOCKER_STATUS_OPTIONS = ["OPEN", "RESOLVED"];
const IMPACT_OPTIONS = ["LOW", "MEDIUM", "HIGH", "CRITICAL"];
const WORK_CATEGORY_OPTIONS = ["DEVELOPMENT", "TESTING", "MEETINGS", "DOCUMENTATION", "RESEARCH", "OTHER"];

function titleCase(enumValue) {
  return enumValue
    .toLowerCase()
    .split("_")
    .map((word) => word[0].toUpperCase() + word.slice(1))
    .join(" ");
}

function newTask() {
  return {
    key: crypto.randomUUID(),
    taskName: "",
    description: "",
    status: "NOT_STARTED",
    priority: "MEDIUM",
    plannedPercentage: 0,
    actualPercentage: 0,
    hoursPlanned: "",
    hoursSpent: "",
    deliverable: "",
  };
}

function newNextWeekTask() {
  return { key: crypto.randomUUID(), taskName: "", description: "", priority: "MEDIUM" };
}

function newBlocker() {
  return {
    key: crypto.randomUUID(),
    title: "",
    description: "",
    impact: "MEDIUM",
    status: "OPEN",
    keyIssue: false,
  };
}

function newAchievement() {
  return { key: crypto.randomUUID(), title: "", description: "", keyAchievement: false };
}

function newWorkHour() {
  return { key: crypto.randomUUID(), taskType: "DEVELOPMENT", hours: "" };
}

// Response DTOs carry a server `id`; request DTOs don't accept one at all (update clears and
// rebuilds every child collection server-side), so every row just needs a client-only `key` for
// React -- initializeRows maps either an existing report's children or a blank first row into that
// shape, and toPayload strips `key` back off before the request goes out.
function initializeRows(existingRows, factory) {
  if (!existingRows || existingRows.length === 0) return [factory()];
  return existingRows.map((row) => ({ ...row, key: crypto.randomUUID() }));
}

export default function ReportForm({ mode, report, onSaveDraft, onSubmitForReview, isSaving, isSubmitting }) {
  const locked = mode === "edit" && report && (report.currentVersion > 1 || report.status === "NEEDS_CORRECTION");

  const [projectId, setProjectId] = useState(report?.projectId ? String(report.projectId) : "");
  const [weekStartDate, setWeekStartDate] = useState(report?.weekStartDate ?? "");
  const [weekEndDate, setWeekEndDate] = useState(report?.weekEndDate ?? "");
  const [summary, setSummary] = useState(report?.summary ?? "");
  const [notes, setNotes] = useState(report?.notes ?? "");
  const [tasks, setTasks] = useState(() => initializeRows(report?.tasks, newTask));
  const [nextWeekTasks, setNextWeekTasks] = useState(() => initializeRows(report?.nextWeekTasks, newNextWeekTask));
  const [blockers, setBlockers] = useState(() => initializeRows(report?.blockers, newBlocker));
  const [achievements, setAchievements] = useState(() => initializeRows(report?.achievements, newAchievement));
  const [workHours, setWorkHours] = useState(() => initializeRows(report?.workHours, newWorkHour));

  const projectsQuery = useProjects({ status: "ACTIVE", size: 100 });
  const projects = projectsQuery.data?.content ?? [];

  function handleWeekStartChange(value) {
    if (!value) {
      setWeekStartDate("");
      setWeekEndDate("");
      return;
    }
    const week = mondayOf(value);
    setWeekStartDate(week.start);
    setWeekEndDate(week.end);
  }

  function buildPayload() {
    const clean = (rows, mapRow) => rows.map(mapRow).filter((row) => row !== null);

    return {
      projectId: projectId ? Number(projectId) : null,
      weekStartDate,
      weekEndDate,
      summary,
      notes,
      tasks: clean(tasks, (t) =>
        t.taskName.trim()
          ? {
              taskName: t.taskName,
              description: t.description,
              status: t.status,
              priority: t.priority,
              plannedPercentage: Number(t.plannedPercentage) || 0,
              actualPercentage: Number(t.actualPercentage) || 0,
              hoursPlanned: Number(t.hoursPlanned) || 0,
              hoursSpent: Number(t.hoursSpent) || 0,
              deliverable: t.deliverable,
            }
          : null
      ),
      nextWeekTasks: clean(nextWeekTasks, (t) =>
        t.taskName.trim()
          ? { taskName: t.taskName, description: t.description, priority: t.priority }
          : null
      ),
      blockers: clean(blockers, (b) =>
        b.title.trim()
          ? {
              title: b.title,
              description: b.description,
              impact: b.impact,
              status: b.status,
              keyIssue: b.keyIssue,
            }
          : null
      ),
      achievements: clean(achievements, (a) =>
        a.title.trim()
          ? { title: a.title, description: a.description, keyAchievement: a.keyAchievement }
          : null
      ),
      workHours: clean(workHours, (w) => (w.hours !== "" ? { taskType: w.taskType, hours: Number(w.hours) || 0 } : null)),
    };
  }

  function handleSaveDraft(event) {
    event.preventDefault();
    onSaveDraft(buildPayload());
  }

  function handleSubmitForReview(event) {
    event.preventDefault();
    onSubmitForReview(buildPayload());
  }

  const usedWorkCategories = (exceptKey) =>
    new Set(workHours.filter((w) => w.key !== exceptKey).map((w) => w.taskType));

  const latestReview = report?.reviews?.[0];

  const submitLabel = mode === "edit" && report?.status === "NEEDS_CORRECTION" ? "Resubmit Report" : "Submit for Review";

  return (
    <form className="flex flex-col gap-6">
      {latestReview && report?.status === "NEEDS_CORRECTION" && (
        <div className="flex gap-4 rounded border-l-4 border-amber-300 bg-amber-50/40 p-4">
          <AlertTriangle className="size-5 shrink-0 text-amber-600" />
          <div className="flex flex-1 flex-col gap-1 text-[13px]">
            <p className="font-bold text-amber-700">
              Changes Requested by {latestReview.reviewerName} on{" "}
              {new Date(latestReview.reviewedAt).toLocaleDateString("en-US", {
                month: "short",
                day: "numeric",
                year: "numeric",
              })}
            </p>
            {latestReview.comment && (
              <p className="leading-relaxed text-slate-600">&ldquo;{latestReview.comment}&rdquo;</p>
            )}
          </div>
        </div>
      )}

      <div className="flex flex-col gap-6 rounded-xl border border-slate-200 bg-white p-6 shadow-[0_1px_1.5px_rgba(0,0,0,0.04)]">
        <div className="flex flex-col gap-6 sm:flex-row">
          <div className="flex min-w-0 flex-1 flex-col gap-1.5">
            <Label htmlFor="project" className="text-xs font-semibold text-slate-600">
              Project
            </Label>
            <Select value={projectId} onValueChange={setProjectId} disabled={locked}>
              <SelectTrigger id="project" className="h-10 w-full justify-between rounded-md bg-slate-50 px-3 text-[13px]">
                <SelectValue placeholder={projectsQuery.isPending ? "Loading projects…" : "Select a project"} />
              </SelectTrigger>
              <SelectContent>
                {projects.map((p) => (
                  <SelectItem key={p.id} value={String(p.id)}>
                    {p.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="flex w-full flex-col gap-1.5 sm:w-[220px] sm:shrink-0">
            <Label htmlFor="weekStart" className="text-xs font-semibold text-slate-600">
              Reporting Week
            </Label>
            <Input
              id="weekStart"
              type="date"
              value={weekStartDate}
              disabled={locked}
              onChange={(e) => handleWeekStartChange(e.target.value)}
              className="h-10 rounded-md bg-slate-50 text-[13px]"
            />
            {weekStartDate && weekEndDate && (
              <p className="text-[11px] text-slate-500">
                Week of {formatWeekLabel(weekStartDate, weekEndDate)} (auto-snapped to Monday)
              </p>
            )}
          </div>
        </div>

        <div className="flex flex-col gap-1.5">
          <Label htmlFor="summary" className="text-xs font-semibold text-slate-600">
            Weekly Summary <span className="text-slate-400">(required to submit)</span>
          </Label>
          <Textarea
            id="summary"
            value={summary}
            onChange={(e) => setSummary(e.target.value)}
            placeholder="What did you work on this week?"
            className="min-h-[90px] rounded-lg bg-slate-50 text-[13px]"
          />
        </div>

        <div className="flex flex-col gap-1.5">
          <Label htmlFor="notes" className="text-xs font-semibold text-slate-600">
            Notes <span className="text-slate-400">(optional)</span>
          </Label>
          <Textarea
            id="notes"
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
            className="min-h-[70px] rounded-lg bg-slate-50 text-[13px]"
          />
        </div>

        <Separator />

        {/* Tasks */}
        <div className="flex flex-col gap-3">
          <div className="flex items-center justify-between">
            <p className="text-[13px] font-bold text-slate-900">
              Tasks <span className="font-normal text-slate-400">(at least one required to submit)</span>
            </p>
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

          <div className="flex flex-col gap-3">
            {tasks.map((task, index) => (
              <div key={task.key} className="flex flex-col gap-2.5 rounded-lg border border-slate-200 p-3">
                <div className="flex items-start gap-2">
                  <Input
                    value={task.taskName}
                    onChange={(e) =>
                      setTasks((rows) => rows.map((r, i) => (i === index ? { ...r, taskName: e.target.value } : r)))
                    }
                    placeholder="Task name"
                    className="h-9 min-w-0 flex-1 rounded-md bg-slate-50 text-[13px]"
                  />
                  <Button
                    type="button"
                    variant="ghost"
                    size="icon"
                    disabled={tasks.length === 1}
                    onClick={() => setTasks((rows) => rows.filter((_, i) => i !== index))}
                    className="h-9 w-9 shrink-0 text-slate-400 hover:text-red-600"
                    aria-label="Remove task"
                  >
                    <Trash2 className="size-4" />
                  </Button>
                </div>
                <Input
                  value={task.description}
                  onChange={(e) =>
                    setTasks((rows) => rows.map((r, i) => (i === index ? { ...r, description: e.target.value } : r)))
                  }
                  placeholder="Description (optional)"
                  className="h-9 rounded-md bg-slate-50 text-[13px]"
                />
                <div className="grid grid-cols-2 gap-2 sm:grid-cols-6">
                  <Select
                    value={task.status}
                    onValueChange={(value) =>
                      setTasks((rows) => rows.map((r, i) => (i === index ? { ...r, status: value } : r)))
                    }
                  >
                    <SelectTrigger className="h-9 rounded-md bg-slate-50 text-[12px]">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      {TASK_STATUS_OPTIONS.map((option) => (
                        <SelectItem key={option} value={option}>
                          {titleCase(option)}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>

                  <Select
                    value={task.priority}
                    onValueChange={(value) =>
                      setTasks((rows) => rows.map((r, i) => (i === index ? { ...r, priority: value } : r)))
                    }
                  >
                    <SelectTrigger className="h-9 rounded-md bg-slate-50 text-[12px]">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      {PRIORITY_OPTIONS.map((option) => (
                        <SelectItem key={option} value={option}>
                          {titleCase(option)}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>

                  <div className="flex flex-col gap-0.5">
                    <Input
                      type="number"
                      min="0"
                      max="100"
                      value={task.plannedPercentage}
                      onChange={(e) =>
                        setTasks((rows) =>
                          rows.map((r, i) => (i === index ? { ...r, plannedPercentage: e.target.value } : r))
                        )
                      }
                      className="h-9 rounded-md bg-slate-50 text-center text-[12px]"
                    />
                    <p className="text-center text-[10px] text-slate-400">Planned %</p>
                  </div>

                  <div className="flex flex-col gap-0.5">
                    <Input
                      type="number"
                      min="0"
                      max="100"
                      value={task.actualPercentage}
                      onChange={(e) =>
                        setTasks((rows) =>
                          rows.map((r, i) => (i === index ? { ...r, actualPercentage: e.target.value } : r))
                        )
                      }
                      className="h-9 rounded-md bg-slate-50 text-center text-[12px]"
                    />
                    <p className="text-center text-[10px] text-slate-400">Actual %</p>
                  </div>

                  <div className="flex flex-col gap-0.5">
                    <Input
                      type="number"
                      step="0.5"
                      min="0"
                      value={task.hoursPlanned}
                      onChange={(e) =>
                        setTasks((rows) =>
                          rows.map((r, i) => (i === index ? { ...r, hoursPlanned: e.target.value } : r))
                        )
                      }
                      placeholder="0.0"
                      className="h-9 rounded-md bg-slate-50 text-center text-[12px]"
                    />
                    <p className="text-center text-[10px] text-slate-400">Hrs Planned</p>
                  </div>

                  <div className="flex flex-col gap-0.5">
                    <Input
                      type="number"
                      step="0.5"
                      min="0"
                      value={task.hoursSpent}
                      onChange={(e) =>
                        setTasks((rows) =>
                          rows.map((r, i) => (i === index ? { ...r, hoursSpent: e.target.value } : r))
                        )
                      }
                      placeholder="0.0"
                      className="h-9 rounded-md bg-slate-50 text-center text-[12px]"
                    />
                    <p className="text-center text-[10px] text-slate-400">Hrs Spent</p>
                  </div>
                </div>
                <Input
                  value={task.deliverable}
                  onChange={(e) =>
                    setTasks((rows) => rows.map((r, i) => (i === index ? { ...r, deliverable: e.target.value } : r)))
                  }
                  placeholder="Deliverable (optional)"
                  className="h-9 rounded-md bg-slate-50 text-[13px]"
                />
              </div>
            ))}
          </div>
        </div>

        <Separator />

        {/* Next Week Tasks */}
        <div className="flex flex-col gap-3">
          <div className="flex items-center justify-between">
            <p className="text-[13px] font-bold text-slate-900">Plan for Next Week</p>
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={() => setNextWeekTasks((rows) => [...rows, newNextWeekTask()])}
              className="h-7 gap-1 px-2.5 text-[11px] font-semibold"
            >
              <Plus className="size-3" />
              Add
            </Button>
          </div>
          <div className="flex flex-col gap-2">
            {nextWeekTasks.map((task, index) => (
              <div key={task.key} className="flex flex-col gap-2 sm:flex-row sm:items-center">
                <Input
                  value={task.taskName}
                  onChange={(e) =>
                    setNextWeekTasks((rows) =>
                      rows.map((r, i) => (i === index ? { ...r, taskName: e.target.value } : r))
                    )
                  }
                  placeholder="Planned task"
                  className="h-9 min-w-0 flex-1 rounded-md bg-slate-50 text-[13px]"
                />
                <Select
                  value={task.priority}
                  onValueChange={(value) =>
                    setNextWeekTasks((rows) => rows.map((r, i) => (i === index ? { ...r, priority: value } : r)))
                  }
                >
                  <SelectTrigger className="h-9 w-[110px] shrink-0 rounded-md bg-slate-50 text-[12px]">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {PRIORITY_OPTIONS.map((option) => (
                      <SelectItem key={option} value={option}>
                        {titleCase(option)}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                <Button
                  type="button"
                  variant="ghost"
                  size="icon"
                  onClick={() => setNextWeekTasks((rows) => rows.filter((_, i) => i !== index))}
                  className="h-9 w-9 shrink-0 text-slate-400 hover:text-red-600"
                  aria-label="Remove"
                >
                  <Trash2 className="size-4" />
                </Button>
              </div>
            ))}
          </div>
        </div>

        <Separator />

        <div className="flex flex-col gap-6 lg:flex-row">
          {/* Blockers */}
          <div className="flex min-w-0 flex-1 flex-col gap-3">
            <div className="flex items-center justify-between">
              <p className="text-[13px] font-bold text-slate-900">Blockers &amp; Issues</p>
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={() => setBlockers((rows) => [...rows, newBlocker()])}
                className="h-7 gap-1 px-2.5 text-[11px] font-semibold"
              >
                <Plus className="size-3" />
                Add
              </Button>
            </div>
            {blockers.length > 1 && (
              <p className="text-[11px] text-slate-500">Mark exactly one as the key issue.</p>
            )}
            <div className="flex flex-col gap-2.5">
              {blockers.map((blocker, index) => (
                <div key={blocker.key} className="flex flex-col gap-2 rounded-lg border border-slate-200 p-3">
                  <div className="flex items-start gap-2">
                    <Input
                      value={blocker.title}
                      onChange={(e) =>
                        setBlockers((rows) => rows.map((r, i) => (i === index ? { ...r, title: e.target.value } : r)))
                      }
                      placeholder="Blocker title"
                      className="h-9 min-w-0 flex-1 rounded-md bg-slate-50 text-[13px]"
                    />
                    <Button
                      type="button"
                      variant="ghost"
                      size="icon"
                      onClick={() => setBlockers((rows) => rows.filter((_, i) => i !== index))}
                      className="h-9 w-9 shrink-0 text-slate-400 hover:text-red-600"
                      aria-label="Remove blocker"
                    >
                      <Trash2 className="size-4" />
                    </Button>
                  </div>
                  <div className="flex gap-2">
                    <Select
                      value={blocker.impact}
                      onValueChange={(value) =>
                        setBlockers((rows) => rows.map((r, i) => (i === index ? { ...r, impact: value } : r)))
                      }
                    >
                      <SelectTrigger className="h-9 flex-1 rounded-md bg-slate-50 text-[12px]">
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        {IMPACT_OPTIONS.map((option) => (
                          <SelectItem key={option} value={option}>
                            {titleCase(option)} Impact
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                    <Select
                      value={blocker.status}
                      onValueChange={(value) =>
                        setBlockers((rows) => rows.map((r, i) => (i === index ? { ...r, status: value } : r)))
                      }
                    >
                      <SelectTrigger className="h-9 flex-1 rounded-md bg-slate-50 text-[12px]">
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        {BLOCKER_STATUS_OPTIONS.map((option) => (
                          <SelectItem key={option} value={option}>
                            {titleCase(option)}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>
                  <label className="flex items-center gap-2 text-[12px] text-slate-600">
                    <input
                      type="checkbox"
                      checked={blocker.keyIssue}
                      onChange={(e) =>
                        setBlockers((rows) =>
                          rows.map((r, i) => ({ ...r, keyIssue: i === index ? e.target.checked : false }))
                        )
                      }
                      className="size-3.5 rounded border-slate-300"
                    />
                    Key issue
                  </label>
                </div>
              ))}
            </div>
          </div>

          {/* Achievements */}
          <div className="flex min-w-0 flex-1 flex-col gap-3">
            <div className="flex items-center justify-between">
              <p className="text-[13px] font-bold text-slate-900">Achievements</p>
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={() => setAchievements((rows) => [...rows, newAchievement()])}
                className="h-7 gap-1 px-2.5 text-[11px] font-semibold"
              >
                <Plus className="size-3" />
                Add
              </Button>
            </div>
            {achievements.length > 1 && (
              <p className="text-[11px] text-slate-500">Mark exactly one as the key achievement.</p>
            )}
            <div className="flex flex-col gap-2.5">
              {achievements.map((achievement, index) => (
                <div key={achievement.key} className="flex flex-col gap-2 rounded-lg border border-slate-200 p-3">
                  <div className="flex items-start gap-2">
                    <Input
                      value={achievement.title}
                      onChange={(e) =>
                        setAchievements((rows) =>
                          rows.map((r, i) => (i === index ? { ...r, title: e.target.value } : r))
                        )
                      }
                      placeholder="Achievement title"
                      className="h-9 min-w-0 flex-1 rounded-md bg-slate-50 text-[13px]"
                    />
                    <Button
                      type="button"
                      variant="ghost"
                      size="icon"
                      onClick={() => setAchievements((rows) => rows.filter((_, i) => i !== index))}
                      className="h-9 w-9 shrink-0 text-slate-400 hover:text-red-600"
                      aria-label="Remove achievement"
                    >
                      <Trash2 className="size-4" />
                    </Button>
                  </div>
                  <label className="flex items-center gap-2 text-[12px] text-slate-600">
                    <input
                      type="checkbox"
                      checked={achievement.keyAchievement}
                      onChange={(e) =>
                        setAchievements((rows) =>
                          rows.map((r, i) => ({ ...r, keyAchievement: i === index ? e.target.checked : false }))
                        )
                      }
                      className="size-3.5 rounded border-slate-300"
                    />
                    Key achievement
                  </label>
                </div>
              ))}
            </div>
          </div>
        </div>

        <Separator />

        {/* Work Hours */}
        <div className="flex flex-col gap-3">
          <div className="flex items-center justify-between">
            <p className="text-[13px] font-bold text-slate-900">Hours by Category</p>
            <Button
              type="button"
              variant="outline"
              size="sm"
              disabled={workHours.length >= WORK_CATEGORY_OPTIONS.length}
              onClick={() => setWorkHours((rows) => [...rows, newWorkHour()])}
              className="h-7 gap-1 px-2.5 text-[11px] font-semibold"
            >
              <Plus className="size-3" />
              Add
            </Button>
          </div>
          <div className="flex flex-col gap-2">
            {workHours.map((row, index) => {
              const used = usedWorkCategories(row.key);
              return (
                <div key={row.key} className="flex items-center gap-2">
                  <Select
                    value={row.taskType}
                    onValueChange={(value) =>
                      setWorkHours((rows) => rows.map((r, i) => (i === index ? { ...r, taskType: value } : r)))
                    }
                  >
                    <SelectTrigger className="h-9 min-w-0 flex-1 rounded-md bg-slate-50 text-[13px]">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      {WORK_CATEGORY_OPTIONS.filter((option) => !used.has(option) || option === row.taskType).map(
                        (option) => (
                          <SelectItem key={option} value={option}>
                            {titleCase(option)}
                          </SelectItem>
                        )
                      )}
                    </SelectContent>
                  </Select>
                  <Input
                    type="number"
                    step="0.5"
                    min="0"
                    value={row.hours}
                    onChange={(e) =>
                      setWorkHours((rows) => rows.map((r, i) => (i === index ? { ...r, hours: e.target.value } : r)))
                    }
                    placeholder="Hours"
                    className="h-9 w-[100px] shrink-0 rounded-md bg-slate-50 text-center text-[13px]"
                  />
                  <Button
                    type="button"
                    variant="ghost"
                    size="icon"
                    onClick={() => setWorkHours((rows) => rows.filter((_, i) => i !== index))}
                    className="h-9 w-9 shrink-0 text-slate-400 hover:text-red-600"
                    aria-label="Remove"
                  >
                    <Trash2 className="size-4" />
                  </Button>
                </div>
              );
            })}
          </div>
        </div>

        <Separator />

        <div className="flex justify-end gap-3">
          <Button
            type="button"
            variant="outline"
            disabled={isSaving || isSubmitting}
            onClick={handleSaveDraft}
            className="h-10 px-4 text-[13px] font-semibold"
          >
            {isSaving ? <Loader2 className="size-4 animate-spin" /> : "Save Draft"}
          </Button>
          <Button
            type="button"
            disabled={isSaving || isSubmitting}
            onClick={handleSubmitForReview}
            className="h-10 px-4 text-[13px] font-semibold"
          >
            {isSubmitting ? <Loader2 className="size-4 animate-spin" /> : submitLabel}
          </Button>
        </div>
      </div>
    </form>
  );
}
