import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { AlertCircle } from "lucide-react";
import { toast } from "sonner";
import PageHeader from "@/components/layout/PageHeader";
import ReportForm from "./ReportForm";
import { useCreateReport, useSubmitReport } from "@/hooks/useReports";
import { getApiErrorList, getApiErrorMessage } from "@/utils/apiError";

export default function CreateWeeklyReport() {
  const navigate = useNavigate();
  const createReport = useCreateReport();
  const submitReport = useSubmitReport();
  const [submitErrors, setSubmitErrors] = useState([]);

  const isSaving = createReport.isPending && !submitReport.isPending;
  const isSubmitting = createReport.isPending || submitReport.isPending;

  async function handleSaveDraft(payload) {
    setSubmitErrors([]);
    try {
      await createReport.mutateAsync(payload);
      toast.success("Draft saved");
      navigate("/reports");
    } catch (err) {
      toast.error(getApiErrorMessage(err));
    }
  }

  async function handleSubmitForReview(payload) {
    setSubmitErrors([]);
    try {
      const created = await createReport.mutateAsync(payload);
      await submitReport.mutateAsync(created.id);
      toast.success("Report submitted for review");
      navigate("/reports");
    } catch (err) {
      const errors = getApiErrorList(err);
      setSubmitErrors(errors);
      toast.error(getApiErrorMessage(err));
    }
  }

  return (
    <div className="flex flex-col gap-6">
      <PageHeader title="New Weekly Report" description="Log your work for this reporting week" />

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
        mode="create"
        report={null}
        onSaveDraft={handleSaveDraft}
        onSubmitForReview={handleSubmitForReview}
        isSaving={isSaving}
        isSubmitting={isSubmitting}
      />
    </div>
  );
}
