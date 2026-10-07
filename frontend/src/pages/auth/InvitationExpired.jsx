import { AlertTriangle } from "lucide-react";
import { Card } from "@/components/ui/card";

export default function InvitationExpired() {
  return (
    <div className="flex min-h-screen w-full flex-col items-center justify-center gap-6 bg-[#f8f8fa] p-4 sm:p-8">
      <Card className="w-full max-w-[420px] gap-7 rounded-2xl border border-[#e2e8f0] p-6 shadow-[0px_4px_6px_rgba(0,0,0,0.05)] sm:p-8">
        <div className="flex flex-col items-center gap-5">
          <div className="flex size-14 shrink-0 items-center justify-center rounded-full bg-amber-100">
            <AlertTriangle className="size-7 text-amber-500" />
          </div>
          <div className="flex flex-col items-center gap-3 text-center">
            <h1 className="text-[22px] font-bold text-[#0f172a]">Invitation Expired</h1>
            <p className="text-sm leading-[22px] text-[#475569]">
              This invitation link has expired. Invitation links are valid for 48 hours after
              being sent.
            </p>
          </div>
        </div>

        <div className="flex flex-col gap-4">
          <p className="text-center text-[13px] leading-5 text-[#475569]">
            Please contact your administrator to request a new invitation.
          </p>
          <a
            href="mailto:admin@company.com"
            className="flex w-full items-center justify-center rounded-lg border border-[#1a0b2e] py-3 text-sm font-semibold text-[#1a0b2e] outline-none hover:bg-slate-50 focus-visible:ring-3 focus-visible:ring-ring/50"
          >
            Contact Administrator
          </a>
        </div>
      </Card>

      <p className="max-w-[420px] text-center text-xs text-[#94a3b8]">
        This is a secure internal company application
      </p>
    </div>
  );
}
