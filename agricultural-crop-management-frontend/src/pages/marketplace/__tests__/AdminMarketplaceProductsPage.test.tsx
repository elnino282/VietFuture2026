import { describe, expect, it, vi, beforeEach } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { AdminMarketplaceProductsPage } from "../AdminMarketplaceProductsPage";
import * as marketplaceHooks from "@/features/marketplace/hooks/useMarketplaceQueries";
import type { MarketplaceProductStatus } from "@/shared/api";

vi.mock("../../hooks/useMarketplaceQueries");

describe("AdminMarketplaceProductsPage", () => {
  let queryClient: QueryClient;

  beforeEach(() => {
    queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });
    vi.clearAllMocks();
  });

  const renderPage = () => {
    return render(
      <QueryClientProvider client={queryClient}>
        <AdminMarketplaceProductsPage />
      </QueryClientProvider>
    );
  };

  const createMockProduct = (overrides?: {
    id?: number;
    name?: string;
    status?: MarketplaceProductStatus;
    price?: number;
    imageUrl?: string;
    farmerDisplayName?: string;
    farmName?: string;
    category?: string;
    traceable?: boolean;
  }) => ({
    id: 1,
    name: "Test Product",
    status: "PENDING_REVIEW" as MarketplaceProductStatus,
    price: 50000,
    imageUrl: "https://example.com/image.jpg",
    farmerDisplayName: "Farmer Name",
    farmName: "Farm Name",
    category: "Vegetables",
    traceable: true,
    ...overrides,
  });

  it("renders product list with moderation actions", async () => {
    vi.mocked(marketplaceHooks.useMarketplaceAdminProducts).mockReturnValue({
      data: {
        items: [createMockProduct()],
        totalPages: 1,
        totalElements: 1,
        page: 0,
        size: 25,
      },
      isLoading: false,
      isError: false,
    } as any);

    vi.mocked(marketplaceHooks.useMarketplaceUpdateAdminProductStatusMutation).mockReturnValue({
      mutate: vi.fn(),
      mutateAsync: vi.fn(),
      isPending: false,
    } as any);

    renderPage();

    await waitFor(() => {
      expect(screen.getByText("Test Product")).toBeInTheDocument();
      expect(screen.getByText("Approve")).toBeInTheDocument();
      expect(screen.getByText("Hide")).toBeInTheDocument();
    });
  });

  it("approve action calls mutation without modal", async () => {
    const user = userEvent.setup();
    const mutateAsync = vi.fn().mockResolvedValue({});

    vi.mocked(marketplaceHooks.useMarketplaceAdminProducts).mockReturnValue({
      data: {
        items: [createMockProduct()],
        totalPages: 1,
        totalElements: 1,
        page: 0,
        size: 25,
      },
      isLoading: false,
      isError: false,
    } as any);

    vi.mocked(marketplaceHooks.useMarketplaceUpdateAdminProductStatusMutation).mockReturnValue({
      mutate: vi.fn((req) => mutateAsync(req)),
      mutateAsync,
      isPending: false,
    } as any);

    renderPage();

    const approveButton = await screen.findByRole("button", { name: /approve/i });
    await user.click(approveButton);

    expect(mutateAsync).toHaveBeenCalledWith({ status: "PUBLISHED" });
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
  });

  it("hide action opens modal and requires reason", async () => {
    const user = userEvent.setup();
    const mutateAsync = vi.fn().mockResolvedValue({});

    vi.mocked(marketplaceHooks.useMarketplaceAdminProducts).mockReturnValue({
      data: {
        items: [createMockProduct()],
        totalPages: 1,
        totalElements: 1,
        page: 0,
        size: 25,
      },
      isLoading: false,
      isError: false,
    } as any);

    vi.mocked(marketplaceHooks.useMarketplaceUpdateAdminProductStatusMutation).mockReturnValue({
      mutate: vi.fn(),
      mutateAsync,
      isPending: false,
    } as any);

    renderPage();

    const hideButton = await screen.findByRole("button", { name: /hide/i });
    await user.click(hideButton);

    expect(screen.getByRole("dialog")).toBeInTheDocument();
    expect(screen.getByText("Hide Product")).toBeInTheDocument();

    const confirmButton = screen.getByRole("button", { name: /confirm/i });
    expect(confirmButton).toBeDisabled();

    const textarea = screen.getByLabelText(/reason/i);
    await user.type(textarea, "Product images do not match description");

    expect(confirmButton).toBeEnabled();
    await user.click(confirmButton);

    await waitFor(() => {
      expect(mutateAsync).toHaveBeenCalledWith({
        status: "HIDDEN",
        statusReason: "Product images do not match description",
      });
    });
  });

  it("shows loading state when products are being fetched", () => {
    vi.mocked(marketplaceHooks.useMarketplaceAdminProducts).mockReturnValue({
      data: undefined,
      isLoading: true,
      isError: false,
    } as any);

    vi.mocked(marketplaceHooks.useMarketplaceUpdateAdminProductStatusMutation).mockReturnValue({
      mutate: vi.fn(),
      mutateAsync: vi.fn(),
      isPending: false,
    } as any);

    renderPage();

    expect(screen.getByText("Loading products...")).toBeInTheDocument();
  });

  it("shows error message when products fail to load", () => {
    vi.mocked(marketplaceHooks.useMarketplaceAdminProducts).mockReturnValue({
      data: undefined,
      isLoading: false,
      isError: true,
    } as any);

    vi.mocked(marketplaceHooks.useMarketplaceUpdateAdminProductStatusMutation).mockReturnValue({
      mutate: vi.fn(),
      mutateAsync: vi.fn(),
      isPending: false,
    } as any);

    renderPage();

    expect(screen.getByText("Failed to load admin marketplace products.")).toBeInTheDocument();
  });

  it("shows only Hide button for PUBLISHED products", async () => {
    vi.mocked(marketplaceHooks.useMarketplaceAdminProducts).mockReturnValue({
      data: {
        items: [createMockProduct({ status: "PUBLISHED" })],
        totalPages: 1,
        totalElements: 1,
        page: 0,
        size: 25,
      },
      isLoading: false,
      isError: false,
    } as any);

    vi.mocked(marketplaceHooks.useMarketplaceUpdateAdminProductStatusMutation).mockReturnValue({
      mutate: vi.fn(),
      mutateAsync: vi.fn(),
      isPending: false,
    } as any);

    renderPage();

    await waitFor(() => {
      expect(screen.getByText("Test Product")).toBeInTheDocument();
    });

    expect(screen.queryByRole("button", { name: /approve/i })).not.toBeInTheDocument();
    expect(screen.getByRole("button", { name: /hide/i })).toBeInTheDocument();
  });
});
