import { Link } from "react-router-dom";
import { useForm } from "react-hook-form";
import { Check, Loader2 } from "lucide-react";
import { toast } from "sonner";
import { useLogin } from "@/hooks/useAuth";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card } from "@/components/ui/card";
import PasswordInput from "@/components/common/PasswordInput.jsx";
import { emailRules } from "@/utils/passwordRules";
import { getApiErrorMessage, getApiFieldError } from "@/utils/apiError";

const BENEFITS = [
  "Track project progress and completed work",
  "Manage team reports and approvals",
  "Monitor hours and team productivity",
];

export default function Login() {
  const loginMutation = useLogin();

  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm({ defaultValues: { email: "", password: "" } });

  const onSubmit = (values) => {
    loginMutation.mutate(values, {
      onError: (error) => {
        // Login only ever returns a single generic message (backend deliberately doesn't say
        // "wrong password" vs "no such account"), so this almost always falls through to the
        // toast -- the field-error attempt is here for the rare case the backend does return a
        // field-scoped validation error (e.g. malformed email caught server-side).
        const fieldError = getApiFieldError(error);
        if (fieldError && (fieldError.field === "email" || fieldError.field === "password")) {
          setError(fieldError.field, { type: "server", message: fieldError.message });
        } else {
          toast.error(getApiErrorMessage(error));
        }
      },
    });
  };

  return (
    <div className="flex min-h-screen w-full flex-col bg-[#f8f8fa] lg:flex-row">
      {/* Branding panel -- hidden below lg: it's marketing content, not something a 320px
          viewport has room for alongside an actually-usable form. */}
      <div className="hidden shrink-0 flex-col justify-between bg-[#1a0b2e] p-8 lg:flex lg:w-[648px] lg:p-18">
        <div className="flex flex-col gap-10">
          <div className="flex flex-col gap-3">
            <h1 className="text-[40px] leading-[48px] font-bold text-white">
              Weekly Report Generator
            </h1>
            <p className="text-base text-white/80">
              Streamline your team&apos;s weekly reporting workflow
            </p>
          </div>
          <ul className="flex flex-col gap-4">
            {BENEFITS.map((benefit) => (
              <li key={benefit} className="flex items-center gap-3">
                <span className="flex size-7 shrink-0 items-center justify-center rounded-xl bg-white/8">
                  <Check className="size-3.5 text-white" />
                </span>
                <span className="flex-1 text-sm text-white">{benefit}</span>
              </li>
            ))}
          </ul>
        </div>
        <p className="text-xs font-medium text-white/60">© 2026 Company Name</p>
      </div>

      {/* Form panel -- full width with side padding on mobile, centered card on larger screens. */}
      <div className="flex flex-1 items-center justify-center p-4 sm:p-8 lg:p-18">
        <Card className="w-full max-w-[450px] gap-6 rounded-[16px] border border-[#e2e8f0] p-6 shadow-[0px_2px_2px_rgba(0,0,0,0.03),0px_4px_6px_rgba(0,0,0,0.05)] sm:p-8">
          <div className="flex flex-col gap-2">
            <h2 className="text-2xl font-bold text-[#0f172a]">Sign in to your account</h2>
            <p className="text-sm text-[#475569]">Enter your work credentials to continue</p>
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

            <div className="flex flex-col gap-2">
              <Label htmlFor="password" className="text-[13px] font-semibold text-[#475569]">
                Password
              </Label>
              <PasswordInput
                id="password"
                autoComplete="current-password"
                placeholder="••••••••••••"
                aria-invalid={errors.password ? "true" : "false"}
                className="h-11 rounded-lg border-[#e2e8f0] bg-[#f8fafc] px-3.5 py-3 text-base text-[#0f172a] md:text-sm"
                {...register("password", { required: "Password is required" })}
              />
              {errors.password && (
                <p className="text-xs text-destructive" role="alert">
                  {errors.password.message}
                </p>
              )}
            </div>

            <div className="flex items-center justify-end">
              <Link
                to="/forgot-password"
                className="text-[13px] font-medium text-[#4f46e5] outline-none hover:underline focus-visible:ring-3 focus-visible:ring-ring/50 rounded-sm"
              >
                Forgot password?
              </Link>
            </div>

            <Button
              type="submit"
              disabled={loginMutation.isPending}
              className="h-11 w-full rounded-lg text-sm font-semibold"
            >
              {loginMutation.isPending ? (
                <>
                  <Loader2 className="size-4 animate-spin" />
                  Signing in…
                </>
              ) : (
                "Sign In"
              )}
            </Button>
          </form>

          <p className="text-center text-xs text-[#94a3b8]">
            This is a secure internal company application
          </p>
        </Card>
      </div>
    </div>
  );
}
