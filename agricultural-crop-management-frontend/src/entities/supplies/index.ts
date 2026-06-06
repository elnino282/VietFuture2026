// Supplies Entity - Public API
// Only export what components need to use

// Types
export type {
  CreateSupplyItemRequest,
  CreateSupplierRequest,
  StockInRequest,
  StockInResponse,
  Supplier,
  SuppliersParams,
  SupplyItem,
  SupplyItemsParams,
  SupplyLot,
  SupplyLotsParams,
  UpdateSupplyItemRequest,
  UpdateSupplierRequest,
} from "./model/types";

// Schemas (for validation)
export {
  CreateSupplyItemRequestSchema,
  CreateSupplierRequestSchema,
  StockInRequestSchema,
  StockInResponseSchema,
  SupplierSchema,
  SupplyItemSchema,
  SupplyLotSchema,
  UpdateSupplyItemRequestSchema,
  UpdateSupplierRequestSchema,
} from "./model/schemas";

// Query Keys
export { suppliesKeys } from "./model/keys";

// API Client
export { suppliesApi } from "./api/client";

// React Query Hooks
export {
  useAllSuppliers,
  useAllSupplyItems,
  useEmployeeSeasonSupplyItems,
  useEmployeeSeasonSupplyLots,
  useCreateSupplyItem,
  useCreateSupplier,
  useDeleteSupplyItem,
  useDeleteSupplier,
  useStockIn,
  useSuppliers,
  useSupplyItems,
  useSupplyLots,
  useUpdateSupplyItem,
  useUpdateSupplier,
} from "./api/hooks";
