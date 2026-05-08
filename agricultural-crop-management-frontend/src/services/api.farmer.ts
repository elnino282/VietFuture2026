import httpClient from "@/shared/api/http";
import { parseApiResponse } from "@/shared/api/types";
import { z } from "zod";
import {
  CostReportSchema,
  ProfitReportSchema,
  RevenueReportSchema,
  YieldReportSchema,
  type CostReport,
  type ProfitReport,
  type RevenueReport,
  type YieldReport,
} from "./api.admin";

const NullableNumberSchema = z.preprocess(
  (value) => {
    if (value === null || value === undefined || value === "") {
      return null;
    }

    if (typeof value === "number") {
      return Number.isFinite(value) ? value : null;
    }

    const parsedValue = Number(value);
    return Number.isFinite(parsedValue) ? parsedValue : null;
  },
  z.number().nullable()
);

const SeasonCostCategoryBreakdownSchema = z.object({
  category: z.string().nullable().optional(),
  amount: NullableNumberSchema,
  percentageOfTotal: NullableNumberSchema,
});

const SeasonInventoryUsageSummarySchema = z.object({
  itemName: z.string().nullable().optional(),
  unit: z.string().nullable().optional(),
  totalOutQuantity: NullableNumberSchema,
  movementCount: z.coerce.number().int().nullable().optional(),
});

const SeasonCostOptimizationBaseSchema = z.object({
  seasonId: z.coerce.number().int().positive(),
  seasonName: z.string().nullable().optional(),
  budgetAmount: NullableNumberSchema,
  totalExpense: NullableNumberSchema,
  remainingBudget: NullableNumberSchema,
  expenseByCategory: z.array(SeasonCostCategoryBreakdownSchema).default([]),
  topCostCategories: z.array(SeasonCostCategoryBreakdownSchema).default([]),
  expectedYieldKg: NullableNumberSchema,
  actualYieldKg: NullableNumberSchema,
  costPerExpectedKg: NullableNumberSchema,
  costPerActualKg: NullableNumberSchema,
  laborCost: NullableNumberSchema,
  pesticideTreatmentCost: NullableNumberSchema,
  inventoryUsageSummary: z.array(SeasonInventoryUsageSummarySchema).default([]),
  warnings: z.array(z.string()).default([]),
  disclaimer: z.string().nullable().optional(),
});

const SeasonCostOptimizationSuggestionRequestSchema = z
  .object({
    question: z.string().min(1).max(2000).optional(),
    additionalNote: z.string().min(1).max(4000).optional(),
    includeInventory: z.boolean().optional(),
  })
  .optional();

export const SeasonCostOptimizationSummarySchema = SeasonCostOptimizationBaseSchema;

export const SeasonCostOptimizationAiSuggestionSchema =
  SeasonCostOptimizationBaseSchema.extend({
    aiSuggestionText: z.string().nullable().optional(),
    usedContextSummary: z.record(z.unknown()).nullable().optional(),
    generatedAt: z.string().nullable().optional(),
  });

export type SeasonCostCategoryBreakdown = z.infer<
  typeof SeasonCostCategoryBreakdownSchema
>;
export type SeasonInventoryUsageSummary = z.infer<
  typeof SeasonInventoryUsageSummarySchema
>;
export type SeasonCostOptimizationSummary = z.infer<
  typeof SeasonCostOptimizationSummarySchema
>;
export type SeasonCostOptimizationAiSuggestion = z.infer<
  typeof SeasonCostOptimizationAiSuggestionSchema
>;
export type SeasonCostOptimizationSuggestionRequest = z.infer<
  typeof SeasonCostOptimizationSuggestionRequestSchema
>;

export const farmerReportsApi = {
  getYieldReport: async (seasonId: number): Promise<YieldReport[]> => {
    const response = await httpClient.get("/api/v1/farmer/reports/yield", {
      params: { seasonId },
    });
    return parseApiResponse(response.data, z.array(YieldReportSchema));
  },

  getCostReport: async (seasonId: number): Promise<CostReport[]> => {
    const response = await httpClient.get("/api/v1/farmer/reports/cost", {
      params: { seasonId },
    });
    return parseApiResponse(response.data, z.array(CostReportSchema));
  },

  getRevenueReport: async (seasonId: number): Promise<RevenueReport[]> => {
    const response = await httpClient.get("/api/v1/farmer/reports/revenue", {
      params: { seasonId },
    });
    return parseApiResponse(response.data, z.array(RevenueReportSchema));
  },

  getProfitReport: async (seasonId: number): Promise<ProfitReport[]> => {
    const response = await httpClient.get("/api/v1/farmer/reports/profit", {
      params: { seasonId },
    });
    return parseApiResponse(response.data, z.array(ProfitReportSchema));
  },

  getCostOptimizationSummary: async (
    seasonId: number
  ): Promise<SeasonCostOptimizationSummary> => {
    const response = await httpClient.get(
      `/api/v1/seasons/${seasonId}/cost-optimization/summary`
    );
    return parseApiResponse(response.data, SeasonCostOptimizationSummarySchema);
  },

  getCostOptimizationAiSuggestion: async (
    seasonId: number,
    request?: SeasonCostOptimizationSuggestionRequest
  ): Promise<SeasonCostOptimizationAiSuggestion> => {
    const validatedPayload =
      SeasonCostOptimizationSuggestionRequestSchema.parse(request) ?? {};
    const response = await httpClient.post(
      `/api/v1/seasons/${seasonId}/cost-optimization/ai-suggestion`,
      validatedPayload
    );
    return parseApiResponse(
      response.data,
      SeasonCostOptimizationAiSuggestionSchema
    );
  },
};
