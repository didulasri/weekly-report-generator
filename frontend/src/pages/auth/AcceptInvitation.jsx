import { useNavigate } from "react-router-dom";
import { useForm } from "react-hook-form";
import { Info, User } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card } from "@/components/ui/card";
import PasswordInput from "@/components/common/PasswordInput.jsx";
import { passwordStrengthRules } from "@/utils/passwordRules";
import { ROLE_LABELS } from "@/utils/constants";

// No live invitation lookup yet -- this checkpoint is UI only, so the invite details are a
// representative stand-in for what /invitations/validate would return.
const INVITE = {
  name: "Sarah Chen",
  email: "sarah.chen@company.com",
  role: "TEAM_MEMBER",
};

export default function AcceptInvitation() {
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
    navigate("/account-created");
  };

  return (
    <div className="flex min-h-screen w-full flex-col items-center justify-center gap-6 bg-[#f8f8fa] p-4 sm:p-8">
      <Card className="w-full max-w-[480px] gap-7 rounded-2xl border border-[#e2e8f0] p-6 shadow-[0px_4px_6px_rgba(0,0,0,0.05)] sm:p-8">
        <div className="flex flex-col items-center gap-4">
          <div className="flex size-12 shrink-0 items-center justify-center rounded-xl bg-[#1a0b2e]">
            <p className="font-mono text-lg font-bold text-[#ffb7a5]">WRG</p>
          </div>
          <div className="flex flex-col items-center gap-2 text-center">
            <h1 className="text-2xl font-bold text-[#0f172a]">Complete Your Account</h1>
            <p className="text-sm text-[#475569]">
              You&apos;ve been invited to join Weekly Report Generator.
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2.5 rounded-lg border border-[#ffb7a5] bg-[#fff0ed] p-3">
          <User className="size-[18px] shrink-0 text-[#1a0b2e]" />
          <p className="text-[13px] font-semibold text-[#1a0b2e]">
            You&apos;ve been invited as a {ROLE_LABELS[INVITE.role]}
          </p>
        </div>

        <form onSubmit={handleSubmit(onSubmit)} noValidate className="flex flex-col gap-4">
          <div className="flex flex-col gap-2">
            <Label htmlFor="name" className="text-[13px] font-semibold text-[#475569]">
              Full Name
            </Label>
            <Input
              id="name"
              defaultValue={INVITE.name}
              aria-invalid={errors.name ? "true" : "false"}
              className="h-11 rounded-lg border-[#e2e8f0] bg-[#f8fafc] px-3.5 py-3 text-base text-[#0f172a] md:text-sm"
              {...register("name", { required: "Full name is required" })}
            />
            {errors.name && (
              <p className="text-xs text-destructive" role="alert">
                {errors.name.message}
              </p>
            )}
          </div>

          <div className="flex flex-col gap-2">
            <Label htmlFor="email" className="text-[13px] font-semibold text-[#475569]">
              Work Email (Read Only)
            </Label>
            <Input
              id="email"
              value={INVITE.email}
              readOnly
              disabled
              className="h-11 rounded-lg border-[#e2e8f0] bg-[#f1f5f9] px-3.5 py-3 text-base text-[#475569] md:text-sm"
            />
          </div>

          <div className="flex flex-col gap-2">
            <Label htmlFor="role" className="text-[13px] font-semibold text-[#475569]">
              Role (Read Only)
            </Label>
            <Input
              id="role"
              value={ROLE_LABELS[INVITE.role]}
              readOnly
              disabled
              className="h-11 rounded-lg border-[#e2e8f0] bg-[#f1f5f9] px-3.5 py-3 text-base text-[#475569] md:text-sm"
            />
          </div>

          <div className="flex flex-col gap-2">
            <Label htmlFor="password" className="text-[13px] font-semibold text-[#475569]">
              Password
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

          <div className="flex gap-2 pt-1">
            <Info className="size-4 shrink-0 text-[#475569]" />
            <p className="flex-1 text-xs leading-[18px] text-[#475569]">
              Your role has been assigned by your administrator and cannot be changed.
            </p>
          </div>

          <Button type="submit" className="h-11 w-full rounded-lg text-sm font-semibold">
            Complete Account Setup
          </Button>
        </form>
      </Card>

      <p className="max-w-[480px] text-center text-xs text-[#94a3b8]">
        This is a secure internal company application
      </p>
    </div>
  );
}
