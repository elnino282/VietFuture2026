import { useCallback, useEffect, useRef, useState, type CSSProperties, type FormEvent } from "react";
import { Link, NavLink, Outlet, useNavigate } from "react-router-dom";
import { createPortal } from "react-dom";
import { cn } from "@/shared/lib";
import { ProductFilterDropdown } from "./ProductFilterDropdown";
import {
  ChevronDown,
  Facebook,
  Instagram,
  LogOut,
  Menu,
  Package,
  Search,
  ShoppingCart,
  Store,
  Twitter,
  User,
  X,
} from "lucide-react";
import { useAuth } from "@/features/auth";
import { Button } from "@/shared/ui";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/shared/ui";
import {
  useMarketplaceCartCount,
  useMarketplaceCartMergeBridge,
  useScrolled,
} from "../hooks";
import "./MarketplacePublicLayout.css";

function resolvePortalRoute(role: string | undefined): string {
  switch (role) {
    case "admin":
      return "/admin/dashboard";
    case "employee":
      return "/employee/tasks";
    case "buyer":
      return "/marketplace/orders";
    default:
      return "/farmer/marketplace-dashboard";
  }
}

function resolvePortalButtonLabel(role: string | undefined) {
  switch (role) {
    case "admin":
      return "Quản trị";
    case "employee":
      return "Công việc";
    default:
      return "Quản lý";
  }
}

function formatRoleLabel(role: string | undefined) {
  switch (role) {
    case "admin":
      return "Admin";
    case "employee":
      return "Employee";
    case "farmer":
      return "Farmer";
    default:
      return "Buyer";
  }
}

const NAV_LINKS = [
  { to: "/marketplace/farms", label: "Nông trại" },
  { to: "/marketplace/traceability", label: "Truy xuất" },
];

const PRODUCTS_NAV_ACTIVE_STYLE = { color: "#3BA55D", background: "#f0faf4" };

function ProductsNavItem() {
  const [isOpen, setIsOpen] = useState(false);
  const [dropdownStyle, setDropdownStyle] = useState<CSSProperties>();
  const closeTimerRef = useRef<ReturnType<typeof setTimeout> | undefined>(
    undefined,
  );
  const triggerRef = useRef<HTMLDivElement | null>(null);

  const updateDropdownPosition = useCallback(() => {
    const trigger = triggerRef.current;
    if (!trigger) return;

    const rect = trigger.getBoundingClientRect();
    const viewportPadding = 16;
    const gap = 10;
    const preferredWidth = Math.min(
      680,
      window.innerWidth * 0.92,
      window.innerWidth - viewportPadding * 2,
    );
    const left = Math.min(
      Math.max(rect.left, viewportPadding),
      window.innerWidth - preferredWidth - viewportPadding,
    );

    setDropdownStyle({
      position: "fixed",
      top: rect.bottom + gap,
      left,
      width: preferredWidth,
      maxWidth: `calc(100vw - ${viewportPadding * 2}px)`,
      zIndex: 2147483647,
    });
  }, []);

  const handleEnter = () => {
    clearTimeout(closeTimerRef.current);
    updateDropdownPosition();
    setIsOpen(true);
  };

  const handleLeave = () => {
    closeTimerRef.current = setTimeout(() => setIsOpen(false), 220);
  };

  const handleClose = () => {
    clearTimeout(closeTimerRef.current);
    setIsOpen(false);
  };

  useEffect(() => {
    if (!isOpen) return;

    updateDropdownPosition();
    window.addEventListener("resize", updateDropdownPosition);
    window.addEventListener("scroll", updateDropdownPosition, true);

    return () => {
      window.removeEventListener("resize", updateDropdownPosition);
      window.removeEventListener("scroll", updateDropdownPosition, true);
    };
  }, [isOpen, updateDropdownPosition]);

  useEffect(() => {
    return () => clearTimeout(closeTimerRef.current);
  }, []);

  return (
    <div
      className="relative"
      onKeyDown={(e) => {
        if (e.key === "Escape") handleClose();
      }}
    >
      <div ref={triggerRef} onMouseEnter={handleEnter} onMouseLeave={handleLeave}>
        <NavLink
          to="/marketplace/products"
          onFocus={handleEnter}
          onBlur={handleLeave}
          className={({ isActive }) =>
            cn(
              "fb-nav-link flex items-center gap-1 rounded-full px-3 py-1 text-sm font-medium transition-colors",
              isActive || isOpen
                ? "fb-active font-semibold ring-1 ring-[#3BA55D]"
                : "text-gray-600 hover:bg-gray-100 hover:text-emerald-600",
            )
          }
          style={({ isActive }) =>
            isActive || isOpen ? PRODUCTS_NAV_ACTIVE_STYLE : undefined
          }
        >
          {({ isActive }) => (
            <>
              <span
                style={isActive || isOpen ? { color: "#3BA55D" } : undefined}
              >
                Sản phẩm
              </span>
              <ChevronDown
                className={cn(
                  "h-3 w-3 transition-transform",
                  isOpen && "rotate-180",
                )}
                style={isActive || isOpen ? { color: "#3BA55D" } : undefined}
              />
            </>
          )}
        </NavLink>
      </div>

      {isOpen && dropdownStyle
        ? createPortal(
            <div
              className="marketplace-product-filter-portal"
              style={dropdownStyle}
              onMouseEnter={handleEnter}
              onMouseLeave={handleLeave}
            >
              <div className="absolute -top-3 left-0 h-3 w-full" aria-hidden="true" />
              <ProductFilterDropdown
                onMouseEnter={handleEnter}
                onMouseLeave={handleLeave}
                onClose={handleClose}
              />
            </div>,
            document.body,
          )
        : null}
    </div>
  );
}

function MarketplaceNavLink({
  to,
  label,
  onClick,
}: {
  to: string;
  label: string;
  onClick?: () => void;
}) {
  return (
    <NavLink
      to={to}
      onClick={onClick}
      className={({ isActive }) =>
        cn(
          "fb-nav-link rounded-full px-3 py-1 text-sm transition-colors",
          isActive
            ? "fb-active bg-emerald-50 font-semibold text-emerald-700 ring-1 ring-emerald-200"
            : "font-medium text-gray-600 hover:bg-gray-100 hover:text-emerald-600",
        )
      }
    >
      {label}
    </NavLink>
  );
}

function MarketplaceSearchBar({ className = "" }: { className?: string }) {
  const [query, setQuery] = useState("");
  const navigate = useNavigate();

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const trimmed = query.trim();
    if (!trimmed) {
      return;
    }

    navigate(`/marketplace/products?q=${encodeURIComponent(trimmed)}`);
  }

  return (
    <form onSubmit={handleSubmit} className={`relative ${className}`}>
      <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-500" />
      <input
        type="search"
        placeholder="Tìm kiếm nông sản, nông trại..."
        value={query}
        onChange={(event) => setQuery(event.target.value)}
        className="fb-search-input w-full rounded-full border border-transparent bg-gray-100 py-2.5 pl-10 pr-4 text-sm text-gray-700 outline-none transition focus:bg-white"
      />
    </form>
  );
}

function MarketplaceFooter() {
  return (
    <footer className="bg-gray-900 py-12 text-gray-300">
      <div className="container mx-auto px-4">
        <div className="grid grid-cols-1 gap-8 md:grid-cols-4">
          <div>
            <Link to="/marketplace" className="mb-4 flex items-center gap-2">
              <div className="rounded-md bg-emerald-600 p-1.5 text-white">
                <Package size={24} />
              </div>
              <span className="text-xl font-bold text-white">FarmTrace</span>
            </Link>
            <p className="mb-4 text-sm text-gray-400">
              Nền tảng giao dịch nông sản minh bạch, kết nối trực tiếp từ nông
              trại đến bàn ăn của bạn.
            </p>
            <div className="flex gap-4">
              <a
                href="#"
                className="transition-colors hover:text-white"
                aria-label="Facebook"
              >
                <Facebook size={20} />
              </a>
              <a
                href="#"
                className="transition-colors hover:text-white"
                aria-label="Twitter"
              >
                <Twitter size={20} />
              </a>
              <a
                href="#"
                className="transition-colors hover:text-white"
                aria-label="Instagram"
              >
                <Instagram size={20} />
              </a>
            </div>
          </div>

          <div>
            <h3 className="mb-4 font-semibold text-white">Khám phá</h3>
            <ul className="space-y-2 text-sm">
              <li>
                <Link
                  to="/marketplace/products"
                  className="hover:text-emerald-400"
                >
                  Tất cả sản phẩm
                </Link>
              </li>
              <li>
                <Link
                  to="/marketplace/farms"
                  className="hover:text-emerald-400"
                >
                  Nông trại tiêu biểu
                </Link>
              </li>
              <li>
                <Link
                  to="/marketplace/traceability"
                  className="hover:text-emerald-400"
                >
                  Truy xuất nguồn gốc
                </Link>
              </li>
            </ul>
          </div>

          <div>
            <h3 className="mb-4 font-semibold text-white">Hỗ trợ</h3>
            <ul className="space-y-2 text-sm text-gray-400">
              <li>Câu hỏi thường gặp</li>
              <li>Chính sách vận chuyển</li>
              <li>Đổi trả và hoàn tiền</li>
              <li>Liên hệ</li>
            </ul>
          </div>

          <div>
            <h3 className="mb-4 font-semibold text-white">
              Dành cho người bán
            </h3>
            <ul className="space-y-2 text-sm">
              <li>
                <Link to="/sign-up" className="hover:text-emerald-400">
                  Đăng ký bán hàng
                </Link>
              </li>
              <li className="text-gray-400">Hướng dẫn bán hàng</li>
              <li className="text-gray-400">Tiêu chuẩn chất lượng</li>
            </ul>
          </div>
        </div>

        <div className="mt-8 border-t border-gray-800 pt-8 text-center text-sm text-gray-500">
          <p>&copy; {new Date().getFullYear()} FarmTrace.</p>
        </div>
      </div>
    </footer>
  );
}

function MobileMenu({
  isOpen,
  onClose,
  isAuthenticated,
  userName,
  userRole,
  cartCount,
  onLogout,
}: {
  isOpen: boolean;
  onClose: () => void;
  isAuthenticated: boolean;
  userName?: string;
  userRole?: string;
  cartCount: number;
  onLogout: () => void;
}) {
  if (!isOpen) {
    return null;
  }

  const showPortalLink = Boolean(userRole && userRole !== "buyer");

  return (
    <div
      id="marketplace-mobile-menu"
      className="marketplace-header__mobile-menu border-t border-gray-200 bg-white"
    >
      <div className="space-y-4 px-4 py-4">
        <MarketplaceSearchBar className="w-full" />

        <nav className="flex flex-col gap-3">
          <Link
            to="/marketplace"
            onClick={onClose}
            className="text-sm font-medium text-gray-700 hover:text-emerald-600"
          >
            Trang chủ
          </Link>
          <Link
            to="/marketplace/products"
            onClick={onClose}
            className="text-sm font-medium text-gray-700 hover:text-emerald-600"
          >
            Sản phẩm
          </Link>
          {NAV_LINKS.map((link) => (
            <MarketplaceNavLink
              key={link.to}
              to={link.to}
              label={link.label}
              onClick={onClose}
            />
          ))}
        </nav>

        <div className="border-t border-gray-100 pt-3">
          {isAuthenticated ? (
            <div className="space-y-2">
              <div className="rounded-lg bg-gray-50 px-3 py-2">
                <p className="text-sm font-medium text-gray-900">
                  {userName ?? "Tài khoản của bạn"}
                </p>
                <p className="text-xs text-gray-500">
                  {formatRoleLabel(userRole)}
                </p>
              </div>

              <Link
                to="/marketplace/cart"
                onClick={onClose}
                className="flex items-center gap-2 text-sm text-gray-700 hover:text-emerald-600"
              >
                <ShoppingCart size={16} /> Giỏ hàng ({cartCount})
              </Link>

              {!showPortalLink ? (
                <Link
                  to="/marketplace/profile"
                  onClick={onClose}
                  className="flex items-center gap-2 text-sm text-gray-700 hover:text-emerald-600"
                >
                  <User size={16} /> Hồ sơ của tôi
                </Link>
              ) : null}

              <Link
                to="/marketplace/orders"
                onClick={onClose}
                className="flex items-center gap-2 text-sm text-gray-700 hover:text-emerald-600"
              >
                <Package size={16} /> Đơn hàng của tôi
              </Link>

              {showPortalLink ? (
                <Link
                  to={resolvePortalRoute(userRole)}
                  onClick={onClose}
                  className="flex items-center gap-2 text-sm text-gray-700 hover:text-emerald-600"
                >
                  <Store size={16} /> {resolvePortalButtonLabel(userRole)}
                </Link>
              ) : null}

              <Button
                type="button"
                variant="ghost"
                className="w-full justify-start px-0 text-sm text-gray-700 hover:text-emerald-600"
                onClick={() => {
                  onClose();
                  onLogout();
                }}
              >
                <LogOut size={16} /> Đăng xuất
              </Button>
            </div>
          ) : (
            <div className="flex flex-col gap-2">
              <Link to="/sign-in" onClick={onClose}>
                <Button variant="ghost" className="w-full justify-start">
                  Đăng nhập
                </Button>
              </Link>
              <Link to="/sign-up" onClick={onClose}>
                <Button className="w-full justify-start">Đăng ký</Button>
              </Link>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

export function MarketplacePublicLayout() {
  const { isAuthenticated, user, logout } = useAuth();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const showPortalAction = isAuthenticated && user?.role !== "buyer";

  useMarketplaceCartMergeBridge();
  const cartCount = useMarketplaceCartCount();
  const scrolled = useScrolled(80);

  return (
    <div className="flex min-h-screen flex-col bg-gray-50">
      <header
        className={cn(
          "marketplace-header fb-marketplace sticky top-0 z-50 w-full border-b border-gray-200 bg-white shadow-sm",
          scrolled && "fb-nav-scrolled",
        )}
      >
        <div className="marketplace-header__inner container mx-auto px-4 relative">
          <div className="marketplace-header__left">
            <Link to="/marketplace" className="flex items-center gap-2">
              <div className="rounded-md bg-emerald-600 p-1.5 text-white">
                <Package size={24} />
              </div>
              <span className="marketplace-header__brand-text text-xl font-bold text-emerald-800">
                FarmTrace
              </span>
            </Link>

            <nav className="marketplace-header__nav">
              <ProductsNavItem />
              {NAV_LINKS.map((link) => (
                <MarketplaceNavLink
                  key={link.to}
                  to={link.to}
                  label={link.label}
                />
              ))}
            </nav>
          </div>

          <div className="marketplace-header__desktop-search">
            <MarketplaceSearchBar />
          </div>

          <div className="marketplace-header__right">
            <Link
              to="/marketplace/cart"
              aria-label="Giỏ hàng"
              className="relative rounded-md p-2 text-gray-600 transition-colors hover:bg-gray-100 hover:text-emerald-600"
            >
              <ShoppingCart size={24} />
              {cartCount > 0 && (
                <span className="fb-cart-badge absolute right-0 top-0 inline-flex translate-x-1/4 -translate-y-1/4 items-center justify-center rounded-full bg-red-600 px-2 py-1 text-xs font-bold leading-none text-white">
                  {cartCount}
                </span>
              )}
            </Link>

            <div className="marketplace-header__desktop-actions">
              {isAuthenticated ? (
                <>
                  {showPortalAction ? (
                    <>
                      <div className="marketplace-header__user">
                        <span className="text-sm font-medium text-gray-900">
                          {user?.name ?? "Người dùng"}
                        </span>
                        <span className="text-xs text-gray-500">
                          {formatRoleLabel(user?.role)}
                        </span>
                      </div>

                      <Button asChild variant="outline" size="sm">
                        <Link to="/marketplace/orders">Đơn hàng</Link>
                      </Button>

                      <Button asChild variant="outline" size="sm">
                        <Link to={resolvePortalRoute(user?.role)}>
                          <Store size={14} />{" "}
                          {resolvePortalButtonLabel(user?.role)}
                        </Link>
                      </Button>

                      <Button
                        variant="ghost"
                        size="icon"
                        onClick={logout}
                        title="Đăng xuất"
                      >
                        <LogOut size={18} />
                      </Button>
                    </>
                  ) : (
                    <>
                      <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                          <Button
                            variant="ghost"
                            className="flex items-center gap-2"
                          >
                            <User size={16} />
                            <span className="text-sm font-medium">
                              {user?.name ?? "Người dùng"}
                            </span>
                            <ChevronDown size={14} />
                          </Button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="end" className="w-48">
                          <div className="px-2 py-1.5">
                            <p className="text-sm font-medium text-gray-900">
                              {user?.name ?? "Người dùng"}
                            </p>
                            <p className="text-xs text-gray-500">
                              {user?.email ?? ""}
                            </p>
                          </div>
                          <DropdownMenuSeparator />
                          <DropdownMenuItem asChild>
                            <Link
                              to="/marketplace/profile"
                              className="flex items-center cursor-pointer"
                            >
                              <User size={16} className="mr-2" />
                              Hồ sơ của tôi
                            </Link>
                          </DropdownMenuItem>
                          <DropdownMenuItem asChild>
                            <Link
                              to="/marketplace/orders"
                              className="flex items-center cursor-pointer"
                            >
                              <Package size={16} className="mr-2" />
                              Đơn hàng của tôi
                            </Link>
                          </DropdownMenuItem>
                          <DropdownMenuSeparator />
                          <DropdownMenuItem
                            onClick={logout}
                            className="cursor-pointer text-red-600"
                          >
                            <LogOut size={16} className="mr-2" />
                            Đăng xuất
                          </DropdownMenuItem>
                        </DropdownMenuContent>
                      </DropdownMenu>
                    </>
                  )}
                </>
              ) : (
                <>
                  <Button asChild variant="ghost" size="sm">
                    <Link to="/sign-in">
                      <User size={14} /> Đăng nhập
                    </Link>
                  </Button>
                  <Button asChild size="sm">
                    <Link to="/sign-up">Đăng ký</Link>
                  </Button>
                </>
              )}
            </div>

            <button
              type="button"
              className="marketplace-header__mobile-toggle rounded-md p-2 text-gray-600 transition-colors hover:bg-gray-100"
              onClick={() => setMobileMenuOpen((current) => !current)}
              aria-controls="marketplace-mobile-menu"
              aria-expanded={mobileMenuOpen}
              aria-label={mobileMenuOpen ? "Đóng menu" : "Mở menu"}
            >
              {mobileMenuOpen ? <X size={20} /> : <Menu size={20} />}
            </button>
          </div>
        </div>

        <MobileMenu
          isOpen={mobileMenuOpen}
          onClose={() => setMobileMenuOpen(false)}
          isAuthenticated={isAuthenticated}
          userName={user?.name}
          userRole={user?.role}
          cartCount={cartCount}
          onLogout={() => {
            void logout();
          }}
        />
      </header>

      <main className="flex-1">
        <Outlet />
      </main>

      <MarketplaceFooter />
    </div>
  );
}
