import { Link, NavLink, useNavigate } from "react-router-dom";
import { Search, ShoppingCart, Store, Wheat, User } from "lucide-react";
import { useAuth } from "@/features/auth";
import { Button } from "@/shared/ui";
import { useState, type FormEvent } from "react";

/**
 * PublicHeader Component
 *
 * Marketplace / buyer-facing header with:
 * - ACM Marketplace branding and navigation
 * - Search bar for products and farms
 * - Cart CTA (authenticated only)
 * - "Seller Channel" / "Farmer Channel" button for farmer/admin roles
 * - Sign-in / Create account for unauthenticated users
 *
 * Mounted only in PublicLayout paths.
 */

function PublicNavLink({ to, label }: { to: string; label: string }) {
  return (
    <NavLink
      to={to}
      className={({ isActive }) =>
        isActive
          ? "text-primary font-semibold"
          : "text-muted-foreground hover:text-primary transition-colors"
      }
      end={to === "/marketplace"}
    >
      {label}
    </NavLink>
  );
}

function PublicSearchBar() {
  const [query, setQuery] = useState("");
  const navigate = useNavigate();

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    const trimmed = query.trim();
    if (trimmed) {
      navigate(`/marketplace/products?q=${encodeURIComponent(trimmed)}`);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="relative hidden lg:block">
      <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
      <input
        type="search"
        value={query}
        onChange={(e) => setQuery(e.target.value)}
        placeholder="Search products, farms..."
        className="w-64 rounded-full border border-border bg-muted py-2 pl-9 pr-4 text-sm text-foreground placeholder:text-muted-foreground focus:border-primary focus:bg-background focus:outline-none focus:ring-2 focus:ring-ring/20 xl:w-80"
      />
    </form>
  );
}

function resolvePortalRoute(role: string | undefined): string {
  switch (role) {
    case "admin":
      return "/admin/dashboard";
    case "employee":
      return "/employee/tasks";
    case "buyer":
      return "/marketplace/orders";
    default:
      return "/farmer/dashboard";
  }
}

function resolveChannelLabel(role: string | undefined): string {
  switch (role) {
    case "admin":
      return "Admin Channel";
    case "farmer":
      return "Seller Channel";
    default:
      return "Internal Portal";
  }
}

export function PublicHeader() {
  const { isAuthenticated, user } = useAuth();
  const role = user?.role;
  const showChannelSwitch = role === "farmer" || role === "admin";

  return (
    <header className="sticky top-0 z-40 border-b border-border bg-background/95 backdrop-blur">
      <div className="mx-auto flex h-16 w-full max-w-7xl items-center justify-between gap-4 px-4 sm:px-6 lg:px-8">
        {/* Logo */}
        <Link to="/marketplace" className="flex shrink-0 items-center gap-2">
          <div className="rounded-md bg-primary p-1.5 text-primary-foreground">
            <Wheat size={20} />
          </div>
          <span className="font-semibold tracking-tight text-foreground">
            ACM Marketplace
          </span>
        </Link>

        {/* Navigation */}
        <nav className="hidden items-center gap-5 text-sm md:flex">
          <PublicNavLink to="/marketplace" label="Home" />
          <PublicNavLink to="/marketplace/products" label="Products" />
        </nav>

        <PublicSearchBar />

        {/* Right actions */}
        <div className="flex shrink-0 items-center gap-2">
          {isAuthenticated ? (
            <>
              <Link to="/marketplace/cart" className="relative inline-flex">
                <Button variant="outline" size="sm" className="gap-2">
                  <ShoppingCart size={16} /> Cart
                </Button>
              </Link>
              <Link to="/marketplace/orders">
                <Button variant="outline" size="sm">
                  Orders
                </Button>
              </Link>
              {showChannelSwitch ? (
                <Link to={resolvePortalRoute(role)}>
                  <Button size="sm" className="gap-2">
                    <Store size={14} />
                    {resolveChannelLabel(role)}
                  </Button>
                </Link>
              ) : (
                <Link to={resolvePortalRoute(role)}>
                  <Button variant="outline" size="sm" className="gap-2">
                    <User size={14} />
                    My Account
                  </Button>
                </Link>
              )}
            </>
          ) : (
            <>
              <span className="hidden rounded-md bg-secondary px-2 py-1 text-xs font-medium text-secondary-foreground sm:inline-flex">
                Guest mode
              </span>
              <Link to="/sign-up">
                <Button size="sm">Create account</Button>
              </Link>
              <Link to="/sign-in">
                <Button variant="outline" size="sm">Sign in</Button>
              </Link>
            </>
          )}
        </div>
      </div>
    </header>
  );
}
