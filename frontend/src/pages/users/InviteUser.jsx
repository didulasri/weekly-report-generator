import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useForm } from "react-hook-form";
import { FileText } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { emailRules } from "@/utils/passwordRules";
import { ROLE_LABELS } from "@/utils/constants";

const ROLE_OPTIONS = Object.keys(ROLE_LABELS);

export default function InviteUser() {
  const navigate = useNavigate();
  const [role, setRole] = useState("TEAM_MEMBER");

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm({ defaultValues: { email: "" } });

  // No submit handler wired to a service yet -- this checkpoint is UI only.
  const onSubmit = () => {
    navigate("/admin/users");
  };

  return (
    <div className="flex flex-col gap-6">
      <p className="flex flex-wrap items-center gap-2 text-[13px]">
        <Link to="/admin/users" className="font-medium text-slate-500 hover:text-slate-700">
          Users
        </Link>
        <span className="text-slate-400">/</span>
        <span className="font-semibold text-slate-900">Invite User</span>
      </p>

      <div className="flex justify-center">
        <form
          onSubmit={handleSubmit(onSubmit)}
          noValidate
          className="flex w-full max-w-[580px] flex-col gap-6 rounded-2xl border border-slate-200 bg-white p-8 shadow-[0px_4px_6px_rgba(0,0,0,0.05)]"
        >
          <h1 className="text-lg font-bold text-slate-900">Invite New User</h1>

          <div className="flex flex-col gap-1.5">
            <Label htmlFor="email" className="text-[13px] font-semibold text-slate-600">
              Work Email
            </Label>
            <Input
              id="email"
              type="email"
              placeholder="Enter email address (e.g., user@company.com)"
              aria-invalid={errors.email ? "true" : "false"}
              className="h-10 rounded-md text-[13px]"
              {...register("email", emailRules())}
            />
            {errors.email ? (
              <p className="text-xs text-destructive" role="alert">
                {errors.email.message}
              </p>
            ) : (
              <p className="text-xs text-slate-400">
                The secure link will be dispatched to this corporate address.
              </p>
            )}
          </div>

          <div className="flex flex-col gap-1.5">
            <Label htmlFor="role" className="text-[13px] font-semibold text-slate-600">
              Role
            </Label>
            <Select value={role} onValueChange={setRole}>
              <SelectTrigger id="role" className="h-10 w-full justify-between rounded-md text-[13px]">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {ROLE_OPTIONS.map((option) => (
                  <SelectItem key={option} value={option}>
                    {ROLE_LABELS[option]}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="flex gap-3 rounded-lg border border-blue-200 bg-blue-50 p-4">
            <FileText className="size-[18px] shrink-0 text-blue-700" />
            <p className="flex-1 text-[13px] leading-5 text-blue-700">
              The user will receive a secure invitation link to create their account. The
              invitation expires after 48 hours.
            </p>
          </div>

          <div className="flex justify-end gap-3">
            <Button type="button" variant="outline" className="h-9 px-4 text-[13px] font-semibold" asChild>
              <Link to="/admin/users">Cancel</Link>
            </Button>
            <Button type="submit" className="h-9 px-4 text-[13px] font-semibold">
              Send Invitation
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}
