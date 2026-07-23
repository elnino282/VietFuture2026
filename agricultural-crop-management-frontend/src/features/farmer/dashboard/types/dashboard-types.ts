// src/features/farmer/dashboard/types/dashboard-types.ts

export interface Season {
  id: string;
  name: string;
  cropName: string;
  startDate: string;
  expectedEndDate: string;
  status: 'ACTIVE' | 'COMPLETED' | 'CANCELLED';
  farmId: string;
  farmName: string;
}

export interface FarmingLog {
  id: string;
  seasonId: string;
  date: string;
  activityType: 'FERTILIZER' | 'PESTICIDE' | 'WATERING' | 'HARVEST' | 'OTHER';
  description: string;
  amount?: number;
  unit?: string;
  performedBy: string;
  status: 'COMPLETED' | 'PENDING' | 'CANCELLED';
  notes?: string;

  // Additional fields for Phase 2 (Hot Data details)
  materialName?: string; // Tên phân bón / thuốc BVTV
  quarantineDays?: number; // Thời gian cách ly
  soilCondition?: string; // Tình trạng đất

  // VietGAP Compliance
  vietGapCompliant?: boolean;
  vietGapReason?: string;
}

export interface ReportData {
  seasonId: string;
  totalYieldKg: number;
  expectedYieldKg: number;
  totalExpenseVnd: number;
  estimatedRevenueVnd: number;
  yieldHistory: { month: string; yieldKg: number }[];
  expenseByCategory: { category: string; amountVnd: number }[];
}
