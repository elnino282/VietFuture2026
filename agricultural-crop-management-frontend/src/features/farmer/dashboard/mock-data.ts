// src/features/farmer/dashboard/mock-data.ts
import { Season, FarmingLog, ReportData } from './types/dashboard-types';

export const mockSeasons: Season[] = [
  {
    id: '1',
    name: 'Vụ Hè Thu 2026',
    cropName: 'Lúa Đài Thơm 8',
    startDate: '2026-05-15',
    expectedEndDate: '2026-08-30',
    status: 'ACTIVE',
    farmId: 'f1',
    farmName: 'Nông trại Hạnh Phúc',
  },
  {
    id: '2',
    name: 'Vụ Thu Đông 2026',
    cropName: 'Ngô sinh khối',
    startDate: '2026-09-05',
    expectedEndDate: '2026-12-15',
    status: 'ACTIVE',
    farmId: 'f2',
    farmName: 'Nông trại Bình Minh',
  }
];

export const mockFarmingLogs: FarmingLog[] = [
  {
    id: 'log1',
    seasonId: 's1',
    date: '2026-07-22T08:30:00Z',
    activityType: 'FERTILIZER',
    description: 'Bón phân NPK đợt 2',
    materialName: 'Phân bón hỗn hợp NPK 16-16-8',
    amount: 50,
    unit: 'kg',
    performedBy: 'Nguyễn Văn A',
    status: 'COMPLETED',
    vietGapCompliant: true,
  },
  {
    id: 'log2',
    seasonId: 's1',
    date: '2026-07-21T16:00:00Z',
    activityType: 'WATERING',
    description: 'Bơm nước vào ruộng',
    soilCondition: 'Đất khô nứt nẻ',
    performedBy: 'Trần Văn B',
    status: 'COMPLETED',
    notes: 'Mực nước đạt 5cm',
    vietGapCompliant: true,
  },
  {
    id: 'log3',
    seasonId: 's1',
    date: '2026-07-23T07:00:00Z',
    activityType: 'PESTICIDE',
    description: 'Phun thuốc trừ rầy nâu',
    materialName: 'Thuốc trừ sâu sinh học Radiant',
    amount: 2,
    unit: 'lít',
    quarantineDays: 7,
    performedBy: 'Nguyễn Văn A',
    status: 'COMPLETED',
    vietGapCompliant: true,
  },
  {
    id: 'log4',
    seasonId: 's1',
    date: '2026-07-23T09:15:00Z',
    activityType: 'PESTICIDE',
    description: 'Phun thuốc trừ sâu hoá học',
    materialName: 'Thuốc trừ sâu Basudin 50EC',
    amount: 5,
    unit: 'lít',
    quarantineDays: 14,
    performedBy: 'Lê Thị C',
    status: 'COMPLETED',
    vietGapCompliant: false,
    vietGapReason: 'Sử dụng thuốc bảo vệ thực vật ngoài danh mục an toàn, hoặc quá liều lượng cho phép.',
  },
  {
    id: 'log5',
    seasonId: 's2',
    date: '2026-07-20T09:15:00Z',
    activityType: 'OTHER',
    description: 'Làm cỏ bờ',
    performedBy: 'Lê Thị C',
    status: 'COMPLETED',
  }
];

export const mockReportData: Record<string, ReportData> = {
  's1': {
    seasonId: 's1',
    totalYieldKg: 0,
    expectedYieldKg: 15000,
    totalExpenseVnd: 25000000,
    estimatedRevenueVnd: 120000000,
    yieldHistory: [
      { month: 'Tháng 5', yieldKg: 0 },
      { month: 'Tháng 6', yieldKg: 0 },
      { month: 'Tháng 7', yieldKg: 0 },
    ],
    expenseByCategory: [
      { category: 'Giống', amountVnd: 5000000 },
      { category: 'Phân bón', amountVnd: 12000000 },
      { category: 'Thuốc BVTV', amountVnd: 3000000 },
      { category: 'Nhân công', amountVnd: 5000000 },
    ]
  },
  's2': {
    seasonId: 's2',
    totalYieldKg: 0,
    expectedYieldKg: 8000,
    totalExpenseVnd: 10000000,
    estimatedRevenueVnd: 45000000,
    yieldHistory: [],
    expenseByCategory: [
      { category: 'Giống', amountVnd: 3000000 },
      { category: 'Phân bón', amountVnd: 5000000 },
      { category: 'Khác', amountVnd: 2000000 },
    ]
  }
};
