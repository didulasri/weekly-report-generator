import { useState } from "react";
import { Outlet } from "react-router-dom";
import Sidebar from "./Sidebar";
import Topbar from "./Topbar";
import { Sheet, SheetContent, SheetTitle } from "@/components/ui/sheet";

// The layout route wrapping every protected page (see App.jsx). Below md the sidebar lives inside
// a Sheet triggered from Topbar's hamburger; md and up it's a persistent <aside>. Both render the
// exact same <Sidebar> content so nav items/role-filtering only exist in one place.
export default function AppShell() {
  const [mobileNavOpen, setMobileNavOpen] = useState(false);

  return (
    <div className="flex h-screen w-full overflow-hidden bg-[#f8f8fa]">
      <aside className="hidden w-60 shrink-0 md:block">
        <Sidebar />
      </aside>

      <Sheet open={mobileNavOpen} onOpenChange={setMobileNavOpen}>
        <SheetContent side="left" className="w-60 gap-0 border-sidebar-border bg-sidebar p-0 sm:max-w-60">
          {/* Radix Dialog requires an accessible title; visually hidden since Sidebar already
              renders the "Weekly Reports" brand mark as the visible heading. */}
          <SheetTitle className="sr-only">Navigation</SheetTitle>
          <Sidebar onNavigate={() => setMobileNavOpen(false)} />
        </SheetContent>
      </Sheet>

      <div className="flex min-w-0 flex-1 flex-col">
        <Topbar onOpenMobileNav={() => setMobileNavOpen(true)} />
        <main className="flex-1 overflow-y-auto p-4 sm:p-6">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
