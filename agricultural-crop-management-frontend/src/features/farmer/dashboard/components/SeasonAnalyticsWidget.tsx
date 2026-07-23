import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { 
  TrendingUp, 
  DollarSign, 
  ShoppingCart, 
  BarChart as BarChartIcon, 
  PieChart as PieChartIcon,
  Loader2
} from 'lucide-react';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip as RechartsTooltip,
  Legend,
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
} from 'recharts';

import { Card, CardContent, CardHeader, CardTitle } from '@/shared/ui/card';
import { formatMoney } from '@/shared/lib';
import { dashboardApi } from '../api/dashboardApi';

interface SeasonAnalyticsWidgetProps {
  seasonId: string;
}

export function SeasonAnalyticsWidget({ seasonId }: SeasonAnalyticsWidgetProps) {
  const { data: reportData, isLoading, error } = useQuery({
    queryKey: ['seasonStats', seasonId],
    queryFn: () => dashboardApi.getSeasonStats(seasonId),
    enabled: !!seasonId,
    retry: 1
  });

  if (error) {
    throw error; // Let the ErrorBoundary catch it
  }

  if (isLoading || !reportData) {
    return (
      <div className="flex flex-col items-center justify-center h-[400px] text-muted-foreground p-8 space-y-4 pt-6 mt-6 border-t border-border/50">
        <Loader2 className="w-8 h-8 animate-spin text-primary" />
        <p>Đang tải dữ liệu thống kê báo cáo...</p>
      </div>
    );
  }

  const COLORS = ['#10b981', '#f59e0b', '#3b82f6', '#8b5cf6', '#ec4899'];

  // Data for Bar Chart
  const yieldChartData = [
    { 
      name: 'Mùa vụ hiện tại', 
      expected: reportData.expectedYieldKg ?? 0, 
      actual: reportData.totalYieldKg ?? 0
    }
  ];

  const expenseByCategory = reportData.expenseByCategory ?? [];

  // Dummy pending orders
  const pendingOrdersCount = 12;

  return (
    <div className="space-y-6 pt-6 mt-6 border-t border-border/50">
      <div className="flex items-center gap-2 mb-2">
        <BarChartIcon className="w-6 h-6 text-primary" />
        <h3 className="text-xl font-bold text-foreground">Báo cáo & Thống kê</h3>
      </div>

      {/* KPI Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        {/* KPI 1: Yield */}
        <Card className="border-border shadow-sm">
          <CardContent className="p-5 flex items-center justify-between">
            <div className="space-y-1">
              <p className="text-xs font-medium text-muted-foreground uppercase tracking-wider">Sản lượng đạt được</p>
              <p className="text-2xl font-bold tracking-tight text-foreground">
                {reportData.totalYieldKg.toLocaleString()} <span className="text-sm font-normal text-muted-foreground">/ {reportData.expectedYieldKg.toLocaleString()} kg</span>
              </p>
            </div>
            <div className="p-3 rounded-xl bg-emerald-100/50 text-emerald-600">
              <TrendingUp className="w-6 h-6" />
            </div>
          </CardContent>
        </Card>

        {/* KPI 2: Expenses */}
        <Card className="border-border shadow-sm">
          <CardContent className="p-5 flex items-center justify-between">
            <div className="space-y-1">
              <p className="text-xs font-medium text-muted-foreground uppercase tracking-wider">Tổng chi phí</p>
              <p className="text-2xl font-bold tracking-tight text-foreground">
                {formatMoney(reportData.totalExpenseVnd, 'VND', 'vi-VN')}
              </p>
            </div>
            <div className="p-3 rounded-xl bg-amber-100/50 text-amber-600">
              <DollarSign className="w-6 h-6" />
            </div>
          </CardContent>
        </Card>

        {/* KPI 3: Orders */}
        <Card className="border-border shadow-sm">
          <CardContent className="p-5 flex items-center justify-between">
            <div className="space-y-1">
              <p className="text-xs font-medium text-muted-foreground uppercase tracking-wider">Đơn hàng chờ</p>
              <p className="text-2xl font-bold tracking-tight text-foreground">
                {pendingOrdersCount} <span className="text-sm font-normal text-muted-foreground">đơn</span>
              </p>
            </div>
            <div className="p-3 rounded-xl bg-blue-100/50 text-blue-600">
              <ShoppingCart className="w-6 h-6" />
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Charts */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Chart 1: Yield Performance */}
        <Card className="border-border shadow-sm flex flex-col">
          <CardHeader className="pb-2 shrink-0">
            <CardTitle className="text-base flex items-center gap-2">
              <TrendingUp className="w-5 h-5 text-emerald-500" />
              Hiệu suất Sản lượng Mùa vụ
              <span className="text-xs font-normal text-muted-foreground ml-auto">(đơn vị: kg)</span>
            </CardTitle>
          </CardHeader>
          <CardContent className="flex-1 min-h-[300px] h-[300px]">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={yieldChartData} margin={{ top: 20, right: 10, left: 10, bottom: 20 }}>
                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="var(--border)" />
                <XAxis dataKey="name" tick={{ fontSize: 12, fill: "var(--muted-foreground)" }} axisLine={false} tickLine={false} />
                <YAxis tick={{ fontSize: 12, fill: "var(--muted-foreground)" }} axisLine={false} tickLine={false} />
                <RechartsTooltip cursor={{ fill: 'rgba(0, 0, 0, 0.05)' }} contentStyle={{ borderRadius: '8px', border: '1px solid var(--border)' }} />
                <Legend verticalAlign="top" height={36} iconType="circle" />
                <Bar dataKey="expected" name="Sản lượng Dự kiến" fill="#a8a29e" radius={[4, 4, 0, 0]} barSize={80} />
                <Bar dataKey="actual" name="Sản lượng Thực tế" fill="#10b981" radius={[4, 4, 0, 0]} barSize={80} />
              </BarChart>
            </ResponsiveContainer>
          </CardContent>
        </Card>

        {/* Chart 2: Expense Breakdown */}
        <Card className="border-border shadow-sm flex flex-col">
          <CardHeader className="pb-2 shrink-0">
            <CardTitle className="text-base flex items-center gap-2">
              <PieChartIcon className="w-5 h-5 text-amber-500" />
              Phân tích Chi phí Mùa vụ
            </CardTitle>
          </CardHeader>
          <CardContent className="flex-1 min-h-[300px] h-[300px]">
            {expenseByCategory.length === 0 ? (
              <div className="h-full flex items-center justify-center text-sm text-muted-foreground">
                Không có dữ liệu chi phí cho mùa vụ này.
              </div>
            ) : (
              <div className="flex h-full items-center">
                <div className="flex-1 h-full relative">
                  <ResponsiveContainer width="100%" height="100%">
                    <PieChart>
                      <Pie
                        data={expenseByCategory}
                        cx="50%"
                        cy="50%"
                        innerRadius={70}
                        outerRadius={95}
                        paddingAngle={2}
                        dataKey="amountVnd"
                        nameKey="category"
                      >
                        {expenseByCategory.map((entry, index) => (
                          <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                        ))}
                      </Pie>
                      <RechartsTooltip formatter={(value: number) => formatMoney(value, 'VND', 'vi-VN')} contentStyle={{ borderRadius: '8px', border: '1px solid var(--border)' }} />
                    </PieChart>
                  </ResponsiveContainer>
                </div>
                <div className="w-[40%] space-y-3 pr-4 overflow-y-auto max-h-[250px]">
                  {expenseByCategory.map((item, index) => (
                    <div key={item.category} className="flex flex-col gap-1 text-xs border-b border-border/50 pb-2">
                      <div className="flex items-center gap-2">
                        <div className="w-2.5 h-2.5 rounded-full shrink-0" style={{ backgroundColor: COLORS[index % COLORS.length] }} />
                        <span className="text-muted-foreground font-medium truncate">{item.category}</span>
                      </div>
                      <span className="font-semibold text-foreground pl-4">{formatMoney(item.amountVnd, 'VND', 'vi-VN')}</span>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
