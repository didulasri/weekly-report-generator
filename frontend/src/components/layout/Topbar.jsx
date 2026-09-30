import { Menu, LogOut } from "lucide-react";
import { useCurrentUser, useLogout } from "@/hooks/useAuth";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { ROLE_LABELS } from "@/utils/constants";
import { getInitials } from "@/utils/initials";

// Hamburger (mobile only, opens the Sheet in AppShell) on the left; identity + role badge + an
// account menu (Logout, wired to the existing useLogout() hook) on the right. No breadcrumbs or
// page-specific actions here -- those belong to PageHeader on each individual page, not the
// shell chrome that's the same on every route.
export default function Topbar({ onOpenMobileNav }) {
  const { data: user } = useCurrentUser();
  const logoutMutation = useLogout();

  return (
    <header className="flex h-14 w-full shrink-0 items-center justify-between border-b border-slate-200 bg-white px-4 sm:px-6">
      <Button
        type="button"
        variant="ghost"
        size="icon"
        className="md:hidden"
        onClick={onOpenMobileNav}
        aria-label="Open navigation menu"
      >
        <Menu className="size-5" />
      </Button>

      <div className="ml-auto flex items-center gap-3">
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <button
              type="button"
              className="flex items-center gap-2.5 rounded-md px-2 py-1.5 outline-none hover:bg-slate-50 focus-visible:ring-2 focus-visible:ring-ring/50"
            >
              <div className="flex size-8 shrink-0 items-center justify-center rounded-full bg-primary text-xs font-semibold text-primary-foreground">
                {getInitials(user?.name)}
              </div>
              <span className="hidden text-sm font-medium text-slate-900 sm:inline">
                {user?.name}
              </span>
              <Badge variant="secondary" className="hidden sm:inline-flex">
                {ROLE_LABELS[user?.role] ?? user?.role}
              </Badge>
            </button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end" className="w-48">
            <DropdownMenuLabel className="sm:hidden">
              <span className="block truncate font-medium">{user?.name}</span>
              <span className="block text-xs font-normal text-muted-foreground">
                {ROLE_LABELS[user?.role] ?? user?.role}
              </span>
            </DropdownMenuLabel>
            <DropdownMenuSeparator className="sm:hidden" />
            <DropdownMenuItem
              variant="destructive"
              disabled={logoutMutation.isPending}
              onSelect={() => logoutMutation.mutate()}
            >
              <LogOut className="size-4" />
              {logoutMutation.isPending ? "Logging out…" : "Logout"}
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>
    </header>
  );
}
