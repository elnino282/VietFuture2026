import { z } from 'zod';

const NumericSchema = z.preprocess((value) => {
    if (value === null || value === undefined || value === '') return value;
    if (typeof value === 'string') {
        const parsed = Number(value);
        return Number.isFinite(parsed) ? parsed : value;
    }
    return value;
}, z.number());

const NullableNumericSchema = z.preprocess((value) => {
    if (value === null || value === undefined || value === '') return null;
    if (typeof value === 'string') {
        const parsed = Number(value);
        return Number.isFinite(parsed) ? parsed : null;
    }
    if (typeof value === 'number') {
        return Number.isFinite(value) ? value : null;
    }
    return value;
}, z.number().nullable());

// ═══════════════════════════════════════════════════════════════
// DASHBOARD ZOD SCHEMAS
// Backend DTOs → Frontend validation
// ═══════════════════════════════════════════════════════════════

// Season Context nested schema
export const SeasonContextSchema = z.object({
    seasonId: z.number().nullable(),
    seasonName: z.string().nullable(),
    startDate: z.string().nullable(),
    endDate: z.string().nullable(),
    plannedHarvestDate: z.string().nullable(),
});

// Counts nested schema
export const CountsSchema = z.object({
    activeFarms: z.number(),
    activePlots: z.number(),
    seasonsByStatus: z.record(z.string(), z.number()),
});

// KPIs nested schema
export const KpisSchema = z.object({
    avgYieldTonsPerHa: z.number().nullable(),
    costPerHectare: z.number().nullable(),
    onTimePercent: z.number().nullable(),
});

// Expenses nested schema
export const ExpensesSchema = z.object({
    totalExpense: NullableNumericSchema,
});

// Harvest nested schema
export const HarvestSchema = z.object({
    totalQuantityKg: NullableNumericSchema,
    totalRevenue: NullableNumericSchema,
    expectedYieldKg: z.number().nullable(),
    yieldVsPlanPercent: z.number().nullable(),
});

// Alerts nested schema
export const AlertsSchema = z.object({
    openIncidents: z.number(),
    expiringLots: z.number(),
    lowStockItems: z.number(),
});

// Main Dashboard Overview Response
export const DashboardOverviewSchema = z.object({
    seasonContext: SeasonContextSchema.nullable(),
    counts: CountsSchema,
    kpis: KpisSchema,
    expenses: ExpensesSchema,
    harvest: HarvestSchema,
    alerts: AlertsSchema,
    unavailableReasons: z.array(z.string()),
    missingInputs: z.array(z.string()),
});

// Today Task Response
export const TodayTaskSchema = z.object({
    taskId: z.number(),
    title: z.string(),
    plotName: z.string().nullable(),
    type: z.string().nullable(),
    assigneeName: z.string().nullable(),
    dueDate: z.string().nullable(),
    status: z.string(),
});

export const DashboardDataCompletenessWarningSchema = z.object({
    warningId: z.string(),
    title: z.string(),
    source: z.string(),
    type: z.string(),
    status: z.string(),
    dueDate: z.string().nullable(),
    actionTarget: z.string(),
    seasonId: z.number().nullable(),
    inputCode: z.string(),
});

export const DashboardIncidentAlertSchema = z.object({
    id: z.string(),
    type: z.string(),
    severity: z.string(),
    title: z.string(),
    description: z.string().nullable().optional(),
    seasonId: z.number().nullable().optional(),
    plotId: z.number().nullable().optional(),
    createdAt: z.string().nullable().optional(),
    dueDate: z.string().nullable().optional(),
    actionUrl: z.string().nullable().optional(),
    actionTarget: z.string().nullable().optional(),
});

export const DashboardRecentActivitySchema = z.object({
    id: z.string(),
    type: z.string(),
    title: z.string(),
    description: z.string().nullable().optional(),
    occurredAt: z.string(),
    actorName: z.string().nullable().optional(),
    entityType: z.string(),
    entityId: z.string(),
    actionUrl: z.string().nullable().optional(),
});

// Plot Status Response
export const PlotStatusSchema = z.object({
    plotId: z.number(),
    plotName: z.string(),
    areaHa: z.number().nullable(),
    cropName: z.string().nullable(),
    stage: z.string().nullable(),
    health: z.string(),
});

// Low Stock Alert Response
export const LowStockAlertSchema = z.object({
    supplyLotId: z.number(),
    batchCode: z.string().nullable(),
    itemName: z.string(),
    warehouseName: z.string(),
    locationLabel: z.string().nullable(),
    onHand: z.number(),
    unit: z.string().nullable(),
});

export const DashboardInventoryAlertItemSchema = z.object({
    supplyLotId: z.number(),
    itemName: z.string(),
    lotCode: z.string().nullable(),
    warehouseName: z.string().nullable(),
    locationLabel: z.string().nullable(),
    quantity: NumericSchema,
    unit: z.string().nullable(),
    expiryDate: z.string().nullable(),
    alertType: z.string(),
    severity: z.string(),
    reason: z.string().nullable(),
    lastMovementAt: z.string().nullable(),
});

export const DashboardInventoryAlertsSummarySchema = z.object({
    totalAlerts: z.number().int(),
    lowStock: z.number().int(),
    expired: z.number().int(),
    expiringSoon: z.number().int(),
    noMovement: z.number().int(),
    abnormalMovement: z.number().int(),
});

export const DashboardInventoryAlertsResponseSchema = z.object({
    asOfDate: z.string(),
    lowStockThreshold: NullableNumericSchema,
    expiringSoonDays: z.number().int(),
    noMovementDays: z.number().int(),
    thresholdSource: z.string(),
    summary: DashboardInventoryAlertsSummarySchema,
    alerts: z.array(DashboardInventoryAlertItemSchema).default([]),
});

// Page Response for Today Tasks
export const TodayTasksPageSchema = z.object({
    content: z.array(TodayTaskSchema),
    pageable: z.any().optional(),
    totalElements: z.number(),
    totalPages: z.number(),
    size: z.number(),
    number: z.number(),
    first: z.boolean().optional(),
    last: z.boolean().optional(),
    empty: z.boolean().optional(),
});

export const SustainabilityScoreSchema = z.object({
    value: NullableNumericSchema,
    label: z.string(),
    components: z.record(z.string(), NullableNumericSchema),
    weights: z.record(z.string(), NullableNumericSchema),
});

export const DashboardFdnMetricsSchema = z.object({
    total: NullableNumericSchema,
    mineral: NullableNumericSchema,
    organic: NullableNumericSchema,
    level: z.enum(['low', 'medium', 'high']),
    status: z.enum(['measured', 'estimated', 'missing', 'unavailable']),
    thresholdSource: z.string(),
    lowMaxExclusive: NullableNumericSchema,
    mediumMaxExclusive: NullableNumericSchema,
    mineralHighMin: NullableNumericSchema,
    explanation: z.string(),
});

export const DashboardCurrentSeasonSchema = z.object({
    seasonName: z.string().nullable(),
    cropName: z.string().nullable(),
    dayCount: NullableNumericSchema,
    stage: z.string().nullable(),
});

export const DashboardYieldSummarySchema = z.object({
    estimated: NullableNumericSchema,
    unit: z.string(),
});

export const DashboardInputsBreakdownSchema = z.object({
    mineralFertilizerN: NullableNumericSchema,
    organicFertilizerN: NullableNumericSchema,
    biologicalFixationN: NullableNumericSchema,
    irrigationWaterN: NullableNumericSchema,
    atmosphericDepositionN: NullableNumericSchema,
    seedImportN: NullableNumericSchema,
    soilLegacyN: NullableNumericSchema,
    controlSupplyN: NullableNumericSchema,
});

export const DashboardDataQualitySchema = z.object({
    source: z.string(),
    method: z.string(),
    confidence: NullableNumericSchema,
});

export const DashboardDataQualitySummarySchema = z.object({
    overallConfidence: NullableNumericSchema,
    measuredInputCount: z.number().int(),
    estimatedInputCount: z.number().int(),
    missingInputCount: z.number().int(),
    unavailableInputCount: z.number().int(),
    summary: z.string(),
});

export const DashboardMetricSchema = z.object({
    value: NullableNumericSchema,
    unit: z.string(),
    status: z.enum(['measured', 'estimated', 'missing', 'unavailable']),
    confidence: NullableNumericSchema,
    calculationMode: z.string(),
    assumptions: z.array(z.string()).default([]),
    missingInputs: z.array(z.string()).default([]),
});

export const DashboardHistoryPointSchema = z.object({
    seasonId: z.number(),
    seasonName: z.string(),
    startDate: z.string(),
    fdnTotal: NullableNumericSchema,
    fdnMineral: NullableNumericSchema,
    fdnOrganic: NullableNumericSchema,
    nue: NullableNumericSchema,
    nOutput: NullableNumericSchema,
    yield: NullableNumericSchema,
});

export const DashboardFieldHistorySchema = z.array(DashboardHistoryPointSchema);

export const DashboardFdnOverviewSchema = z.object({
    scope: z.string(),
    entityId: z.string().nullable(),
    seasonId: z.number().nullable(),
    calculationMode: z.string(),
    confidence: NullableNumericSchema,
    sustainableScore: SustainabilityScoreSchema,
    fdn: DashboardFdnMetricsSchema,
    nue: NullableNumericSchema,
    nOutput: NullableNumericSchema,
    nSurplus: NullableNumericSchema,
    currentSeason: DashboardCurrentSeasonSchema.nullable(),
    yield: DashboardYieldSummarySchema,
    inputsBreakdown: DashboardInputsBreakdownSchema,
    unit: z.string(),
    dataQuality: z.array(DashboardDataQualitySchema),
    dataQualitySummary: DashboardDataQualitySummarySchema.nullable(),
    missingInputs: z.array(z.string()),
    unavailableReasons: z.array(z.string()),
    notes: z.array(z.string()),
    recommendations: z.array(z.string()),
    recommendationSource: z.string(),
    sustainableScoreMetric: DashboardMetricSchema,
    fdnTotalMetric: DashboardMetricSchema,
    fdnMineralMetric: DashboardMetricSchema,
    fdnOrganicMetric: DashboardMetricSchema,
    nueMetric: DashboardMetricSchema,
    nOutputMetric: DashboardMetricSchema,
    nSurplusMetric: DashboardMetricSchema,
    estimatedYieldMetric: DashboardMetricSchema,
    historicalTrend: z.array(DashboardHistoryPointSchema),
});

export const DashboardFieldMapItemSchema = z.object({
    fieldId: z.number(),
    fieldName: z.string(),
    farmId: z.number().nullable(),
    farmName: z.string().nullable(),
    boundaryGeoJson: z.object({
        type: z.string(),
        coordinates: z.any(),
    }).nullable().optional().default(null),
    center: z.object({
        lat: NumericSchema,
        lng: NumericSchema,
    }).nullable().optional().default(null),
    boundaryIssue: z.string().nullable().optional().default(null),
    cropName: z.string(),
    seasonName: z.string(),
    fdnLevel: z.enum(['low', 'medium', 'high']).default('medium'),
    fdnTotal: NullableNumericSchema,
    fdnMineral: NullableNumericSchema,
    fdnOrganic: NullableNumericSchema,
    nue: NullableNumericSchema,
    confidence: NullableNumericSchema,
    calculationMode: z.string(),
    thresholdSource: z.string(),
    recommendationSource: z.string(),
    missingInputs: z.array(z.string()).default([]),
    inputsBreakdown: DashboardInputsBreakdownSchema,
    recommendations: z.array(z.string()).default([]),
});

export const DashboardFieldMapViewportSchema = z.object({
    center: z.object({
        lat: NumericSchema,
        lng: NumericSchema,
    }),
    zoom: z.number().int(),
    source: z.string(),
});

export const DashboardFieldMapResponseSchema = z.object({
    fieldsWithBoundary: z.array(DashboardFieldMapItemSchema).default([]),
    fieldsMissingBoundary: z.array(DashboardFieldMapItemSchema).default([]),
    defaultViewport: DashboardFieldMapViewportSchema.nullable().default(null),
    unavailableReason: z.string().nullable().default(null),
});

export const DashboardFieldRecommendationSchema = z.object({
    fieldId: z.number(),
    seasonId: z.number().nullable(),
    fdnTotal: NullableNumericSchema,
    fdnMineral: NullableNumericSchema,
    nue: NullableNumericSchema,
    confidence: NullableNumericSchema,
    fdnLevel: z.enum(['low', 'medium', 'high']),
    thresholdSource: z.string(),
    recommendationSource: z.string(),
    calculationMode: z.string(),
    missingInputs: z.array(z.string()).default([]),
    recommendations: z.array(z.string()).default([]),
});
