import { z } from "zod";

export const MARKETPLACE_API_PREFIX = "/api/v1/marketplace";

export type MarketplaceApiResponse<T> = {
  code: string;
  message: string;
  result: T;
  data?: T;
  generatedAt?: string;
  status?: number;
};

export type MarketplacePage<T> = {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

const MarketplaceEnvelopeSchema = z.object({
  code: z.string(),
  message: z.string(),
  result: z.unknown().optional(),
  data: z.unknown().optional(),
  generatedAt: z.string().optional(),
  status: z.number().optional(),
});

export class MarketplaceApiClientError extends Error {
  readonly code: string;
  readonly status?: number;

  constructor(message: string, code = "ERR_MARKETPLACE_CLIENT", status?: number) {
    super(message);
    this.name = "MarketplaceApiClientError";
    this.code = code;
    this.status = status;
  }
}

export function parseMarketplaceEnvelope<T>(
  payload: unknown,
): MarketplaceApiResponse<T> {
  const parsed = MarketplaceEnvelopeSchema.parse(payload);
  const data = parsed.data ?? parsed.result;
  if (data === undefined) {
    throw new MarketplaceApiClientError("Marketplace response payload is missing both 'data' and 'result'.");
  }
  return {
    code: parsed.code,
    message: parsed.message,
    status: parsed.status,
    generatedAt: parsed.generatedAt,
    result: data as T,
    data: data as T,
  };
}

export function assertMarketplaceDashboardContract<T extends Record<string, unknown>>(
  payload: T,
  requiredFields: readonly (keyof T)[],
  context: string,
): T {
  for (const field of requiredFields) {
    if (payload[field] === undefined) {
      throw new MarketplaceApiClientError(
        `${context} is missing required field '${String(field)}'.`,
        "ERR_MARKETPLACE_CONTRACT",
      );
    }
  }
  return payload;
}

export function okMarketplaceResponse<T>(
  result: T,
  message = "OK",
  code = "SUCCESS",
): MarketplaceApiResponse<T> {
  return {
    code,
    message,
    result,
    data: result,
  };
}

export function toMarketplaceClientError(error: unknown): MarketplaceApiClientError {
  if (error instanceof MarketplaceApiClientError) {
    return error;
  }

  if (typeof error === "object" && error !== null) {
    const maybeError = error as {
      message?: string;
      response?: {
        status?: number;
        data?: {
          code?: string;
          message?: string;
        };
      };
    };

    const status = maybeError.response?.status;
    const code = maybeError.response?.data?.code ?? "ERR_MARKETPLACE_CLIENT";
    const message =
      maybeError.response?.data?.message ??
      maybeError.message ??
      "Marketplace API request failed.";

    return new MarketplaceApiClientError(message, code, status);
  }

  return new MarketplaceApiClientError("Marketplace API request failed.");
}
