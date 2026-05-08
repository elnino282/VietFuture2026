import { LayoutDashboard, Package, ShoppingBag } from "lucide-react";
import { NavLink } from "react-router-dom";
import { cn } from "@/shared/lib";
import { Card, CardContent } from "@/shared/ui";

const sellerTabs = [
  {
    to: "/farmer/marketplace-dashboard",
    label: "Overview",
    description: "Store summary",
    icon: LayoutDashboard,
  },
  {
    to: "/farmer/marketplace-products",
    label: "Products",
    description: "Listings and stock",
    icon: Package,
  },
  {
    to: "/farmer/marketplace-orders",
    label: "Orders",
    description: "Buyer orders",
    icon: ShoppingBag,
  },
] as const;

export function SellerMarketplaceTabs() {
  return (
    <Card className="border border-border rounded-xl shadow-sm">
      <CardContent className="px-6 py-4">
        {/* Header Section */}
        <div className="mb-4">
          <p className="text-xs font-semibold uppercase tracking-[0.24em] text-emerald-600">
            FarmTrace Seller
          </p>
          <h2 className="mt-1 text-lg font-semibold text-foreground">Marketplace workspace</h2>
        </div>

        {/* Navigation Tabs */}
        <nav className="grid gap-3 md:grid-cols-3" aria-label="Marketplace seller tabs">
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
                      ? "border-emerald-200 bg-emerald-50 text-emerald-700 dark:border-emerald-800 dark:bg-emerald-950 dark:text-emerald-300"
                      : "border-border text-muted-foreground hover:border-border/80 hover:bg-muted hover:text-foreground",
                  )
                }
              >
                <div className="rounded-lg bg-card p-2 shadow-sm border border-border">
                  <Icon size={18} />
                </div>
                <div className="min-w-0">
                  <p className="text-sm font-semibold">{tab.label}</p>
                  <p className="text-xs text-muted-foreground">{tab.description}</p>
                </div>
              </NavLink>
            );
          })}
        </nav>
      </CardContent>
    </Card>
  );
}
