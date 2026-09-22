import SiteFooter from "@/components/layout/site-footer";
import SiteHeader from "@/components/layout/site-header";

export default function SiteLayout({ children }: { children: React.ReactNode }) {
  return <>
    <SiteHeader />
    {children}
    <SiteFooter />
  </>;
}
