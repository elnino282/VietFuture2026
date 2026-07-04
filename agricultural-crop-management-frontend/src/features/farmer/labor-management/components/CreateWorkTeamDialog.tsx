import React, { useState, useEffect } from "react";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import { Label } from "@/shared/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/shared/ui/select";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/shared/ui";
import { toast } from "sonner";
import httpClient from "@/shared/api/http";
import { useTranslation } from "react-i18next";
import { useEmployeeDirectory } from "@/entities/labor";

interface CreateWorkTeamDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  seasonId: number;
  onSuccess: () => void;
}

interface Employee {
  userId: number;
  displayName: string;
}

export function CreateWorkTeamDialog({ open, onOpenChange, seasonId, onSuccess }: CreateWorkTeamDialogProps) {
  const { t } = useTranslation();
  const [teamName, setTeamName] = useState("");
  const [leaderId, setLeaderId] = useState("");
  const [memberIds, setMemberIds] = useState<number[]>([]);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [employees, setEmployees] = useState<Employee[]>([]);

  const { data: employeeDirectoryData } = useEmployeeDirectory(
    { page: 0, size: 200 },
    { enabled: open }
  );

  useEffect(() => {
    if (employeeDirectoryData?.items) {
      setEmployees(
        employeeDirectoryData.items.map(emp => ({
          userId: emp.userId,
          displayName: emp.fullName || emp.username || emp.email || `User #${emp.userId}`
        }))
      );
    } else if (!open) {
      setEmployees([]);
    }
  }, [employeeDirectoryData, open]);

  const handleSubmit = async () => {
    if (!teamName.trim()) {
      toast.error(t("workTeams.validation.nameRequired", "Tên đội không được để trống"));
      return;
    }
    if (!leaderId) {
      toast.error(t("workTeams.validation.leaderRequired", "Vui lòng chọn đội trưởng"));
      return;
    }

    setIsSubmitting(true);
    try {
      await httpClient.post(`/api/v1/farmer/seasons/${seasonId}/teams`, memberIds, {
        params: {
          teamName,
          leaderId: Number(leaderId)
        }
      });
      toast.success(t("workTeams.createSuccess", "Tạo đội nhóm thành công!"));
      onSuccess();
      onOpenChange(false);
      
      // Reset form
      setTeamName("");
      setLeaderId("");
      setMemberIds([]);
    } catch (error) {
      toast.error(t("workTeams.createError", "Lỗi khi tạo đội nhóm"));
    } finally {
      setIsSubmitting(false);
    }
  };

  const toggleMember = (userId: number) => {
    setMemberIds(prev => 
      prev.includes(userId) ? prev.filter(id => id !== userId) : [...prev, userId]
    );
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[500px]">
        <DialogHeader>
          <DialogTitle>{t("workTeams.dialog.title", "Tạo Đội Nhóm Mới")}</DialogTitle>
          <DialogDescription>
            {t("workTeams.dialog.description", "Nhập thông tin để tạo đội và phân công nhân sự.")}
          </DialogDescription>
        </DialogHeader>
        
        <div className="grid gap-4 py-4">
          <div className="space-y-2">
            <Label htmlFor="teamName" required>{t("workTeams.form.name", "Tên Đội")}</Label>
            <Input 
              id="teamName" 
              placeholder={t("workTeams.form.namePlaceholder", "VD: Đội Phun Thuốc 1")} 
              value={teamName}
              onChange={e => setTeamName(e.target.value)}
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="leader" required>{t("workTeams.form.leader", "Đội Trưởng")}</Label>
            <Select value={leaderId} onValueChange={setLeaderId}>
              <SelectTrigger id="leader">
                <SelectValue placeholder={t("workTeams.form.selectLeader", "Chọn Đội Trưởng")} />
              </SelectTrigger>
              <SelectContent>
                {employees.map(emp => (
                  <SelectItem key={`leader-${emp.userId}`} value={String(emp.userId)}>
                    {emp.displayName}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="space-y-2">
            <Label>{t("workTeams.form.members", "Thành Viên (Tùy chọn)")}</Label>
            <div className="border border-border rounded-md p-2 h-[150px] overflow-y-auto space-y-1 bg-muted/20">
              {employees.filter(emp => String(emp.userId) !== leaderId).map(emp => (
                <div 
                  key={`member-${emp.userId}`} 
                  className={`flex items-center px-3 py-2 rounded-md cursor-pointer text-sm transition-colors ${
                    memberIds.includes(emp.userId) ? 'bg-primary/10 text-primary font-medium border border-primary/20' : 'hover:bg-muted'
                  }`}
                  onClick={() => toggleMember(emp.userId)}
                >
                  <input 
                    type="checkbox" 
                    checked={memberIds.includes(emp.userId)} 
                    readOnly 
                    className="mr-3"
                  />
                  {emp.displayName}
                </div>
              ))}
            </div>
            <p className="text-xs text-muted-foreground">
              {t("workTeams.form.membersHint", "Nhấp vào nhân sự để thêm/bớt khỏi đội.")}
            </p>
          </div>
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)} disabled={isSubmitting}>
            {t("common.cancel", "Hủy")}
          </Button>
          <Button onClick={handleSubmit} disabled={isSubmitting}>
            {isSubmitting ? t("common.saving", "Đang lưu...") : t("common.save", "Tạo Đội")}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
