import { Link } from "react-router-dom";
import { useForm } from "react-hook-form";
import { Loader2, MailCheck } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card } from "@/components/ui/card";
import { emailRules } from "@/utils/passwordRules";
import { useForgotPassword } from "@/hooks/useAuthFlows";
import { getApiErrorMessage } from "@/utils/apiError";

export default function ForgotPassword() {
  const {
    register,
    handleSubmit,
    getValues,
    formState: { errors },
  } = useForm({ defaultValues: { email: "" } });

  const forgotPasswordMutation = useForgotPassword();

  const onSubmit = (values) => {
    forgotPasswordMutation.mutate(values.email);
  };

  return (
    <div className="flex min-h-screen w-full flex-col items-center justify-center gap-6 bg-[#f8f8fa] p-4 sm:p-8">
      <Card className="w-full max-w-[420px] gap-7 rounded-2xl border border-[#e2e8f0] p-6 shadow-[0px_4px_6px_rgba(0,0,0,0.05)] sm:p-8">
        {forgotPasswordMutation.isSuccess ? (
          <div className="flex flex-col items-center gap-5 text-center">
            <div className="flex size-14 shrink-0 items-center justify-center rounded-full bg-emerald-50">
              <MailCheck className="size-7 text-emerald-500" />
            </div>
            <div className="flex flex-col items-center gap-2">
              <h1 className="text-2xl font-bold text-[#0f172a]">Check Your Email</h1>
              <p className="text-sm text-[#475569]">
                If an account exists for <span className="font-semibold">{getValues("email")}</span>,
                we&apos;ve sent a password reset link to it.
              </p>
            </div>
            <Link
              to="/login"
              className="text-center text-sm font-semibold text-[#1a0b2e] outline-none hover:underline focus-visible:ring-3 focus-visible:ring-ring/50 rounded-sm"
            >
              Back to Login
            </Link>
          </div>
        ) : (
          <>
            <div className="flex flex-col items-center gap-4">
              <div className="flex size-12 shrink-0 items-center justify-center rounded-xl bg-[#1a0b2e]">
                <p className="font-mono text-lg font-bold text-[#ffb7a5]">WRG</p>
              </div>
              <div className="flex flex-col items-center gap-2 text-center">
                <h1 className="text-2xl font-bold text-[#0f172a]">Forgot Password</h1>
                <p className="text-sm text-[#475569]">
                  Enter your work email and we&apos;ll send you a reset link.
                </p>
              </div>
            </div>

            <form onSubmit={handleSubmit(onSubmit)} noValidate className="flex flex-col gap-5">
              <div className="flex flex-col gap-2">
                <Label htmlFor="email" className="text-[13px] font-semibold text-[#475569]">
                  Work Email
                </Label>
                <Input
                  id="email"
                  type="email"
                  autoComplete="email"
                  placeholder="m.vance@company.com"
                  aria-invalid={errors.email ? "true" : "false"}
                  className="h-11 rounded-lg border-[#e2e8f0] bg-[#f8fafc] px-3.5 py-3 text-base text-[#0f172a] md:text-sm"
                  {...register("email", emailRules())}
                />
                {errors.email && (
                  <p className="text-xs text-destructive" role="alert">
                    {errors.email.message}
                  </p>
                )}
              </div>

              {forgotPasswordMutation.isError && (
                <p className="text-xs text-destructive" role="alert">
                  {getApiErrorMessage(forgotPasswordMutation.error)}
                </p>
              )}

              <Button
                type="submit"
                disabled={forgotPasswordMutation.isPending}
                className="h-11 w-full rounded-lg text-sm font-semibold"
              >
                {forgotPasswordMutation.isPending && <Loader2 className="size-4 animate-spin" />}
                Send Reset Link
              </Button>
            </form>

            <Link
              to="/login"
              className="text-center text-sm font-semibold text-[#1a0b2e] outline-none hover:underline focus-visible:ring-3 focus-visible:ring-ring/50 rounded-sm"
            >
              Back to Login
            </Link>
          </>
        )}
      </Card>

      <p className="max-w-[420px] text-center text-xs text-[#94a3b8]">
        This is a secure internal company application
      </p>
    </div>
  );
}
