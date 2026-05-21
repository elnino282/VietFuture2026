import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { QueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import {
  marketplaceApi,
  type MarketplaceAddCartItemRequest,
  type MarketplaceAdminOrderQuery,
  type MarketplaceAdminProductQuery,
  type MarketplaceAdminStats,
  type MarketplaceAddressUpsertRequest,
  type MarketplaceCart,
  type MarketplaceCreateOrderRequest,
  type MarketplaceCreateReviewRequest,
  type MarketplaceFarmerDashboard,
  type MarketplaceFarmerOrderQuery,
  type MarketplaceFarmerProductFormOptions,
  type MarketplaceFarmerProductQuery,
  type MarketplaceFarmerProductUpsertRequest,
  type MarketplaceFarmQuery,
  type MarketplaceOrderQuery,
  type MarketplaceOrderAuditLog,
  type MarketplaceOrderStatus,
  type MarketplaceProductPage,
  type MarketplaceProductQuery,
  type MarketplaceReviewQuery,
  type MarketplaceUpdateOrderStatusRequest,
  type MarketplaceUpdatePaymentVerificationRequest,
  type MarketplaceUpdateProductStatusRequest,
  type MarketplaceUpdateCartItemRequest,
  type MarketplaceStatsUnavailableReason,
} from "@/shared/api";

export const marketplaceQueryKeys = {
  root: ["marketplace"] as const,
  productsBase: () => [...marketplaceQueryKeys.root, "products"] as const,
  products: (query?: MarketplaceProductQuery) =>
    [...marketplaceQueryKeys.productsBase(), query ?? {}] as const,
  product: (slug?: string) => [...marketplaceQueryKeys.root, "product", slug ?? ""] as const,
  productReviews: (productId?: number, query?: MarketplaceReviewQuery) =>
    [...marketplaceQueryKeys.root, "product-reviews", productId ?? 0, query ?? {}] as const,
  farmsBase: () => [...marketplaceQueryKeys.root, "farms"] as const,
  farms: (query?: MarketplaceFarmQuery) =>
    [...marketplaceQueryKeys.farmsBase(), query ?? {}] as const,
  farm: (farmId?: number) => [...marketplaceQueryKeys.root, "farm", farmId ?? 0] as const,
  traceability: (productId?: number | null) =>
    [...marketplaceQueryKeys.root, "traceability", productId ?? 0] as const,
  cart: () => [...marketplaceQueryKeys.root, "cart"] as const,
  ordersBase: () => [...marketplaceQueryKeys.root, "orders"] as const,
  orders: (query?: MarketplaceOrderQuery) =>
    [...marketplaceQueryKeys.ordersBase(), query ?? {}] as const,
  order: (orderId?: number) => [...marketplaceQueryKeys.root, "order", orderId ?? 0] as const,
  addresses: () => [...marketplaceQueryKeys.root, "addresses"] as const,
  farmerDashboard: () => [...marketplaceQueryKeys.root, "farmer-dashboard"] as const,
  farmerProductsBase: () => [...marketplaceQueryKeys.root, "farmer-products"] as const,
  farmerProducts: (query?: MarketplaceFarmerProductQuery) =>
    [...marketplaceQueryKeys.farmerProductsBase(), query ?? {}] as const,
  farmerProduct: (productId?: number) =>
    [...marketplaceQueryKeys.root, "farmer-product", productId ?? 0] as const,
  farmerOrdersBase: () => [...marketplaceQueryKeys.root, "farmer-orders"] as const,
  farmerOrders: (query?: MarketplaceFarmerOrderQuery) =>
    [...marketplaceQueryKeys.farmerOrdersBase(), query ?? {}] as const,
  farmerOrder: (orderId?: number) => [...marketplaceQueryKeys.root, "farmer-order", orderId ?? 0] as const,
  adminProductsBase: () => [...marketplaceQueryKeys.root, "admin-products"] as const,
  adminProducts: (query?: MarketplaceAdminProductQuery) =>
    [...marketplaceQueryKeys.adminProductsBase(), query ?? {}] as const,
  adminOrdersBase: () => [...marketplaceQueryKeys.root, "admin-orders"] as const,
  adminOrders: (query?: MarketplaceAdminOrderQuery) =>
    [...marketplaceQueryKeys.adminOrdersBase(), query ?? {}] as const,
  adminOrder: (orderId?: number) => [...marketplaceQueryKeys.root, "admin-order", orderId ?? 0] as const,
  adminOrderAuditLogs: (orderId?: number) =>
    [...marketplaceQueryKeys.root, "admin-order-audit-logs", orderId ?? 0] as const,
  adminStats: () => [...marketplaceQueryKeys.root, "admin-stats"] as const,
};

async function invalidateMarketplaceCheckoutQueries(
  queryClient: QueryClient,
): Promise<void> {
  await Promise.all([
    queryClient.invalidateQueries({ queryKey: marketplaceQueryKeys.cart() }),
    queryClient.invalidateQueries({ queryKey: marketplaceQueryKeys.ordersBase() }),
  ]);
}

type CartMutationContext = {
  previousCart?: ReturnType<QueryClient["getQueryData"]>;
};

function recalculateCart(cart: MarketplaceCart): MarketplaceCart {
  return {
    ...cart,
    itemCount: cart.items.reduce((total, item) => total + item.quantity, 0),
    subtotal: cart.items.reduce(
      (total, item) => total + item.unitPrice * item.quantity,
      0,
    ),
  };
}

function updateCartItemInCache(
  cart: MarketplaceCart | undefined,
  productId: number,
  quantity: number,
): MarketplaceCart | undefined {
  if (!cart) {
    return cart;
  }

  const items = cart.items.map((item) =>
    item.productId === productId ? { ...item, quantity } : item,
  );
  return recalculateCart({ ...cart, items });
}

function removeCartItemFromCache(
  cart: MarketplaceCart | undefined,
  productId: number,
): MarketplaceCart | undefined {
  if (!cart) {
    return cart;
  }

  const items = cart.items.filter((item) => item.productId !== productId);
  return recalculateCart({ ...cart, items });
}

function isMarketplaceUnavailableReason(value: string): value is MarketplaceStatsUnavailableReason {
  return value === "NO_PRODUCTS"
    || value === "NO_ORDERS"
    || value === "NO_REVENUE_DATA"
    || value === "NO_COMPLETED_ORDERS";
}

function normalizeUnavailableReasons(
  reasons: string[] | undefined,
): MarketplaceStatsUnavailableReason[] {
  return Array.from(
    new Set((Array.isArray(reasons) ? reasons : []).filter(isMarketplaceUnavailableReason)),
  );
}

function normalizeFarmerDashboard(
  dashboard: MarketplaceFarmerDashboard,
): MarketplaceFarmerDashboard {
  return {
    ...dashboard,
    unavailableReasons: normalizeUnavailableReasons(dashboard.unavailableReasons),
    lastOrderAt: dashboard.lastOrderAt ?? null,
  };
}

function normalizeAdminStats(stats: MarketplaceAdminStats): MarketplaceAdminStats {
  return {
    ...stats,
    unavailableReasons: normalizeUnavailableReasons(stats.unavailableReasons),
    lastOrderAt: stats.lastOrderAt ?? null,
  };
}

export function useMarketplaceProducts(query?: MarketplaceProductQuery) {
  return useQuery({
    queryKey: marketplaceQueryKeys.products(query),
    queryFn: async () => {
      const response = await marketplaceApi.listProducts(query);
      return response.result;
    },
  });
}

export function useMarketplaceCategories(enabled: boolean) {
  return useQuery({
    queryKey: [...marketplaceQueryKeys.productsBase(), "categories"] as const,
    enabled,
    queryFn: async () => {
      const response = await marketplaceApi.listProducts({ size: 100 });
      return response.result;
    },
    select: (data) =>
      Array.from(new Set(data.items.map((p) => p.category).filter(Boolean))).sort(),
    staleTime: 5 * 60 * 1000,
  });
}

export function useMarketplaceProductDetail(slug?: string) {
  return useQuery({
    queryKey: marketplaceQueryKeys.product(slug),
    enabled: Boolean(slug),
    queryFn: async () => {
      const response = await marketplaceApi.getProductBySlug(slug ?? "");
      return response.result;
    },
  });
}

export function useMarketplaceProductReviews(productId?: number, query?: MarketplaceReviewQuery) {
  return useQuery({
    queryKey: marketplaceQueryKeys.productReviews(productId, query),
    enabled: Boolean(productId && productId > 0),
    queryFn: async () => {
      const response = await marketplaceApi.listProductReviews(productId ?? 0, query);
      return response.result;
    },
  });
}

export function useMarketplaceFarms(query?: MarketplaceFarmQuery) {
  return useQuery({
    queryKey: marketplaceQueryKeys.farms(query),
    queryFn: async () => {
      const response = await marketplaceApi.listFarms(query);
      return response.result;
    },
  });
}

export function useMarketplaceFarmDetail(farmId?: number) {
  return useQuery({
    queryKey: marketplaceQueryKeys.farm(farmId),
    enabled: Boolean(farmId && farmId > 0),
    queryFn: async () => {
      const response = await marketplaceApi.getFarmDetail(farmId ?? 0);
      return response.result;
    },
  });
}

export function useMarketplaceTraceability(productId?: number | null) {
  return useQuery({
    queryKey: marketplaceQueryKeys.traceability(productId),
    enabled: Boolean(productId && productId > 0),
    queryFn: async () => {
      const response = await marketplaceApi.getTraceability(productId ?? 0);
      return response.result;
    },
  });
}

export function useMarketplaceCart(options?: { enabled?: boolean }) {
  return useQuery({
    queryKey: marketplaceQueryKeys.cart(),
    enabled: options?.enabled ?? true,
    queryFn: async () => {
      const response = await marketplaceApi.getCart();
      return response.result;
    },
  });
}

export function useMarketplaceAddCartItemMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (request: MarketplaceAddCartItemRequest) => {
      const response = await marketplaceApi.addCartItem(request);
      return response.result;
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: marketplaceQueryKeys.cart() });
    },
  });
}

export function useMarketplaceUpdateCartItemMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({
      productId,
      request,
    }: {
      productId: number;
      request: MarketplaceUpdateCartItemRequest;
    }) => {
      const response = await marketplaceApi.updateCartItem(productId, request);
      return response.result;
    },
    onMutate: async ({ productId, request }): Promise<CartMutationContext> => {
      await queryClient.cancelQueries({ queryKey: marketplaceQueryKeys.cart() });
      const previousCart = queryClient.getQueryData(marketplaceQueryKeys.cart());
      queryClient.setQueryData<MarketplaceCart>(marketplaceQueryKeys.cart(), (cart) =>
        updateCartItemInCache(cart, productId, request.quantity),
      );
      return { previousCart };
    },
    onError: (_error, _variables, context) => {
      if (context?.previousCart) {
        queryClient.setQueryData(marketplaceQueryKeys.cart(), context.previousCart);
      }
      toast.error("Không thể cập nhật giỏ hàng. Vui lòng thử lại.");
    },
    onSuccess: () => {
      toast.success("Đã cập nhật giỏ hàng.");
    },
    onSettled: async () => {
      await invalidateMarketplaceCheckoutQueries(queryClient);
    },
  });
}

export function useMarketplaceRemoveCartItemMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (productId: number) => {
      const response = await marketplaceApi.removeCartItem(productId);
      return response.result;
    },
    onMutate: async (productId): Promise<CartMutationContext> => {
      await queryClient.cancelQueries({ queryKey: marketplaceQueryKeys.cart() });
      const previousCart = queryClient.getQueryData(marketplaceQueryKeys.cart());
      queryClient.setQueryData<MarketplaceCart>(marketplaceQueryKeys.cart(), (cart) =>
        removeCartItemFromCache(cart, productId),
      );
      return { previousCart };
    },
    onError: (_error, _productId, context) => {
      if (context?.previousCart) {
        queryClient.setQueryData(marketplaceQueryKeys.cart(), context.previousCart);
      }
      toast.error("Không thể xoá sản phẩm khỏi giỏ hàng lúc này.");
    },
    onSuccess: () => {
      toast.success("Đã xoá sản phẩm khỏi giỏ hàng.");
    },
    onSettled: async () => {
      await invalidateMarketplaceCheckoutQueries(queryClient);
    },
  });
}

export function useMarketplaceMergeCartMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: marketplaceApi.mergeCart,
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: marketplaceQueryKeys.cart() });
    },
  });
}

export function useMarketplaceCreateOrderMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (request: MarketplaceCreateOrderRequest) => {
      const response = await marketplaceApi.createOrder(request);
      return response.result;
    },
    onSuccess: async () => {
      await invalidateMarketplaceCheckoutQueries(queryClient);
    },
  });
}

export function useMarketplaceOrders(query?: MarketplaceOrderQuery) {
  return useQuery({
    queryKey: marketplaceQueryKeys.orders(query),
    queryFn: async () => {
      const response = await marketplaceApi.listOrders(query);
      return response.result;
    },
  });
}

export function useMarketplaceOrderDetail(orderId?: number) {
  return useQuery({
    queryKey: marketplaceQueryKeys.order(orderId),
    enabled: Boolean(orderId && orderId > 0),
    queryFn: async () => {
      const response = await marketplaceApi.getOrderDetail(orderId ?? 0);
      return response.result;
    },
  });
}

export function useMarketplaceCancelOrderMutation(orderId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async () => {
      const response = await marketplaceApi.cancelOrder(orderId);
      return response.result;
    },
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: marketplaceQueryKeys.order(orderId) }),
        queryClient.invalidateQueries({ queryKey: marketplaceQueryKeys.ordersBase() }),
      ]);
    },
  });
}

export function useMarketplaceUploadPaymentProofMutation(orderId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (file: File) => {
      const response = await marketplaceApi.uploadOrderPaymentProof(orderId, file);
      return response.result;
    },
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: marketplaceQueryKeys.order(orderId) }),
        queryClient.invalidateQueries({ queryKey: marketplaceQueryKeys.ordersBase() }),
      ]);
    },
  });
}

export function useMarketplaceAddresses() {
  return useQuery({
    queryKey: marketplaceQueryKeys.addresses(),
    queryFn: async () => {
      const response = await marketplaceApi.listAddresses();
      return response.result;
    },
  });
}

export function useMarketplaceCreateAddressMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (request: MarketplaceAddressUpsertRequest) => {
      const response = await marketplaceApi.createAddress(request);
      return response.result;
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: marketplaceQueryKeys.addresses() });
    },
  });
}

export function useMarketplaceUpdateAddressMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({
      addressId,
      request,
    }: {
      addressId: number;
      request: MarketplaceAddressUpsertRequest;
    }) => {
      const response = await marketplaceApi.updateAddress(addressId, request);
      return response.result;
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: marketplaceQueryKeys.addresses() });
    },
  });
}

export function useMarketplaceDeleteAddressMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (addressId: number) => {
      await marketplaceApi.deleteAddress(addressId);
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: marketplaceQueryKeys.addresses() });
    },
  });
}

export function useMarketplaceCreateReviewMutation(productId?: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (request: MarketplaceCreateReviewRequest) => {
      const response = await marketplaceApi.createReview(request);
      return response.result;
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: marketplaceQueryKeys.productReviews(productId),
      });
    },
  });
}

export function useMarketplaceOrdersByStatus(status?: MarketplaceOrderStatus) {
  return useMarketplaceOrders({
    status,
    page: 0,
    size: 20,
  });
}

export function useMarketplaceFarmerDashboard() {
  return useQuery({
    queryKey: marketplaceQueryKeys.farmerDashboard(),
    queryFn: async () => {
      const response = await marketplaceApi.getFarmerDashboard();
      return response.result;
    },
    select: normalizeFarmerDashboard,
  });
}

export function useMarketplaceFarmerProducts(query?: MarketplaceFarmerProductQuery) {
  return useQuery({
    queryKey: marketplaceQueryKeys.farmerProducts(query),
    queryFn: async () => {
      const response = await marketplaceApi.listFarmerProducts(query);
      return response.result;
    },
    select: (page: MarketplaceProductPage): MarketplaceProductPage => ({
      ...page,
      items: page.items ?? [],
    }),
  });
}

export function useMarketplaceFarmerProductFormOptions() {
  return useQuery({
    queryKey: [...marketplaceQueryKeys.farmerProductsBase(), "form-options"] as const,
    queryFn: async (): Promise<MarketplaceFarmerProductFormOptions> => {
      const response = await marketplaceApi.getFarmerProductFormOptions();
      return response.result;
    },
  });
}

export function useMarketplaceFarmerProductDetail(productId?: number) {
  return useQuery({
    queryKey: marketplaceQueryKeys.farmerProduct(productId),
    enabled: Boolean(productId && productId > 0),
    queryFn: async () => {
      const response = await marketplaceApi.getFarmerProductDetail(productId ?? 0);
      return response.result;
    },
  });
}

export function useMarketplaceCreateFarmerProductMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (request: MarketplaceFarmerProductUpsertRequest) => {
      const response = await marketplaceApi.createFarmerProduct(request);
      return response.result;
    },
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: marketplaceQueryKeys.farmerProductsBase() }),
        queryClient.invalidateQueries({ queryKey: marketplaceQueryKeys.farmerDashboard() }),
        queryClient.invalidateQueries({ queryKey: [...marketplaceQueryKeys.farmerProductsBase(), "form-options"] }),
      ]);
    },
  });
}

export function useMarketplaceUpdateFarmerProductMutation(productId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (request: MarketplaceFarmerProductUpsertRequest) => {
      const response = await marketplaceApi.updateFarmerProduct(productId, request);
      return response.result;
    },
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: marketplaceQueryKeys.farmerProductsBase() }),
        queryClient.invalidateQueries({ queryKey: marketplaceQueryKeys.farmerProduct(productId) }),
        queryClient.invalidateQueries({ queryKey: marketplaceQueryKeys.farmerDashboard() }),
        queryClient.invalidateQueries({ queryKey: [...marketplaceQueryKeys.farmerProductsBase(), "form-options"] }),
      ]);
    },
  });
}

export function useMarketplaceUpdateFarmerProductStatusMutation(productId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (request: MarketplaceUpdateProductStatusRequest) => {
      const response = await marketplaceApi.updateFarmerProductStatus(productId, request);
      return response.result;
    },
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: marketplaceQueryKeys.farmerProductsBase() }),
        queryClient.invalidateQueries({ queryKey: marketplaceQueryKeys.farmerProduct(productId) }),
        queryClient.invalidateQueries({ queryKey: marketplaceQueryKeys.farmerDashboard() }),
      ]);
    },
  });
}

export function useMarketplaceFarmerOrders(query?: MarketplaceFarmerOrderQuery) {
  return useQuery({
    queryKey: marketplaceQueryKeys.farmerOrders(query),
    queryFn: async () => {
      const response = await marketplaceApi.listFarmerOrders(query);
      return response.result;
    },
  });
}

export function useMarketplaceFarmerOrderDetail(orderId?: number) {
  return useQuery({
    queryKey: marketplaceQueryKeys.farmerOrder(orderId),
    enabled: Boolean(orderId && orderId > 0),
    queryFn: async () => {
      const response = await marketplaceApi.getFarmerOrderDetail(orderId ?? 0);
      return response.result;
    },
  });
}

export function useMarketplaceUpdateFarmerOrderStatusMutation(orderId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (request: MarketplaceUpdateOrderStatusRequest) => {
      const response = await marketplaceApi.updateFarmerOrderStatus(orderId, request);
      return response.result;
    },
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: marketplaceQueryKeys.farmerOrdersBase() }),
        queryClient.invalidateQueries({ queryKey: marketplaceQueryKeys.farmerOrder(orderId) }),
        queryClient.invalidateQueries({ queryKey: marketplaceQueryKeys.farmerDashboard() }),
      ]);
    },
  });
}

export function useMarketplaceAdminProducts(query?: MarketplaceAdminProductQuery) {
  return useQuery({
    queryKey: marketplaceQueryKeys.adminProducts(query),
    queryFn: async () => {
      const response = await marketplaceApi.listAdminProducts(query);
      return response.result;
    },
  });
}

export function useMarketplaceUpdateAdminProductStatusMutation(productId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (request: MarketplaceUpdateProductStatusRequest) => {
      const response = await marketplaceApi.updateAdminProductStatus(productId, request);
      return response.result;
    },
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: marketplaceQueryKeys.adminProductsBase() }),
        queryClient.invalidateQueries({ queryKey: marketplaceQueryKeys.adminStats() }),
      ]);
    },
  });
}

export function useMarketplaceAdminOrders(query?: MarketplaceAdminOrderQuery) {
  return useQuery({
    queryKey: marketplaceQueryKeys.adminOrders(query),
    queryFn: async () => {
      const response = await marketplaceApi.listAdminOrders(query);
      return response.result;
    },
  });
}

export function useMarketplaceAdminOrderDetail(orderId?: number) {
  return useQuery({
    queryKey: marketplaceQueryKeys.adminOrder(orderId),
    enabled: Boolean(orderId && orderId > 0),
    queryFn: async () => {
      const response = await marketplaceApi.getAdminOrderDetail(orderId ?? 0);
      return response.result;
    },
  });
}

export function useMarketplaceUpdateAdminOrderPaymentVerificationMutation(orderId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (request: MarketplaceUpdatePaymentVerificationRequest) => {
      const response = await marketplaceApi.updateAdminOrderPaymentVerification(orderId, request);
      return response.result;
    },
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: marketplaceQueryKeys.adminOrder(orderId) }),
        queryClient.invalidateQueries({ queryKey: marketplaceQueryKeys.adminOrdersBase() }),
        queryClient.invalidateQueries({ queryKey: marketplaceQueryKeys.adminOrderAuditLogs(orderId) }),
      ]);
    },
  });
}

export function useMarketplaceUpdateAdminOrderStatusMutation(orderId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (request: MarketplaceUpdateOrderStatusRequest) => {
      const response = await marketplaceApi.updateAdminOrderStatus(orderId, request);
      return response.result;
    },
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: marketplaceQueryKeys.adminOrder(orderId) }),
        queryClient.invalidateQueries({ queryKey: marketplaceQueryKeys.adminOrdersBase() }),
        queryClient.invalidateQueries({ queryKey: marketplaceQueryKeys.adminOrderAuditLogs(orderId) }),
        queryClient.invalidateQueries({ queryKey: marketplaceQueryKeys.adminStats() }),
      ]);
    },
  });
}

export function useMarketplaceAdminOrderAuditLogs(orderId?: number) {
  return useQuery({
    queryKey: marketplaceQueryKeys.adminOrderAuditLogs(orderId),
    enabled: Boolean(orderId && orderId > 0),
    queryFn: async (): Promise<MarketplaceOrderAuditLog[]> => {
      const response = await marketplaceApi.listAdminOrderAuditLogs(orderId ?? 0);
      return response.result;
    },
  });
}

export function useMarketplaceAdminStats() {
  return useQuery({
    queryKey: marketplaceQueryKeys.adminStats(),
    queryFn: async () => {
      const response = await marketplaceApi.getAdminStats();
      return response.result;
    },
    select: normalizeAdminStats,
  });
}
