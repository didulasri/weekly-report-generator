// Shared static mock data for the My Reports list and the Edit Weekly Report screen -- UI-only
// checkpoint, no report service wired yet. `id` is what the row-click navigation and the edit
// route's useParams() key off of. Shapes mirror the eventual backend response (ReportStatus enum
// values) so StatusBadge/REPORT_STATUSES keep being the single source of truth for status display.
export const MOCK_REPORTS = [
  { id: "week-42", week: "Week 42 (Oct 14 - Oct 20)", weekRange: "Oct 14 - Oct 20, 2024", project: "Acme Platform", status: "APPROVED", hours: "38.5h", submitted: "Oct 20, 2024", reviewedBy: "Jane Doe (Manager)", current: true },
  { id: "week-41", week: "Week 41 (Oct 07 - Oct 13)", weekRange: "Oct 07 - Oct 13, 2024", project: "Acme Platform", status: "APPROVED", hours: "40.0h", submitted: "Oct 13, 2024", reviewedBy: "Jane Doe (Manager)" },
  { id: "week-40", week: "Week 40 (Sep 30 - Oct 06)", weekRange: "Sep 30 - Oct 06, 2024", project: "Billing Redesign", status: "SUBMITTED", hours: "39.0h", submitted: "Oct 06, 2024", reviewedBy: "Jane Doe (Manager)" },
  { id: "week-39", week: "Week 39 (Sep 23 - Sep 29)", weekRange: "Sep 23 - Sep 29, 2024", project: "Billing Redesign", status: "APPROVED", hours: "42.5h", submitted: "Sep 29, 2024", reviewedBy: "Jane Doe (Manager)" },
  {
    id: "week-38",
    week: "Week 38 (Sep 16 - Sep 22)",
    weekRange: "Sep 16 - Sep 20, 2024",
    project: "Security Audit",
    status: "NEEDS_CORRECTION",
    hours: "12.0h",
    submitted: "Sep 22, 2024",
    reviewedBy: "Jane Doe (Manager)",
    summary:
      "This week was focused entirely on setting up the fundamental pipeline architecture for the Acme Core integration. Accomplished all critical schemas, resolved blockers on auth mechanisms, and initiated mock payloads in the staging container. Overall, ahead of schedule for critical milestones.",
    tasks: [
      { id: "t1", text: "Setup secure JWT auth endpoints", status: "Completed", priority: "High", hours: "14.5" },
      { id: "t2", text: "Optimize batch database mutations", status: "In Progress", priority: "Medium", hours: "12.0" },
      { id: "t3", text: "Establish mock schemas for QA mapping and webhook tests", status: "Completed", priority: "Low", hours: "8.0", flagged: true },
    ],
    achievements:
      "1. Zero-Lag Batch Mutations: Successfully reduced payload iteration wait time to less than 40ms under heavy stress tests.",
    blockers:
      "Acme API Credentials Delay: Staging tests are halted temporarily pending production credential provisioning from client team.",
    feedback: {
      by: "Sarah Chen",
      date: "Sep 18, 2024",
      comment:
        "Please provide more detail about the deployment task and include specific hours for the QA testing work.",
    },
  },
  { id: "week-37", week: "Week 37 (Sep 09 - Sep 15)", weekRange: "Sep 09 - Sep 15, 2024", project: "Acme Platform", status: "APPROVED", hours: "40.0h", submitted: "Sep 15, 2024", reviewedBy: "Jane Doe (Manager)" },
  { id: "week-36", week: "Week 36 (Sep 02 - Sep 08)", weekRange: "Sep 02 - Sep 08, 2024", project: "Security Audit", status: "APPROVED", hours: "41.5h", submitted: "Sep 08, 2024", reviewedBy: "Jane Doe (Manager)" },
  { id: "week-35", week: "Week 35 (Aug 26 - Sep 01)", weekRange: "Aug 26 - Sep 01, 2024", project: "Acme Platform", status: "DRAFT", hours: "32.0h", submitted: "Not Submitted", reviewedBy: "—" },
];

export function getReportById(id) {
  return MOCK_REPORTS.find((report) => report.id === id);
}

// Rows without curated detail (everything but the one NEEDS_CORRECTION example) still need
// something sensible to show when clicked -- derives a plausible single-task breakdown from the
// row's own summary fields rather than leaving the edit screen blank.
export function reportDetail(report) {
  if (report.tasks) return report;
  return {
    ...report,
    summary: `Weekly summary for ${report.project} -- ${report.weekRange}.`,
    tasks: [{ id: "t1", text: `${report.project} work`, status: "Completed", priority: "Medium", hours: report.hours.replace("h", "") }],
    achievements: "No achievements recorded for this report yet.",
    blockers: "No blockers recorded for this report yet.",
    feedback: null,
  };
}
