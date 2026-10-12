import { useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { AlertCircle, Loader2, Trash2 } from "lucide-react";
import { toast } from "sonner";
import StatusBadge from "@/components/common/StatusBadge";
import EmptyState from "@/components/feedback/EmptyState";
import { Button } from "@/components/ui/button";
import ReportForm from "./ReportForm";
import { useDeleteReport, useReport, useSubmitReport, useUpdateReport } from "@/hooks/useReports";
import { getApiErrorList, getApiErrorMessage } from "@/utils/apiError";

export default function EditWeeklyReport() {
  const { reportId } = useParams();
  const navigate = useNavigate();
  const { data: report, isPending, isError, error } = useReport(reportId);

  const updateReport = useUpdateReport();
  const submitReport = useSubmitReport();
  const deleteReport = useDeleteReport();
  const [submitErrors, setSubmitErrors] = useState([]);

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
        description={
          notFound ? "This weekly report doesn't exist or may have been removed." : getApiErrorMessage(error)
        }
        action={
          <Button asChild className="h-9 px-3.5 text-[13px] font-semibold">
            <Link to="/reports">Back to My Reports</Link>
          </Button>
        }
      />
    );
  }

  const isSaving = updateReport.isPending && !submitReport.isPending;
  const isSubmitting = updateReport.isPending || submitReport.isPending;

  async function handleSaveDraft(payload) {
    setSubmitErrors([]);
    try {
      await updateReport.mutateAsync({ id: report.id, payload });
      toast.success("Changes saved");
      navigate("/reports");
    } catch (err) {
      toast.error(getApiErrorMessage(err));
    }
  }

  async function handleSubmitForReview(payload) {
    setSubmitErrors([]);
    try {
      await updateReport.mutateAsync({ id: report.id, payload });
      await submitReport.mutateAsync(report.id);
      toast.success("Report submitted for review");
      navigate("/reports");
    } catch (err) {
      const errors = getApiErrorList(err);
      setSubmitErrors(errors);
      toast.error(getApiErrorMessage(err));
    }
  }

  function handleDelete() {
    if (!window.confirm("Delete this draft? This can't be undone.")) return;
    deleteReport.mutate(report.id, {
      onSuccess: () => {
        toast.success("Draft deleted");
        navigate("/reports");
      },
      onError: (err) => toast.error(getApiErrorMessage(err)),
    });
  }

  const weekLabel = `${report.weekStartDate} to ${report.weekEndDate}`;

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <p className="flex flex-wrap items-center gap-2 text-[13px]">
          <Link to="/reports" className="font-medium text-slate-500 hover:text-slate-700">
            My Reports
          </Link>
          <span className="text-slate-400">/</span>
          <span className="font-semibold text-slate-900">Edit Report ({weekLabel})</span>
        </p>
        <div className="flex items-center gap-2">
          <StatusBadge status={report.status} />
          {report.status === "DRAFT" && (
            <Button
              type="button"
              variant="outline"
              size="sm"
              disabled={deleteReport.isPending}
              onClick={handleDelete}
              className="h-7 gap-1.5 border-red-300 px-2.5 text-[11px] font-semibold text-red-700 hover:bg-red-50"
            >
              <Trash2 className="size-3" />
              Delete Draft
            </Button>
          )}
        </div>
      </div>

      {!report.canEdit ? (
        <EmptyState
          title="This report can't be edited"
          description={`Reports can only be edited while in Draft or Needs Correction status (currently ${report.status}).`}
          action={
            <Button asChild className="h-9 px-3.5 text-[13px] font-semibold">
              <Link to="/reports">Back to My Reports</Link>
            </Button>
          }
        />
      ) : (
        <>
          {submitErrors.length > 0 && (
            <div className="flex gap-3 rounded-lg border border-red-200 bg-red-50 p-4">
              <AlertCircle className="size-[18px] shrink-0 text-red-600" />
              <ul className="flex-1 list-disc pl-4 text-[13px] text-red-700">
                {submitErrors.map((message) => (
                  <li key={message}>{message}</li>
                ))}
              </ul>
            </div>
          )}

          <ReportForm
            mode="edit"
            report={report}
            onSaveDraft={handleSaveDraft}
            onSubmitForReview={handleSubmitForReview}
            isSaving={isSaving}
            isSubmitting={isSubmitting}
          />
        </>
      )}
    </div>
  );
}
