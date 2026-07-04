import React, { useState, useEffect } from "react";
import { Button } from "@/shared/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/shared/ui/card";
import { Plus, Users } from "lucide-react";
import { CreateWorkTeamDialog } from "./CreateWorkTeamDialog";
import httpClient from "@/shared/api/http";
import { toast } from "sonner";
import { useTranslation } from "react-i18next";

interface WorkTeam {
  id: number;
  seasonId: number;
  teamName: string;
  teamLeaderUserId: number;
  leaderName?: string;
  memberCount?: number;
  members?: any[];
}

interface WorkTeamPanelProps {
  seasonId: number;
}

export function WorkTeamPanel({ seasonId }: WorkTeamPanelProps) {
  const { t } = useTranslation();
  const [teams, setTeams] = useState<WorkTeam[]>([]);
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [isLoading, setIsLoading] = useState(false);

  const fetchTeams = async () => {
    if (!seasonId) return;
    setIsLoading(true);
    try {
      // Gọi API lấy danh sách team theo seasonId
      const response = await httpClient.get(`/api/v1/farmer/seasons/${seasonId}/teams`);
      if (response.data) {
        // Backend trả về mảng trực tiếp
        setTeams(response.data);
      }
    } catch (error) {
      toast.error(t("workTeams.fetchError", "Lỗi tải danh sách đội nhóm"));
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchTeams();
  }, [seasonId]);

  return (
    <Card className="shadow-sm border-border">
      <CardHeader className="flex flex-row items-center justify-between pb-2">
        <CardTitle className="text-lg font-semibold flex items-center gap-2">
          <Users className="w-5 h-5 text-primary" />
          {t("workTeams.panelTitle", "Quản lý Đội nhóm")}
        </CardTitle>
        <Button size="sm" onClick={() => setIsDialogOpen(true)} className="flex items-center gap-1">
          <Plus className="w-4 h-4" />
          {t("workTeams.createTeam", "Tạo Đội")}
        </Button>
      </CardHeader>
      <CardContent>
        {isLoading ? (
          <div className="py-8 text-center text-muted-foreground">{t("common.loading", "Đang tải...")}</div>
        ) : teams.length === 0 ? (
          <div className="py-8 text-center text-muted-foreground bg-muted/20 rounded-md border border-dashed">
            {t("workTeams.emptyState", "Chưa có đội nhóm nào được phân công cho mùa vụ này.")}
          </div>
        ) : (
          <div className="space-y-3 mt-4">
            {teams.map((team) => (
              <div
                key={team.id}
                className="flex items-center justify-between p-3 rounded-md border border-border hover:bg-muted/30 transition-colors"
              >
                <div>
                  <h4 className="font-medium text-foreground">{team.teamName}</h4>
                  <p className="text-xs text-muted-foreground mt-1">
                    {t("workTeams.leader", "Đội trưởng")}: {team.leaderName || `User #${team.teamLeaderUserId}`}
                  </p>
                </div>
                <div className="flex items-center gap-2 text-sm text-muted-foreground">
                  <Users className="w-4 h-4" />
                  <span>{(team.members && team.members.length) || team.memberCount || 0} {t("workTeams.members", "Thành viên")}</span>
                </div>
              </div>
            ))}
          </div>
        )}
      </CardContent>

      <CreateWorkTeamDialog 
        open={isDialogOpen} 
        onOpenChange={setIsDialogOpen} 
        seasonId={seasonId}
        onSuccess={fetchTeams}
      />
    </Card>
  );
}
