import React, { useState } from "react";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter, DialogDescription } from "@/shared/ui/dialog";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import { Label } from "@/shared/ui/label";
import { useTranslation } from "react-i18next";
import { toast } from "sonner";
import axios from "axios";

export interface ReceiveToWarehouseModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  lot: any;
  onSuccess: () => void;
}

export function ReceiveToWarehouseModal({ open, onOpenChange, lot, onSuccess }: ReceiveToWarehouseModalProps) {
  const { t } = useTranslation();
  const [currentMoisture, setCurrentMoisture] = useState<string>("");
  const [targetMoisture, setTargetMoisture] = useState<string>("");
  const [mechanicalLoss, setMechanicalLoss] = useState<string>("0");
  const [isSubmitting, setIsSubmitting] = useState(false);

  if (!lot) return null;

  const isGrain = lot.cropCategory === "GRAIN";

  const handleSubmit = async () => {
    if (isGrain) {
      if (!currentMoisture || !targetMoisture) {
        toast.error("Vui lòng nhập đầy đủ thông tin độ ẩm cho ngũ cốc.");
        return;
      }
    }

    setIsSubmitting(true);
    try {
      // Gọi API Nhập kho
      await axios.post(`/api/v1/product-warehouse-lots/${lot.id}/receive-to-warehouse`, {
        currentMoisture: currentMoisture ? Number(currentMoisture) : null,
        targetMoisture: targetMoisture ? Number(targetMoisture) : null,
        mechanicalLoss: mechanicalLoss ? Number(mechanicalLoss) : 0,
      });

      toast.success("Xác nhận nhập kho thành công!");
      onSuccess();
      onOpenChange(false);
    } catch (error) {
      toast.error("Đã có lỗi xảy ra khi nhập kho.");
      console.error(error);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[450px]">
        <DialogHeader>
          <DialogTitle>Xác nhận Nhập kho chuyên biệt</DialogTitle>
          <DialogDescription>
            Nhập kho cho lô <strong>{lot.lotCode}</strong> ({lot.productName})
          </DialogDescription>
        </DialogHeader>

        <div className="grid gap-4 py-4">
          {isGrain && (
            <>
              <div className="space-y-2">
                <Label htmlFor="currentMoisture" required>Độ ẩm hiện tại (%)</Label>
                <Input
                  id="currentMoisture"
                  type="number"
                  step="0.1"
                  min="0"
                  max="100"
                  placeholder="Ví dụ: 25"
                  value={currentMoisture}
                  onChange={(e) => setCurrentMoisture(e.target.value)}
                />
                <p className="text-xs text-muted-foreground">
                  Hệ thống sẽ tự tính toán hao hụt sấy dựa trên độ ẩm mục tiêu.
                </p>
              </div>

              <div className="space-y-2">
                <Label htmlFor="targetMoisture" required>Độ ẩm mục tiêu (%)</Label>
                <Input
                  id="targetMoisture"
                  type="number"
                  step="0.1"
                  min="0"
                  max="100"
                  placeholder="Ví dụ: 14"
                  value={targetMoisture}
                  onChange={(e) => setTargetMoisture(e.target.value)}
                />
              </div>
            </>
          )}

          <div className="space-y-2">
            <Label htmlFor="mechanicalLoss">Thất thoát cơ học (kg) - Nếu có</Label>
            <Input
              id="mechanicalLoss"
              type="number"
              step="0.1"
              min="0"
              placeholder="0"
              value={mechanicalLoss}
              onChange={(e) => setMechanicalLoss(e.target.value)}
            />
          </div>
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)} disabled={isSubmitting}>
            Hủy
          </Button>
          <Button onClick={handleSubmit} disabled={isSubmitting}>
            {isSubmitting ? "Đang xử lý..." : "Xác nhận Nhập kho"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
