import { LayoutDashboard, Package, ShoppingBag } from "lucide-react";
import { NavLink } from "react-router-dom";
// eslint-disable-next-line no-restricted-imports
import { useI18n } from "@/hooks/useI18n";
import { cn } from "@/shared/lib";

const sellerTabs = [
  {
    to: "/farmer/marketplace-dashboard",
    labelKey: "marketplaceSeller.tabs.items.overview.label",
    labelFallback: "Overview",
    descriptionKey: "marketplaceSeller.tabs.items.overview.description",
    descriptionFallback: "Store summary",
    icon: LayoutDashboard,
  },
  {
    to: "/farmer/marketplace-products",
    labelKey: "marketplaceSeller.tabs.items.products.label",
    labelFallback: "Products",
    descriptionKey: "marketplaceSeller.tabs.items.products.description",
    descriptionFallback: "Listings and stock",
    icon: Package,
  },
  {
    to: "/farmer/marketplace-orders",
    labelKey: "marketplaceSeller.tabs.items.orders.label",
    labelFallback: "Orders",
    descriptionKey: "marketplaceSeller.tabs.items.orders.description",
    descriptionFallback: "Buyer orders",
    icon: ShoppingBag,
  },
] as const;

export function SellerMarketplaceTabs() {
  const { t } = useI18n();

  return (
    <nav
      className="rounded-lg border border-border bg-card p-2 shadow-sm"
      aria-label={t("marketplaceSeller.tabs.ariaLabel", "Marketplace seller tabs")}
    >
      <div className="grid gap-2 md:grid-cols-3">
        {sellerTabs.map((tab) => {
          const Icon = tab.icon;

          return (
            <NavLink
              key={tab.to}
              to={tab.to}
              end={tab.to.endsWith("dashboard")}
              className={({ isActive }) =>
                cn(
                  "flex min-w-0 items-center gap-3 rounded-md px-3 py-2.5 text-sm transition-colors",
                  isActive
                    ? "bg-primary/10 text-primary ring-1 ring-primary/20"
                    : "text-muted-foreground hover:bg-muted hover:text-foreground",
                )
              }
            >
              <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-md bg-background text-current ring-1 ring-border/70">
                <Icon size={17} />
              </span>
              <span className="min-w-0">
                <span className="block truncate font-semibold">{t(tab.labelKey, tab.labelFallback)}</span>
                <span className="block truncate text-xs text-muted-foreground">
                  {t(tab.descriptionKey, tab.descriptionFallback)}
                </span>
              </span>
            </NavLink>
          );
        })}
      </div>
    </nav>
  );
}
