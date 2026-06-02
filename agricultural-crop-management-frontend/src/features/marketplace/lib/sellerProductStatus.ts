import type {
  MarketplaceProductStatus,
  MarketplaceUpdateProductStatusRequest,
} from "@/shared/api";

export function getNextSellerProductStatusAction(
  status: MarketplaceProductStatus,
): MarketplaceUpdateProductStatusRequest | null {
  switch (status) {
    case "DRAFT":
      return { status: "PENDING_REVIEW" };
    case "PENDING_REVIEW":
      return { status: "DRAFT" };
    case "ACTIVE":
      return { status: "INACTIVE" };
    case "INACTIVE":
      return { status: "ACTIVE" };
    case "PUBLISHED":
      return { status: "HIDDEN" };
    case "HIDDEN":
      return { status: "PENDING_REVIEW" };
    default:
      return null;
  }
}

export function getNextSellerProductStatusLabel(
  status: MarketplaceProductStatus,
): string {
  switch (status) {
    case "DRAFT":
      return "Submit for review";
    case "PENDING_REVIEW":
      return "Move back to draft";
    case "ACTIVE":
      return "Hide product";
    case "INACTIVE":
      return "Show product";
    case "PUBLISHED":
      return "Hide product";
    case "HIDDEN":
      return "Resubmit for review";
    default:
      return "Update status";
  }
}
