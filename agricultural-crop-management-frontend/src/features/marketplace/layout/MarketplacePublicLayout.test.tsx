import { render } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";
import { MarketplacePublicLayout } from "./MarketplacePublicLayout";

vi.mock("@/features/auth", () => ({
  useAuth: () => ({
    isAuthenticated: false,
    user: null,
    logout: vi.fn(),
  }),
}));

vi.mock("../hooks", () => ({
  useMarketplaceCartCount: () => 0,
  useMarketplaceCartMergeBridge: vi.fn(),
  useScrolled: () => false,
}));

describe("MarketplacePublicLayout document scrolling", () => {
  it("enables normal document scrolling while mounted and cleans up on unmount", () => {
    const { unmount } = render(
      <MemoryRouter initialEntries={["/marketplace"]}>
        <MarketplacePublicLayout />
      </MemoryRouter>,
    );

    expect(document.documentElement).toHaveClass("marketplace-document-scroll");
    expect(document.body).toHaveClass("marketplace-document-scroll");

    unmount();

    expect(document.documentElement).not.toHaveClass(
      "marketplace-document-scroll",
    );
    expect(document.body).not.toHaveClass("marketplace-document-scroll");
  });
});
