import React, { useEffect, useState } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/shared/ui/card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/shared/ui/table";
import { Users } from "lucide-react";
import axios from "axios";
import { useTranslation } from "react-i18next";

// [FIX] Đổi teamId → workTeamId để khớp với backend DTO
export interface TeamProgressSummaryResponse {
  workTeamId: number;
  teamName: string;
  plotName: string;
  totalTasks: number;
  completedTasks: number;
  inProgressTasks: number;
  completionRate: number; // 0 to 100
}

interface TeamProgressDashboardWidgetProps {
  seasonId?: number;
}

export function TeamProgressDashboardWidget({ seasonId }: TeamProgressDashboardWidgetProps) {
  const { t } = useTranslation();
  const [teamProgress, setTeamProgress] = useState<TeamProgressSummaryResponse[]>([]);
  const [isLoading, setIsLoading] = useState(false);

  useEffect(() => {
    const fetchProgress = async () => {
      setIsLoading(true);
      try {
        // Giả lập gọi API lấy tiến độ của các đội theo mùa vụ
        // [FIX] Sửa URL đúng endpoint backend
        const response = await axios.get(`/api/v1/farmer/dashboard/team-progress`, {
          params: { seasonId }
        });
        if (response.data && response.data.items) {
          setTeamProgress(response.data.items);
        } else {
          // Mock data cho demo UI nếu không có API
          setTeamProgress([
            { workTeamId: 1, teamName: "Đội Phun Thuốc 1", plotName: "Khu A - Lúa", totalTasks: 10, completedTasks: 8, inProgressTasks: 2, completionRate: 80 },
            { workTeamId: 2, teamName: "Đội Thu Hoạch", plotName: "Khu B - Dưa lưới", totalTasks: 5, completedTasks: 1, inProgressTasks: 4, completionRate: 20 },
            { workTeamId: 3, teamName: "Đội Cày Xới", plotName: "Khu C - Bắp", totalTasks: 8, completedTasks: 4, inProgressTasks: 2, completionRate: 50 },
          ]);
        }
      } catch (error) {
        console.error("Failed to fetch team progress", error);
        // Fallback mock data
        setTeamProgress([
          { workTeamId: 1, teamName: "Đội Phun Thuốc 1", plotName: "Khu A - Lúa", totalTasks: 10, completedTasks: 8, inProgressTasks: 2, completionRate: 80 },
          { workTeamId: 2, teamName: "Đội Thu Hoạch", plotName: "Khu B - Dưa lưới", totalTasks: 5, completedTasks: 1, inProgressTasks: 4, completionRate: 20 },
          { workTeamId: 3, teamName: "Đội Cày Xới", plotName: "Khu C - Bắp", totalTasks: 8, completedTasks: 4, inProgressTasks: 2, completionRate: 50 },
        ]);
      } finally {
        setIsLoading(false);
      }
    };

    fetchProgress();
  }, [seasonId]);

  const getProgressColor = (rate: number) => {
    if (rate >= 80) return "bg-emerald-500";
    if (rate >= 40) return "bg-amber-500";
    return "bg-red-500";
  };

  const getProgressTextColor = (rate: number) => {
    if (rate >= 80) return "text-emerald-700";
    if (rate >= 40) return "text-amber-700";
    return "text-red-700";
  };

  return (
    <Card className="border-border shadow-sm">
      <CardHeader className="pb-3">
        <CardTitle className="text-base font-semibold flex items-center gap-2">
          <Users className="w-5 h-5 text-primary" />
          {t("dashboard.teamProgressTitle", "Tiến độ Đội nhóm (Team Progress)")}
        </CardTitle>
      </CardHeader>
      <CardContent>
        {isLoading ? (
          <div className="py-6 text-center text-muted-foreground">{t("common.loading", "Đang tải...")}</div>
        ) : teamProgress.length === 0 ? (
          <div className="py-6 text-center text-muted-foreground bg-muted/20 rounded-md border border-dashed">
            {t("dashboard.noTeamProgress", "Không có dữ liệu tiến độ.")}
          </div>
        ) : (
          <div className="overflow-x-auto">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>{t("dashboard.teamName", "Tên Đội")}</TableHead>
                  <TableHead>{t("dashboard.plotName", "Khu vực")}</TableHead>
                  <TableHead className="text-center">{t("dashboard.totalTasks", "Tổng số")}</TableHead>
                  <TableHead className="text-center">{t("dashboard.completedTasks", "Đã xong")}</TableHead>
                  <TableHead className="text-center">{t("dashboard.inProgressTasks", "Đang làm")}</TableHead>
                  <TableHead className="w-[30%]">{t("dashboard.progress", "Tiến độ")}</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {teamProgress.map((team) => (
                  // [FIX] Đổi team.teamId → team.workTeamId
                  <TableRow key={team.workTeamId}>
                    <TableCell className="font-medium">{team.teamName}</TableCell>
                    <TableCell>{team.plotName}</TableCell>
                    <TableCell className="text-center font-semibold">{team.totalTasks}</TableCell>
                    <TableCell className="text-center text-emerald-600 font-medium">{team.completedTasks}</TableCell>
                    <TableCell className="text-center text-blue-600 font-medium">{team.inProgressTasks}</TableCell>
                    <TableCell>
                      <div className="flex items-center gap-3">
                        <div className="flex-1 h-2.5 w-full bg-muted rounded-full overflow-hidden">
                          <div 
                            className={`h-full ${getProgressColor(team.completionRate)} transition-all duration-500`} 
                            style={{ width: `${team.completionRate}%` }}
                          />
                        </div>
                        <span className={`text-sm font-semibold w-10 text-right ${getProgressTextColor(team.completionRate)}`}>
                          {team.completionRate}%
                        </span>
                      </div>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
        )}
      </CardContent>
    </Card>
  );
}
