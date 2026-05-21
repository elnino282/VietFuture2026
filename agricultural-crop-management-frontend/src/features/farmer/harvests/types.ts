export type HarvestStatus = "stored" | "sold" | "processing";
export type HarvestGrade = "A" | "B" | "C" | "Premium";

export interface QCMetrics {
    purity: number;
    foreignMatter: number;
    brokenGrains: number;
}

export interface HarvestBatch {
    id: string;
    batchId: string;
    seasonId?: number;
    seasonName?: string;
    date: string;
    createdAt?: string | null;
    quantity: number;
    unitPrice?: number | null;
    revenue?: number | null;
    grade?: HarvestGrade | null;
    moisture?: number | null;
    linkedSale?: string;
    status?: HarvestStatus | null;
    season?: string;
    plot?: string;
    crop?: string;
    notes?: string;
    qcMetrics?: QCMetrics;
    photo?: string;
}

export interface HarvestFormData {
    batchId: string;
    date: string;
    quantity: string;
    grade: HarvestGrade;
    moisture: string;
    season: string;
    plot: string;
    crop: string;
    warehouseId: string;
    locationId: string;
    productId: string;
    productName: string;
    productVariant: string;
    lotCode: string;
    inventoryUnit: string;
    status: HarvestStatus;
    notes: string;
    purity: string;
    foreignMatter: string;
    brokenGrains: string;
}

export interface ChartDataPoint {
    date?: string;
    name?: string;
    quantity?: number;
    value?: number;
    color?: string;
}

export interface SummaryStats {
    totalStored: number;
    totalSold: number;
    totalProcessing: number;
    premiumGradePercentage: number;
}



