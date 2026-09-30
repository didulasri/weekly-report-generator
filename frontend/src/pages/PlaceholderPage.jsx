import PageHeader from "@/components/layout/PageHeader";

// Stand-in for every nav destination until its real page is built -- a heading and nothing else,
// per the app-shell checkpoint's explicit scope.
export default function PlaceholderPage({ title }) {
  return <PageHeader title={title} />;
}
