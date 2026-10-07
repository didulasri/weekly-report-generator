import { Link } from "react-router-dom";
import { CheckCircle2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { ROLE_LABELS } from "@/utils/constants";

// No live invitation context yet -- this checkpoint is UI only, so the role shown here mirrors
// AcceptInvitation.jsx's stand-in invite (Team Member).
const SIGNED_UP_ROLE = "TEAM_MEMBER";

export default function AccountCreatedSuccess() {
  return (
    <div className="flex min-h-screen w-full flex-col items-center justify-center gap-6 bg-[#f8f8fa] p-4 sm:p-8">
      <Card className="w-full max-w-[420px] gap-7 rounded-2xl border border-[#e2e8f0] p-6 shadow-[0px_4px_6px_rgba(0,0,0,0.05)] sm:p-8">
        <div className="flex flex-col items-center gap-5">
          <div className="flex size-14 shrink-0 items-center justify-center rounded-full bg-emerald-50">
            <CheckCircle2 className="size-7 text-emerald-500" />
          </div>
          <div className="flex flex-col items-center gap-3 text-center">
            <h1 className="text-[22px] font-bold text-[#0f172a]">Account Created Successfully</h1>
            <span className="rounded-full bg-[#fff0ed] px-3 py-1 text-xs font-semibold text-[#1a0b2e]">
              Signed up as {ROLE_LABELS[SIGNED_UP_ROLE]}
            </span>
            <p className="text-sm leading-[22px] text-[#475569]">
              Your account has been set up. You can now sign in to access Weekly Report
              Generator.
            </p>
          </div>
        </div>

        <Button asChild className="h-11 w-full rounded-lg text-sm font-semibold">
          <Link to="/login">Continue to Login</Link>
        </Button>
      </Card>

      <p className="max-w-[420px] text-center text-xs text-[#94a3b8]">
        This is a secure internal company application
      </p>
    </div>
  );
}
