// Single source of truth for the four ReportStatus values (docs/DATABASE_SCHEMA.md) and their
// display colours -- every screen that shows a status (dashboard tables, report detail, manager
// review) reads from here via StatusBadge instead of re-deciding "what colour is APPROVED" per
// screen. Colours lifted from the Figma design system's own badge styling (Approved/Draft/Pending
// Review), which turned out to be exact Tailwind default palette values -- extended with
// NEEDS_CORRECTION using the same design language (the red the mockup already uses for
// "Requires Action").
export const REPORT_STATUSES = {
  DRAFT: {
    label: "Draft",
    className: "bg-slate-50 text-slate-600 border-slate-200",
  },
  SUBMITTED: {
    label: "Pending Review",
    className: "bg-amber-50 text-amber-700 border-amber-200",
  },
  NEEDS_CORRECTION: {
    label: "Needs Correction",
    className: "bg-red-50 text-red-700 border-red-200",
  },
  APPROVED: {
    label: "Approved",
    className: "bg-emerald-50 text-emerald-700 border-emerald-200",
  },
};

// Human-readable labels for the three RoleName values (entity/enums/RoleName.java) -- shared by
// the sidebar's identity footer and the topbar's role badge so both say the same thing.
export const ROLE_LABELS = {
  TEAM_MEMBER: "Team Member",
  MANAGER: "Manager",
  ADMIN: "Admin",
};

