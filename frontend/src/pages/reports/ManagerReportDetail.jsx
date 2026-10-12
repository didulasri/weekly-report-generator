import { useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { CheckCircle2, Loader2, XCircle } from "lucide-react";
import { toast } from "sonner";
import StatusBadge from "@/components/common/StatusBadge";
import EmptyState from "@/components/feedback/EmptyState";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Separator } from "@/components/ui/separator";
import { useApproveReport, useManagerReportDetail, useRequestChanges } from "@/hooks/useManagerReports";
import { getApiErrorMessage } from "@/utils/apiError";

function titleCase(enumValue) {
  if (!enumValue) return "—";
  return enumValue
    .toLowerCase()
    .split("_")
    .map((word) => word[0].toUpperCase() + word.slice(1))
    .join(" ");
}

function formatDate(isoString) {
  if (!isoString) return "—";
  return new Date(isoString).toLocaleDateString("en-US", { month: "short", day: "numeric", year: "numeric" });
}

function formatDateTime(isoString) {
  if (!isoString) return "—";
  return new Date(isoString).toLocaleString("en-US", {
    month: "short",
    day: "numeric",
    year: "numeric",
    hour: "numeric",
    minute: "2-digit",
  });
}

const IMPACT_BADGE = {
  LOW: "bg-slate-50 text-slate-600 border-slate-200",
  MEDIUM: "bg-amber-50 text-amber-700 border-amber-200",
  HIGH: "bg-orange-50 text-orange-700 border-orange-200",
  CRITICAL: "bg-red-50 text-red-700 border-red-200",
};

function Badge({ children, className }) {
  return (
    <span
      className={`inline-flex items-center rounded border px-2 py-0.5 text-[11px] font-semibold uppercase ${
        className ?? "bg-slate-50 text-slate-600 border-slate-200"
      }`}
    >
      {children}
    </span>
  );
}

function Panel({ title, children }) {
  return (
    <div className="flex flex-col gap-3 rounded-xl border border-slate-200 bg-white p-6 shadow-[0_1px_1.5px_rgba(0,0,0,0.04)]">
      <p className="text-[15px] font-bold text-slate-900">{title}</p>
      {children}
    </div>
  );
}

export default function ManagerReportDetail() {
  const { reportId } = useParams();
  const navigate = useNavigate();
  const { data, isPending, isError, error } = useManagerReportDetail(reportId);
  const approveReport = useApproveReport();
  const requestChanges = useRequestChanges();
  const [comment, setComment] = useState("");

  if (isPending) {
    return (
      <div className="flex items-center justify-center gap-2 py-16 text-sm text-slate-500">
        <Loader2 className="size-4 animate-spin" />
        Loading report…
      </div>
    );
  }

  if (isError) {
    const notFound = error?.response?.status === 404;
    return (
      <EmptyState
        title={notFound ? "Report not found" : "Couldn't load this report"}
        description={notFound ? "This report doesn't exist or may have been removed." : getApiErrorMessage(error)}
        action={
          <Button asChild className="h-9 px-3.5 text-[13px] font-semibold">
            <Link to="/team">Back to Team Reports</Link>
          </Button>
        }
      />
    );
  }

  const { report, ownerName, ownerEmail, reviews, versions } = data;
  const canReview = report.status === "SUBMITTED";
  const isActing = approveReport.isPending || requestChanges.isPending;

  function handleApprove() {
    approveReport.mutate(
      { id: report.id, comment: comment.trim() || undefined },
      {
        onSuccess: () => {
          toast.success("Report approved");
          navigate("/team");
        },
        onError: (err) => toast.error(getApiErrorMessage(err)),
      }
    );
  }

  function handleRequestChanges() {
    if (comment.trim().length < 10) {
      toast.error("A comment of at least 10 characters is required to request changes");
      return;
    }
    requestChanges.mutate(
      { id: report.id, comment: comment.trim() },
      {
        onSuccess: () => {
          toast.success("Changes requested");
          navigate("/team");
        },
        onError: (err) => toast.error(getApiErrorMessage(err)),
      }
    );
  }

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <p className="flex flex-wrap items-center gap-2 text-[13px]">
          <Link to="/team" className="font-medium text-slate-500 hover:text-slate-700">
            Team Reports
          </Link>
          <span className="text-slate-400">/</span>
          <span className="font-semibold text-slate-900">
            {ownerName} — {formatDate(report.weekStartDate)}
          </span>
        </p>
        <StatusBadge status={report.status} />
      </div>

      <div className="flex flex-col gap-6 lg:flex-row">
        <div className="flex min-w-0 flex-1 flex-col gap-6">
          <Panel title="Report Overview">
            <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
              <div className="flex flex-col gap-1">
                <p className="text-[11px] font-semibold tracking-wide text-slate-400 uppercase">Employee</p>
                <p className="text-sm font-semibold text-slate-900">{ownerName}</p>
                <p className="text-xs text-slate-500">{ownerEmail}</p>
              </div>
              <div className="flex flex-col gap-1">
                <p className="text-[11px] font-semibold tracking-wide text-slate-400 uppercase">Project</p>
                <p className="text-sm font-semibold text-slate-900">{report.projectName}</p>
              </div>
              <div className="flex flex-col gap-1">
                <p className="text-[11px] font-semibold tracking-wide text-slate-400 uppercase">Week</p>
                <p className="text-sm font-semibold text-slate-900">
                  {formatDate(report.weekStartDate)} - {formatDate(report.weekEndDate)}
                </p>
              </div>
              <div className="flex flex-col gap-1">
                <p className="text-[11px] font-semibold tracking-wide text-slate-400 uppercase">Last Updated</p>
                <p className="text-sm font-semibold text-slate-900">{formatDate(report.updatedAt)}</p>
              </div>
            </div>

            <Separator />

            <div className="flex flex-col gap-1">
              <p className="text-xs font-semibold text-slate-600">Summary</p>
              <p className="text-sm whitespace-pre-wrap text-slate-900">{report.summary || "—"}</p>
            </div>
            {report.notes && (
              <div className="flex flex-col gap-1">
                <p className="text-xs font-semibold text-slate-600">Notes</p>
                <p className="text-sm whitespace-pre-wrap text-slate-900">{report.notes}</p>
              </div>
            )}
          </Panel>

          <Panel title={`Tasks (${report.tasks.length})`}>
            {report.tasks.length === 0 ? (
              <p className="text-sm text-slate-500">No tasks logged.</p>
            ) : (
              <div className="flex flex-col gap-2.5">
                {report.tasks.map((task) => (
                  <div key={task.id} className="flex flex-col gap-1.5 rounded-lg border border-slate-200 p-3">
                    <div className="flex flex-wrap items-center justify-between gap-2">
                      <p className="text-sm font-semibold text-slate-900">{task.taskName}</p>
                      <div className="flex gap-1.5">
                        <Badge>{titleCase(task.status)}</Badge>
                        <Badge>{titleCase(task.priority)} Priority</Badge>
                      </div>
                    </div>
                    {task.description && <p className="text-[13px] text-slate-600">{task.description}</p>}
                    <div className="flex flex-wrap gap-x-4 gap-y-1 text-[12px] text-slate-500">
                      <span>Planned {task.plannedPercentage}% · Actual {task.actualPercentage}%</span>
                      <span>
                        {Number(task.hoursPlanned ?? 0).toFixed(1)}h planned / {Number(task.hoursSpent ?? 0).toFixed(1)}h spent
                      </span>
                      {task.deliverable && <span>Deliverable: {task.deliverable}</span>}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </Panel>

          <Panel title="Plan for Next Week">
            {report.nextWeekTasks.length === 0 ? (
              <p className="text-sm text-slate-500">Nothing planned.</p>
            ) : (
              <div className="flex flex-col gap-2">
                {report.nextWeekTasks.map((task) => (
                  <div key={task.id} className="flex items-center justify-between gap-2 rounded-lg border border-slate-200 p-2.5">
                    <div>
                      <p className="text-[13px] font-semibold text-slate-900">{task.taskName}</p>
                      {task.description && <p className="text-xs text-slate-500">{task.description}</p>}
                    </div>
                    <Badge>{titleCase(task.priority)}</Badge>
                  </div>
                ))}
              </div>
            )}
          </Panel>

          <div className="flex flex-col gap-6 lg:flex-row">
            <Panel title="Blockers & Issues">
              {report.blockers.length === 0 ? (
                <p className="text-sm text-slate-500">None reported.</p>
              ) : (
                <div className="flex flex-col gap-2">
                  {report.blockers.map((blocker) => (
                    <div key={blocker.id} className="flex flex-col gap-1 rounded-lg border border-slate-200 p-2.5">
                      <div className="flex flex-wrap items-center justify-between gap-2">
                        <p className="text-[13px] font-semibold text-slate-900">
                          {blocker.title} {blocker.keyIssue && <span className="text-amber-600">★</span>}
                        </p>
                        <Badge className={IMPACT_BADGE[blocker.impact]}>{titleCase(blocker.impact)}</Badge>
                      </div>
                      {blocker.description && <p className="text-xs text-slate-500">{blocker.description}</p>}
                      <Badge>{titleCase(blocker.status)}</Badge>
                    </div>
                  ))}
                </div>
              )}
            </Panel>

            <Panel title="Achievements">
              {report.achievements.length === 0 ? (
                <p className="text-sm text-slate-500">None reported.</p>
              ) : (
                <div className="flex flex-col gap-2">
                  {report.achievements.map((achievement) => (
                    <div key={achievement.id} className="flex flex-col gap-1 rounded-lg border border-slate-200 p-2.5">
                      <p className="text-[13px] font-semibold text-slate-900">
                        {achievement.title} {achievement.keyAchievement && <span className="text-amber-600">★</span>}
                      </p>
                      {achievement.description && <p className="text-xs text-slate-500">{achievement.description}</p>}
                    </div>
                  ))}
                </div>
              )}
            </Panel>
          </div>

          <Panel title="Hours by Category">
            {report.workHours.length === 0 ? (
              <p className="text-sm text-slate-500">No hours logged.</p>
            ) : (
              <div className="flex flex-wrap gap-2">
                {report.workHours.map((wh) => (
                  <Badge key={wh.id}>
                    {titleCase(wh.taskType)}: {Number(wh.hours).toFixed(1)}h
                  </Badge>
                ))}
              </div>
            )}
          </Panel>
        </div>

        <div className="flex w-full flex-col gap-6 lg:w-[380px] lg:shrink-0">
          {canReview && (
            <Panel title="Review">
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="comment" className="text-xs font-semibold text-slate-600">
                  Comment{" "}
                  <span className="font-normal text-slate-400">(required to request changes)</span>
                </Label>
                <Textarea
                  id="comment"
                  value={comment}
                  onChange={(e) => setComment(e.target.value)}
                  placeholder="Optional note for approval, or feedback explaining what needs to change…"
                  className="min-h-[90px] rounded-lg bg-slate-50 text-[13px]"
                />
              </div>
              <div className="flex flex-col gap-2">
                <Button
                  type="button"
                  disabled={isActing}
                  onClick={handleApprove}
                  className="h-9 gap-1.5 bg-emerald-600 text-[13px] font-semibold hover:bg-emerald-700"
                >
                  {approveReport.isPending ? (
                    <Loader2 className="size-4 animate-spin" />
                  ) : (
                    <CheckCircle2 className="size-4" />
                  )}
                  Approve
                </Button>
                <Button
                  type="button"
                  variant="outline"
                  disabled={isActing}
                  onClick={handleRequestChanges}
                  className="h-9 gap-1.5 border-red-300 text-[13px] font-semibold text-red-700 hover:bg-red-50"
                >
                  {requestChanges.isPending ? <Loader2 className="size-4 animate-spin" /> : <XCircle className="size-4" />}
                  Request Changes
                </Button>
              </div>
            </Panel>
          )}

          <Panel title="Review History">
            {reviews.length === 0 ? (
              <p className="text-sm text-slate-500">No reviews yet.</p>
            ) : (
              <div className="flex flex-col">
                {reviews.map((review, i) => (
                  <div
                    key={review.id}
                    className={`flex flex-col gap-1 py-3 ${i < reviews.length - 1 ? "border-b border-slate-200" : ""}`}
                  >
                    <div className="flex items-center justify-between gap-2">
                      <Badge
                        className={
                          review.action === "APPROVED"
                            ? "bg-emerald-50 text-emerald-700 border-emerald-200"
                            : "bg-red-50 text-red-700 border-red-200"
                        }
                      >
                        {titleCase(review.action)}
                      </Badge>
                      <p className="text-[11px] text-slate-400">{formatDateTime(review.reviewedAt)}</p>
                    </div>
                    <p className="text-xs font-semibold text-slate-900">{review.reviewerName}</p>
                    {review.comment && <p className="text-xs text-slate-600">{review.comment}</p>}
                  </div>
                ))}
              </div>
            )}
          </Panel>

          {versions.length > 0 && (
            <Panel title="Version History">
              <div className="flex flex-col gap-1.5">
                {versions.map((v) => (
                  <div key={v.versionNumber} className="flex items-center justify-between text-[13px]">
                    <span className="text-slate-600">Version {v.versionNumber}</span>
                    <span className="text-slate-500">{formatDateTime(v.submittedAt)}</span>
                  </div>
                ))}
              </div>
            </Panel>
          )}
        </div>
      </div>
    </div>
  );
}
