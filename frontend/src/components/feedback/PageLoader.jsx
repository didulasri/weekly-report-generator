import { Skeleton } from "@/components/ui/skeleton";

// Full-content-area loading state -- skeleton shapes, not a spinner (frontend-conventions skill:
// "Loading states use skeletons, not spinners, for content areas"). Generic enough to sit inside
// any page while its data is pending: a title-sized bar, then a few content-row-sized bars.
export default function PageLoader() {
  return (
    <div className="flex w-full flex-col gap-6 p-6" role="status" aria-label="Loading">
      <Skeleton className="h-7 w-48" />
      <div className="flex flex-col gap-3">
        <Skeleton className="h-24 w-full rounded-xl" />
        <Skeleton className="h-24 w-full rounded-xl" />
        <Skeleton className="h-24 w-full rounded-xl" />
      </div>
    </div>
  );
}
