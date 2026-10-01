import { NavLink } from "react-router-dom";
import { useCurrentUser } from "@/hooks/useAuth";
import { navItemsForRole } from "@/routes/navItems";
import { ROLE_LABELS } from "@/utils/constants";
import { getInitials } from "@/utils/initials";
import { cn } from "@/lib/utils";

// The nav content only -- no <aside>/Sheet wrapper here, so AppShell can place this exact same
// content in a persistent sidebar (md+) or inside a shadcn Sheet (below md) without duplicating
// the nav-item/role-filtering logic in two places.
export default function Sidebar({ onNavigate }) {
  const { data: user } = useCurrentUser();
  const items = navItemsForRole(user?.role);

  return (
    <div className="flex h-full w-full flex-col justify-between bg-sidebar p-4 text-sidebar-foreground">
      <div className="flex flex-col gap-6">
        <div className="flex items-center gap-2">
          <div className="flex size-7 shrink-0 items-center justify-center rounded-md bg-indigo-600">
            <span className="font-mono text-[13px] font-bold text-white">W</span>
          </div>
          <span className="text-[15px] font-bold text-white">Weekly Reports</span>
        </div>

        <nav className="flex flex-col gap-1">
          {items.map((item) => (
            <NavLink
              key={item.path}
              to={item.path}
              end
              onClick={onNavigate}
              className={({ isActive }) =>
                cn(
                  "flex items-center gap-3 rounded-md px-3 py-2 text-[13px] outline-none transition-colors",
                  "focus-visible:ring-2 focus-visible:ring-sidebar-ring",
                  isActive
                    ? "bg-sidebar-primary font-semibold text-sidebar-primary-foreground"
                    : "font-medium text-white/90 hover:bg-sidebar-accent hover:text-white"
                )
              }
            >
              <item.icon className="size-4 shrink-0" />
              <span className="flex-1">{item.label}</span>
            </NavLink>
          ))}
        </nav>
      </div>

      <div className="flex items-center gap-2.5 border-t border-sidebar-border pt-4">
        <div className="flex size-8 shrink-0 items-center justify-center rounded-full bg-sidebar-primary text-xs font-semibold text-sidebar-primary-foreground">
          {getInitials(user?.name)}
        </div>
        <div className="flex min-w-0 flex-1 flex-col gap-0.5">
          <p className="truncate text-[13px] font-semibold text-white">{user?.name}</p>
          <p className="truncate text-[11px] font-medium text-purple-200">
            {ROLE_LABELS[user?.role] ?? user?.role}
          </p>
        </div>
      </div>
    </div>
  );
}
