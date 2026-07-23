import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { ShieldCheck, ShieldAlert, Droplets, SprayCan, Leaf, Beaker, Package, CheckCircle2, Clock, XCircle, AlertCircle, Loader2 } from 'lucide-react';
import { Card, CardContent } from '@/shared/ui/card';
import { Badge } from '@/shared/ui/badge';
import { dashboardApi } from '../api/dashboardApi';

interface FarmingLogsWidgetProps {
  seasonId: string;
}

export function FarmingLogsWidget({ seasonId }: FarmingLogsWidgetProps) {
  const { data: logs = [], isLoading, error } = useQuery({
    queryKey: ['farmingLogs', seasonId],
    queryFn: () => dashboardApi.getFarmingLogs(seasonId),
    enabled: !!seasonId,
    retry: 1
  });

  if (error) {
    throw error; // Let the ErrorBoundary catch it
  }

  if (isLoading) {
    return (
      <div className="flex flex-col items-center justify-center h-48 text-muted-foreground p-8 space-y-4">
        <Loader2 className="w-8 h-8 animate-spin text-primary" />
        <p>Đang tải dữ liệu nhật ký...</p>
      </div>
    );
  }

  // Sắp xếp log theo thời gian mới nhất
  const sortedLogs = Array.isArray(logs) ? [...logs].sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime()) : [];

  if (sortedLogs.length === 0) {
    return (
      <div className="flex items-center justify-center h-full text-muted-foreground p-8 bg-slate-50/50 rounded-md border border-dashed border-border/50">
        Chưa có nhật ký canh tác nào cho mùa vụ này.
      </div>
    );
  }

  return (
    <div className="relative pl-6 py-4">
      {/* Vertical line for timeline */}
      <div className="absolute left-10 top-6 bottom-4 w-px bg-border/60" />

      <div className="space-y-6">
        {sortedLogs.map((log) => {
          const isVietGapCompliant = log.vietGapCompliant;
          const hasVietGapData = isVietGapCompliant !== undefined;

          // Xác định Icon theo loại hoạt động
          let ActivityIcon = Leaf;
          let iconBg = 'bg-primary/10';
          let iconColor = 'text-primary';

          if (log.activityType === 'FERTILIZER') {
            ActivityIcon = Beaker;
            iconBg = 'bg-amber-100';
            iconColor = 'text-amber-600';
          } else if (log.activityType === 'PESTICIDE') {
            ActivityIcon = SprayCan;
            iconBg = 'bg-rose-100';
            iconColor = 'text-rose-600';
          } else if (log.activityType === 'WATERING') {
            ActivityIcon = Droplets;
            iconBg = 'bg-blue-100';
            iconColor = 'text-blue-600';
          }

          // Xác định Status Icon
          let StatusIcon = AlertCircle;
          let statusColor = 'text-gray-500';
          if (log.status === 'COMPLETED') {
            StatusIcon = CheckCircle2;
            statusColor = 'text-emerald-500';
          } else if (log.status === 'PENDING') {
            StatusIcon = Clock;
            statusColor = 'text-amber-500';
          } else if (log.status === 'CANCELLED') {
            StatusIcon = XCircle;
            statusColor = 'text-rose-500';
          }

          return (
            <div key={log.id} className="relative flex items-start gap-6 group">
              {/* Timeline marker */}
              <div className={`relative z-10 flex h-8 w-8 shrink-0 items-center justify-center rounded-full border-2 border-background shadow-sm ${iconBg}`}>
                <ActivityIcon className={`h-4 w-4 ${iconColor}`} />
              </div>

              {/* Log content */}
              <Card className="flex-1 border-border/60 shadow-sm hover:shadow-md transition-shadow hover:border-primary/40 bg-card">
                <CardContent className="p-4">
                  <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-3 mb-2">
                    <div>
                      <h4 className="font-bold text-foreground text-base line-clamp-2">
                        {log.description}
                      </h4>
                      <p className="text-xs text-muted-foreground mt-1 flex items-center gap-1.5">
                        <StatusIcon className={`w-3.5 h-3.5 ${statusColor}`} />
                        {new Date(log.date).toLocaleString('vi-VN', { hour: '2-digit', minute: '2-digit', day: '2-digit', month: '2-digit', year: 'numeric' })}
                        <span className="mx-1">•</span>
                        👤 {log.performedBy}
                      </p>
                    </div>

                    {/* VietGAP Badge */}
                    {hasVietGapData && (
                      <div className="shrink-0">
                        {isVietGapCompliant ? (
                          <Badge variant="outline" className="bg-emerald-50 text-emerald-700 border-emerald-200 flex items-center gap-1">
                            <ShieldCheck className="w-3.5 h-3.5" />
                            Chuẩn VietGAP
                          </Badge>
                        ) : (
                          <Badge variant="outline" className="bg-rose-50 text-rose-700 border-rose-200 flex items-center gap-1">
                            <ShieldAlert className="w-3.5 h-3.5" />
                            Cảnh báo VietGAP
                          </Badge>
                        )}
                      </div>
                    )}
                  </div>

                  {/* Details section */}
                  <div className="mt-3 bg-muted/30 rounded-md p-3 text-sm space-y-2">
                    {log.materialName && (
                      <div className="flex items-start gap-2">
                        <span className="font-medium text-foreground min-w-[90px]">Vật tư:</span>
                        <span className="text-muted-foreground">{log.materialName}</span>
                      </div>
                    )}
                    
                    {log.amount && log.unit && (
                      <div className="flex items-start gap-2">
                        <span className="font-medium text-foreground min-w-[90px]">Liều lượng:</span>
                        <span className="text-muted-foreground">{log.amount} {log.unit}</span>
                      </div>
                    )}

                    {log.quarantineDays !== undefined && (
                      <div className="flex items-start gap-2">
                        <span className="font-medium text-foreground min-w-[90px]">Cách ly:</span>
                        <span className="text-muted-foreground">{log.quarantineDays} ngày</span>
                      </div>
                    )}

                    {log.soilCondition && (
                      <div className="flex items-start gap-2">
                        <span className="font-medium text-foreground min-w-[90px]">Tình trạng đất:</span>
                        <span className="text-muted-foreground">{log.soilCondition}</span>
                      </div>
                    )}

                    {log.notes && (
                      <div className="flex items-start gap-2">
                        <span className="font-medium text-foreground min-w-[90px]">Ghi chú:</span>
                        <span className="text-muted-foreground">{log.notes}</span>
                      </div>
                    )}
                  </div>

                  {/* VietGAP Violation Reason */}
                  {hasVietGapData && !isVietGapCompliant && log.vietGapReason && (
                    <div className="mt-3 text-sm text-rose-600 bg-rose-50/50 p-2.5 rounded border border-rose-100 flex items-start gap-2">
                      <AlertCircle className="w-4 h-4 mt-0.5 shrink-0" />
                      <p>{log.vietGapReason}</p>
                    </div>
                  )}
                </CardContent>
              </Card>
            </div>
          );
        })}
      </div>
    </div>
  );
}
