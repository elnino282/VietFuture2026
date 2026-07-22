/**
 * Buyer Dashboard Page
 * Placeholder dashboard until buyer features are implemented.
 */

export function BuyerDashboardPage() {
  return (
    <div className="flex-1 p-4 md:p-6 bg-background">
      <div className="flex h-full min-h-[60vh] items-center justify-center">
        <div className="w-full max-w-md bg-card border border-border rounded-[24px] px-8 py-10 text-center shadow-sm">
          <h2 className="text-2xl font-bold text-foreground mb-3">
            Buyer Dashboard
          </h2>
          <p className="text-muted-foreground text-sm">
            We are getting your buyer workspace ready. Check back soon for orders,
            catalogs, and procurement tools.
          </p>
        </div>
      </div>
    </div>
  );
}
