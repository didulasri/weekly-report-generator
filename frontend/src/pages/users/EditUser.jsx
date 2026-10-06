import { useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { useForm } from "react-hook-form";
import { AlertTriangle } from "lucide-react";
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
import EmptyState from "@/components/feedback/EmptyState";
import { ROLE_LABELS } from "@/utils/constants";
import { getUserById } from "./mockUsers";

const ROLE_OPTIONS = Object.keys(ROLE_LABELS);

const STATUS_LABELS = {
  ACTIVE: "Active",
  PENDING: "Pending",
  SUSPENDED: "Suspended",
};

const STATUS_OPTIONS = Object.keys(STATUS_LABELS);

export default function EditUser() {
  const { userId } = useParams();
  const navigate = useNavigate();
  const user = getUserById(userId);

  const [role, setRole] = useState(user?.role ?? "TEAM_MEMBER");
  const [status, setStatus] = useState(user?.status ?? "ACTIVE");

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm({ defaultValues: { name: user?.name ?? "" } });

  // No submit handler wired to a service yet -- this checkpoint is UI only.
  const onSubmit = () => {
    navigate(`/admin/users/${userId}`);
  };

  if (!user) {
    return (
      <EmptyState
        title="User not found"
        description="This user doesn't exist or may have been removed."
        action={
          <Button asChild className="h-9 px-3.5 text-[13px] font-semibold">
            <Link to="/admin/users">Back to Users</Link>
          </Button>
        }
      />
    );
  }

  return (
    <div className="flex flex-col gap-6">
      <p className="flex flex-wrap items-center gap-2 text-[13px]">
        <Link to="/admin/users" className="font-medium text-slate-500 hover:text-slate-700">
          Users
        </Link>
        <span className="text-slate-400">/</span>
        <Link to={`/admin/users/${userId}`} className="font-medium text-slate-500 hover:text-slate-700">
          {user.name}
        </Link>
        <span className="text-slate-400">/</span>
        <span className="font-semibold text-slate-900">Edit</span>
      </p>

      <div className="flex justify-center">
        <form
          onSubmit={handleSubmit(onSubmit)}
          noValidate
          className="flex w-full max-w-[580px] flex-col gap-6 rounded-2xl border border-slate-200 bg-white p-8 shadow-[0px_4px_6px_rgba(0,0,0,0.05)]"
        >
          <h1 className="text-lg font-bold text-slate-900">Edit User</h1>

          <div className="flex flex-col gap-1.5">
            <Label htmlFor="name" className="text-[13px] font-semibold text-slate-600">
              Full Name
            </Label>
            <Input
              id="name"
              aria-invalid={errors.name ? "true" : "false"}
              className="h-10 rounded-md text-[13px]"
              {...register("name", { required: "Full name is required" })}
            />
            {errors.name && (
              <p className="text-xs text-destructive" role="alert">
                {errors.name.message}
              </p>
            )}
          </div>

          <div className="flex flex-col gap-1.5">
            <Label htmlFor="email" className="text-[13px] font-semibold text-slate-600">
              Email Address (Read-only)
            </Label>
            <Input
              id="email"
              value={user.email}
              readOnly
              disabled
              className="h-10 rounded-md bg-slate-100 text-[13px] text-slate-600"
            />
          </div>

          <div className="flex flex-col gap-1.5">
            <Label htmlFor="role" className="text-[13px] font-semibold text-slate-600">
              System Role
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

          <div className="flex flex-col gap-1.5">
            <Label htmlFor="status" className="text-[13px] font-semibold text-slate-600">
              Account Status
            </Label>
            <Select value={status} onValueChange={setStatus}>
              <SelectTrigger id="status" className="h-10 w-full justify-between rounded-md text-[13px]">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {STATUS_OPTIONS.map((option) => (
                  <SelectItem key={option} value={option}>
                    {STATUS_LABELS[option]}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="flex gap-3 rounded-lg border border-amber-200 bg-amber-50 p-4">
            <AlertTriangle className="size-[18px] shrink-0 text-amber-700" />
            <p className="flex-1 text-[13px] leading-5 text-amber-700">
              Changing a user&apos;s role will immediately affect their access permissions and
              navigation menu.
            </p>
          </div>

          <div className="flex justify-end gap-3">
            <Button type="button" variant="outline" className="h-9 px-4 text-[13px] font-semibold" asChild>
              <Link to={`/admin/users/${userId}`}>Cancel</Link>
            </Button>
            <Button type="submit" className="h-9 px-4 text-[13px] font-semibold">
              Save Changes
            </Button>
          </div>

          <div className="h-px w-full bg-slate-200" />

          <div className="flex flex-col gap-3 rounded-lg border border-red-300 bg-red-50 p-4">
            <p className="text-[13px] font-bold text-red-700">Danger Zone</p>
            <div className="flex items-center justify-between gap-3">
              <div className="flex flex-col gap-0.5">
                <p className="text-xs font-semibold text-slate-900">Suspend Account</p>
                <p className="text-[11px] text-slate-600">
                  Suspending will prevent the user from logging in.
                </p>
              </div>
              <Button
                type="button"
                variant="outline"
                className="h-7 border-red-300 px-3 text-xs font-semibold text-red-700 hover:bg-red-100"
              >
                Suspend User
              </Button>
            </div>
          </div>
        </form>
      </div>
    </div>
  );
}
