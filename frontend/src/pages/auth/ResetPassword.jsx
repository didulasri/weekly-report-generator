import { Link, useNavigate } from "react-router-dom";
import { useForm } from "react-hook-form";
import { Check } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Card } from "@/components/ui/card";
import PasswordInput from "@/components/common/PasswordInput.jsx";
import { passwordStrengthRules } from "@/utils/passwordRules";

const REQUIREMENTS = [
  { label: "At least 8 characters", test: (value) => value.length >= 8 },
  { label: "At least one uppercase letter", test: (value) => /[A-Z]/.test(value) },
  { label: "At least one lowercase letter", test: (value) => /[a-z]/.test(value) },
  { label: "At least one numeric digit", test: (value) => /\d/.test(value) },
  { label: "At least one special character (!@#$%)", test: (value) => /[!@#$%^&*]/.test(value) },
];

export default function ResetPassword() {
  const navigate = useNavigate();

  const {
    register,
    handleSubmit,
    watch,
    formState: { errors },
  } = useForm({ defaultValues: { password: "", confirmPassword: "" } });

  const password = watch("password");

  // No submit handler wired to a service yet -- this checkpoint is UI only.
  const onSubmit = () => {
    navigate("/login");
  };

  return (
    <div className="flex min-h-screen w-full flex-col items-center justify-center gap-6 bg-[#f8f8fa] p-4 sm:p-8">
      <Card className="w-full max-w-[420px] gap-7 rounded-2xl border border-[#e2e8f0] p-6 shadow-[0px_4px_6px_rgba(0,0,0,0.05)] sm:p-8">
        <div className="flex flex-col items-center gap-4">
          <div className="flex size-12 shrink-0 items-center justify-center rounded-xl bg-[#1a0b2e]">
            <p className="font-mono text-lg font-bold text-[#ffb7a5]">WRG</p>
          </div>
          <div className="flex flex-col items-center gap-2 text-center">
            <h1 className="text-2xl font-bold text-[#0f172a]">Reset Your Password</h1>
            <p className="text-sm text-[#475569]">Create a new secure password for your account.</p>
          </div>
        </div>

        <form onSubmit={handleSubmit(onSubmit)} noValidate className="flex flex-col gap-5">
          <div className="flex flex-col gap-2">
            <Label htmlFor="password" className="text-[13px] font-semibold text-[#475569]">
              New Password
            </Label>
            <PasswordInput
              id="password"
              autoComplete="new-password"
              placeholder="••••••••••••"
              aria-invalid={errors.password ? "true" : "false"}
              className="h-11 rounded-lg border-[#e2e8f0] bg-[#f8fafc] px-3.5 py-3 text-base text-[#0f172a] md:text-sm"
              {...register("password", passwordStrengthRules())}
            />
            {errors.password && (
              <p className="text-xs text-destructive" role="alert">
                {errors.password.message}
              </p>
            )}
          </div>

          <div className="flex flex-col gap-2">
            <Label htmlFor="confirmPassword" className="text-[13px] font-semibold text-[#475569]">
              Confirm Password
            </Label>
            <PasswordInput
              id="confirmPassword"
              autoComplete="new-password"
              placeholder="••••••••••••"
              aria-invalid={errors.confirmPassword ? "true" : "false"}
              className="h-11 rounded-lg border-[#e2e8f0] bg-[#f8fafc] px-3.5 py-3 text-base text-[#0f172a] md:text-sm"
              {...register("confirmPassword", {
                required: "Please confirm your password",
                validate: (value) => value === password || "Passwords do not match",
              })}
            />
            {errors.confirmPassword && (
              <p className="text-xs text-destructive" role="alert">
                {errors.confirmPassword.message}
              </p>
            )}
          </div>

          <div className="flex flex-col gap-2.5 rounded-lg border border-[#e2e8f0] bg-[#f8fafc] p-4">
            <p className="text-xs font-semibold text-[#475569]">Password Requirements</p>
            {REQUIREMENTS.map((requirement) => {
              const met = requirement.test(password ?? "");
              return (
                <div key={requirement.label} className="flex items-center gap-2">
                  <span
                    className={`flex size-4 shrink-0 items-center justify-center rounded-md ${
                      met ? "bg-emerald-50" : "bg-slate-100"
                    }`}
                  >
                    <Check className={`size-2.5 ${met ? "text-emerald-600" : "text-slate-300"}`} />
                  </span>
                  <p className={`text-xs ${met ? "text-[#0f172a]" : "text-slate-400"}`}>
                    {requirement.label}
                  </p>
                </div>
              );
            })}
          </div>

          <Button type="submit" className="h-11 w-full rounded-lg text-sm font-semibold">
            Reset Password
          </Button>
        </form>

        <Link
          to="/login"
          className="text-center text-sm font-semibold text-[#1a0b2e] outline-none hover:underline focus-visible:ring-3 focus-visible:ring-ring/50 rounded-sm"
        >
          Back to Login
        </Link>
      </Card>

      <p className="max-w-[420px] text-center text-xs text-[#94a3b8]">
        This is a secure internal company application
      </p>
    </div>
  );
}
