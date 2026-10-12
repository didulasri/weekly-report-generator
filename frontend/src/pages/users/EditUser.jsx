import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { useForm } from "react-hook-form";
import { AlertTriangle, Loader2 } from "lucide-react";
import { toast } from "sonner";
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
import { useCurrentUser } from "@/hooks/useAuth";
import {
  useActivateUser,
  useChangeUserRole,
  useDeactivateUser,
  useUpdateUser,
  useUser,
} from "@/hooks/useUsers";
import { ROLE_LABELS } from "@/utils/constants";
import { getApiErrorMessage } from "@/utils/apiError";

// Admins only move a user between these two roles here -- promoting/demoting to ADMIN isn't
// exposed in this form (matches InviteUser.jsx's own role options for the same reason: a second
// ADMIN is provisioned via AdminBootstrapRunner on the backend, not through user-management UI).
const ROLE_OPTIONS = ["TEAM_MEMBER", "MANAGER"];

const STATUS_LABELS = {
  ACTIVE: "Active",
  SUSPENDED: "Suspended",
};

const STATUS_OPTIONS = Object.keys(STATUS_LABELS);

export default function EditUser() {
  const { userId } = useParams();
  const navigate = useNavigate();
  const { data: user, isPending, isError, error } = useUser(userId);
  const { data: currentUser } = useCurrentUser();

  const updateUser = useUpdateUser();
  const changeRole = useChangeUserRole();
  const deactivateUser = useDeactivateUser();
  const activateUser = useActivateUser();

  const [role, setRole] = useState("TEAM_MEMBER");
  const [status, setStatus] = useState("ACTIVE");
  const [loadedUserId, setLoadedUserId] = useState(null);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm({ defaultValues: { name: "" } });

  // react-hook-form's state lives outside React, so syncing it to freshly-loaded data belongs in
  // an effect (that's what effects are for: synchronizing with an external system).
  useEffect(() => {
    if (user) {
      reset({ name: user.name });
    }
  }, [user, reset]);

  // role/status are plain React state, so they're adjusted directly during render the moment a
  // new user's data arrives (guarded by loadedUserId so it only fires once per user, not on every
  // render) -- the pattern React itself recommends for "reset state when a prop changes", rather
  // than routing a React state update through an effect.
  if (user && user.id !== loadedUserId) {
    setLoadedUserId(user.id);
    setRole(user.role === "ADMIN" ? "TEAM_MEMBER" : user.role);
    setStatus(user.active ? "ACTIVE" : "SUSPENDED");
  }

  const isSelf = Boolean(currentUser && user && currentUser.id === user.id);
  const isSaving = updateUser.isPending || changeRole.isPending || deactivateUser.isPending || activateUser.isPending;

  const onSubmit = async ({ name }) => {
    try {
      await updateUser.mutateAsync({ id: user.id, name, email: user.email });

      if (!isSelf && role !== user.role) {
        await changeRole.mutateAsync({ id: user.id, role });
      }

      const wantsActive = status === "ACTIVE";
      if (!isSelf && wantsActive !== user.active) {
        await (wantsActive ? activateUser : deactivateUser).mutateAsync(user.id);
      }

      toast.success("User updated");
      navigate(`/admin/users/${user.id}`);
    } catch (err) {
      toast.error(getApiErrorMessage(err));
    }
  };

  function handleSuspendClick() {
    const mutation = user.active ? deactivateUser : activateUser;
    mutation.mutate(user.id, {
      onSuccess: () => {
        toast.success(user.active ? "User suspended" : "User reactivated");
        setStatus(user.active ? "SUSPENDED" : "ACTIVE");
      },
      onError: (err) => toast.error(getApiErrorMessage(err)),
    });
  }

  if (isPending) {
    return (
      <div className="flex items-center justify-center gap-2 py-16 text-sm text-slate-500">
        <Loader2 className="size-4 animate-spin" />
        Loading user…
      </div>
    );
  }

  if (isError) {
    const notFound = error?.response?.status === 404;
    return (
      <EmptyState
        title={notFound ? "User not found" : "Couldn't load this user"}
        description={notFound ? "This user doesn't exist or may have been removed." : getApiErrorMessage(error)}
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
            {user.role === "ADMIN" ? (
              <p className="flex h-10 items-center rounded-md border border-slate-200 bg-slate-100 px-3 text-[13px] text-slate-600">
                Admin (not changeable here)
              </p>
            ) : (
              <Select value={role} onValueChange={setRole} disabled={isSelf}>
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
            )}
            {isSelf && (
              <p className="text-xs text-slate-400">An admin cannot change their own role.</p>
            )}
          </div>

          <div className="flex flex-col gap-1.5">
            <Label htmlFor="status" className="text-[13px] font-semibold text-slate-600">
              Account Status
            </Label>
            <Select value={status} onValueChange={setStatus} disabled={isSelf}>
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
            {isSelf && (
              <p className="text-xs text-slate-400">An admin cannot suspend their own account.</p>
            )}
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
            <Button type="submit" disabled={isSaving} className="h-9 px-4 text-[13px] font-semibold">
              {isSaving ? (
                <>
                  <Loader2 className="size-4 animate-spin" />
                  Saving…
                </>
              ) : (
                "Save Changes"
              )}
            </Button>
          </div>

          <div className="h-px w-full bg-slate-200" />

          <div className="flex flex-col gap-3 rounded-lg border border-red-300 bg-red-50 p-4">
            <p className="text-[13px] font-bold text-red-700">Danger Zone</p>
            <div className="flex items-center justify-between gap-3">
              <div className="flex flex-col gap-0.5">
                <p className="text-xs font-semibold text-slate-900">
                  {user.active ? "Suspend Account" : "Reactivate Account"}
                </p>
                <p className="text-[11px] text-slate-600">
                  {user.active
                    ? "Suspending will prevent the user from logging in."
                    : "Reactivating will allow the user to log in again."}
                </p>
              </div>
              <Button
                type="button"
                variant="outline"
                disabled={isSelf || deactivateUser.isPending || activateUser.isPending}
                onClick={handleSuspendClick}
                className="h-7 border-red-300 px-3 text-xs font-semibold text-red-700 hover:bg-red-100"
              >
                {deactivateUser.isPending || activateUser.isPending ? (
                  <Loader2 className="size-3.5 animate-spin" />
                ) : user.active ? (
                  "Suspend User"
                ) : (
                  "Reactivate User"
                )}
              </Button>
            </div>
            {isSelf && (
              <p className="text-[11px] text-slate-400">An admin cannot suspend their own account.</p>
            )}
          </div>
        </form>
      </div>
    </div>
  );
}
