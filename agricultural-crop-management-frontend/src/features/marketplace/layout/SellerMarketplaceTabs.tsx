import { LayoutDashboard, Package, ShoppingBag } from "lucide-react";
import { NavLink } from "react-router-dom";
import { useI18n } from "@/hooks/useI18n";
import { cn } from "@/shared/lib";
import { Card, CardContent } from "@/shared/ui";

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
    <Card variant="content" className="rounded-xl">
      <CardContent className="px-6 py-4">
        <div className="mb-4">
          <p className="text-xs font-semibold uppercase tracking-[0.24em] text-primary">
            {t("marketplaceSeller.tabs.brand", "Seller")}
          </p>
          <h2 className="mt-1 text-lg font-semibold text-foreground">
            {t("marketplaceSeller.tabs.title", "Marketplace workspace")}
          </h2>
        </div>

        <nav
          className="grid gap-3 md:grid-cols-3"
          aria-label={t("marketplaceSeller.tabs.ariaLabel", "Marketplace seller tabs")}
        >
          {sellerTabs.map((tab) => {
            const Icon = tab.icon;

            return (
              <NavLink
                key={tab.to}
                to={tab.to}
                end={tab.to.endsWith("dashboard")}
                className={({ isActive }) =>
                  cn(
                    "flex items-center gap-3 rounded-xl border px-4 py-3 transition-colors",
                    isActive
                      ? "border-primary/20 bg-primary/10 text-primary"
                      : "border-border text-muted-foreground hover:border-border/80 hover:bg-muted hover:text-foreground",
                  )
                }
              >
                <div className="rounded-lg bg-card p-2 shadow-sm border border-border">
                  <Icon size={18} />
                </div>
                <div className="min-w-0">
                  <p className="text-sm font-semibold">
                    {t(tab.labelKey, tab.labelFallback)}
                  </p>
                  <p className="text-xs text-muted-foreground">
                    {t(tab.descriptionKey, tab.descriptionFallback)}
                  </p>
                </div>
              </NavLink>
            );
          })}
        </nav>
      </CardContent>
    </Card>
  );
}
