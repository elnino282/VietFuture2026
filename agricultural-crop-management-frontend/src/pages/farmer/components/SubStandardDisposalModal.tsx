import React, { useState } from "react";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter, DialogDescription } from "@/shared/ui/dialog";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import { Label } from "@/shared/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/shared/ui/select";
import { Textarea } from "@/shared/ui/textarea";
import { useTranslation } from "react-i18next";
import { toast } from "sonner";
import axios from "axios";
import { SUBSTANDARD_DISPOSITION_OPTIONS } from "@/features/farmer/harvests/constants";
import type { SubStandardDisposition } from "@/features/farmer/harvests/types";

export interface SubStandardDisposalModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  lot: any;
  onSuccess: () => void;
}

export function SubStandardDisposalModal({ open, onOpenChange, lot, onSuccess }: SubStandardDisposalModalProps) {
  const { t } = useTranslation();
  const [quantity, setQuantity] = useState<string>("");
  const [disposition, setDisposition] = useState<SubStandardDisposition | "">("");
  const [buyerName, setBuyerName] = useState<string>("");
  const [buyerContact, setBuyerContact] = useState<string>("");
  const [salePricePerKg, setSalePricePerKg] = useState<string>("");
  const [note, setNote] = useState<string>("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  if (!lot) return null;

  const handleSubmit = async () => {
    if (!quantity || !disposition) {
      toast.error("Vui lòng nhập khối lượng và hướng xử lý.");
      return;
    }

    if (Number(quantity) > lot.onHandQuantity) {
      toast.error(`Khối lượng không được vượt quá tồn kho hiện tại (${lot.onHandQuantity} kg).`);
      return;
    }

    setIsSubmitting(true);
    try {
      await axios.post(`/api/v1/product-warehouses/lots/${lot.id}/dispose-substandard`, {
        quantity: Number(quantity),
        disposition,
        buyerName,
        buyerContact,
        salePricePerKg: salePricePerKg ? Number(salePricePerKg) : undefined,
        note,
      });

      toast.success("Xuất kho phụ phẩm thành công!");
      onSuccess();
      onOpenChange(false);
    } catch (error) {
      toast.error("Đã có lỗi xảy ra khi thanh lý.");
      console.error(error);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[500px]">
        <DialogHeader>
          <DialogTitle>Xuất kho Không đạt chuẩn (Thanh lý/Phụ phẩm)</DialogTitle>
          <DialogDescription>
            Lô hàng: <strong>{lot.lotCode}</strong> - Tồn kho: {lot.onHandQuantity} {lot.unit}
          </DialogDescription>
        </DialogHeader>

        <div className="grid gap-4 py-4">
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="quantity" className="text-foreground">Khối lượng (kg) <span className="text-destructive">*</span></Label>
              <Input
                id="quantity"
                type="number"
                min="0.1"
                step="0.1"
                max={lot.onHandQuantity}
                placeholder="VD: 100"
                value={quantity}
                onChange={(e) => setQuantity(e.target.value)}
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="disposition" className="text-foreground">Mục đích <span className="text-destructive">*</span></Label>
              <Select
                value={disposition}
                onValueChange={(value: SubStandardDisposition) => setDisposition(value)}
              >
                <SelectTrigger className="rounded-xl border-border">
                  <SelectValue placeholder="Chọn..." />
                </SelectTrigger>
                <SelectContent>
                  {SUBSTANDARD_DISPOSITION_OPTIONS.map((option) => (
                    <SelectItem key={option.value} value={option.value}>
                      {option.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>

          {(disposition === "SELL_LIVESTOCK_FEED" || disposition === "SELL_DISCOUNT") && (
            <div className="grid grid-cols-2 gap-4 p-3 border border-muted rounded-lg bg-muted/20">
              <div className="space-y-2 col-span-2">
                <Label htmlFor="buyerName" className="text-foreground">Người/Đơn vị mua</Label>
                <Input
                  id="buyerName"
                  placeholder="Nhập tên người mua..."
                  value={buyerName}
                  onChange={(e) => setBuyerName(e.target.value)}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="buyerContact" className="text-foreground">SĐT liên hệ</Label>
                <Input
                  id="buyerContact"
                  placeholder="09..."
                  value={buyerContact}
                  onChange={(e) => setBuyerContact(e.target.value)}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="salePricePerKg" className="text-foreground">Giá bán/kg (VNĐ)</Label>
                <Input
                  id="salePricePerKg"
                  type="number"
                  placeholder="VD: 5000"
                  value={salePricePerKg}
                  onChange={(e) => setSalePricePerKg(e.target.value)}
                />
              </div>
            </div>
          )}

          <div className="space-y-2">
            <Label htmlFor="note">Ghi chú</Label>
            <Textarea
              id="note"
              placeholder="Nhập ghi chú thêm..."
              value={note}
              onChange={(e) => setNote(e.target.value)}
              className="min-h-[80px]"
            />
          </div>
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)} disabled={isSubmitting}>
            Hủy
          </Button>
          <Button onClick={handleSubmit} disabled={isSubmitting || !quantity || !disposition}>
            {isSubmitting ? "Đang xử lý..." : "Xác nhận Xuất kho"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
