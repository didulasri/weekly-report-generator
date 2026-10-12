// Monday-start week helpers shared by the three dashboards -- mirrors
// DashboardService#resolveWeekStart on the backend (previousOrSame MONDAY), so "this week" means
// the same thing on both sides.

// Local-calendar-day formatting, NOT date.toISOString().slice(0, 10) -- toISOString() converts to
// UTC first, which silently shifts the date backward by one day for any timezone ahead of UTC
// (midnight local on the 5th is still the 4th in UTC). These dates round-trip to the backend as
// exact weekStartDate/weekEndDate values, so a one-day slip here would submit the wrong week.
function toIsoDate(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

export function currentWeekRange() {
  const now = new Date();
  const day = now.getDay(); // 0 (Sun) - 6 (Sat)
  const diffToMonday = day === 0 ? -6 : 1 - day;
  const monday = new Date(now);
  monday.setDate(now.getDate() + diffToMonday);
  const sunday = new Date(monday);
  sunday.setDate(monday.getDate() + 6);
  return { start: toIsoDate(monday), end: toIsoDate(sunday) };
}

// Snaps an arbitrary "YYYY-MM-DD" (e.g. from a <input type="date">) to the Monday-Sunday week it
// falls in -- the backend rejects any weekStartDate that isn't a Monday, so the form never lets
// the user submit a non-Monday start in the first place. Parses the parts manually (not `new
// Date(dateInput)`, which treats a bare date string as UTC midnight) so this always agrees with
// the same local calendar day the user picked, regardless of timezone offset.
export function mondayOf(dateInput) {
  const [year, month, day] = dateInput.split("-").map(Number);
  const picked = new Date(year, month - 1, day);
  const diffToMonday = picked.getDay() === 0 ? -6 : 1 - picked.getDay();
  const monday = new Date(picked);
  monday.setDate(picked.getDate() + diffToMonday);
  const sunday = new Date(monday);
  sunday.setDate(monday.getDate() + 6);
  return { start: toIsoDate(monday), end: toIsoDate(sunday) };
}

export function formatWeekLabel(startIso, endIso) {
  const start = new Date(startIso);
  const end = new Date(endIso);
  const fmt = (d) => d.toLocaleDateString("en-US", { month: "short", day: "numeric" });
  return `${fmt(start)} - ${fmt(end)}, ${end.getFullYear()}`;
}
