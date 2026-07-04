import React, { useState, useEffect } from "react";
import { useTranslation } from "react-i18next";
import { toast } from "sonner";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/shared/ui/card";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import { Progress } from "@/shared/ui/progress";
import { Label } from "@/shared/ui/label";
import { Plot } from "../types";
import apiClient from "@/shared/api/http";
import { Map, Layers, Plus, Trash2 } from "lucide-react";

interface SubZone {
  id: string;
  name: string;
  area: number;
}

interface PlotMapViewProps {
  plots: Plot[];
  onViewDetails: (plot: Plot) => void;
  onGenerateQR: (plot: Plot) => void;
  selectedPlot?: Plot | null;
  onSaveSuccess?: () => void;
}

export function PlotMapView({ plots, selectedPlot, onSaveSuccess }: PlotMapViewProps) {
  const { t } = useTranslation();
  const [subZones, setSubZones] = useState<SubZone[]>([]);
  const [newZoneName, setNewZoneName] = useState("");
  const [newZoneArea, setNewZoneArea] = useState<number | "">("");

  // Fetch existing sub-zones for the selected plot
  useEffect(() => {
    setSubZones([]);
    // In a real app, you would fetch existing sub-zones here:
    // const fetchSubZones = async () => { ... }
  }, [selectedPlot]);

  if (!selectedPlot) {
    return (
      <Card className="border-none shadow-md h-[600px] flex items-center justify-center bg-muted/30">
        <div className="text-center text-muted-foreground flex flex-col items-center gap-4">
          <Map className="w-16 h-16 opacity-20" />
          <p>{t("plots.allocation.selectRequired", "Vui lòng chọn một Thửa ruộng (Plot) từ danh sách để phân bổ khu vực.")}</p>
        </div>
      </Card>
    );
  }

  const parentArea = selectedPlot.area || 10000;
  const usedArea = subZones.reduce((sum, zone) => sum + zone.area, 0);
  const remainingArea = parentArea - usedArea;
  const progressPercentage = Math.min((usedArea / parentArea) * 100, 100);

  const handleAddSubZone = async () => {
    if (!newZoneName.trim()) {
      toast.error(t("plots.allocation.nameRequired", "Vui lòng nhập tên khu vực."));
      return;
    }
    const areaVal = Number(newZoneArea);
    if (!areaVal || areaVal <= 0) {
      toast.error(t("plots.allocation.areaInvalid", "Diện tích không hợp lệ."));
      return;
    }

    if (areaVal > remainingArea) {
      toast.error(t("plots.allocation.areaExceeded", {
        defaultValue: "Không thể lưu! Bạn chỉ còn {{area}} m² đất trống.",
        area: remainingArea.toLocaleString()
      }));
      return;
    }

    try {
      // Simulate backend call
      await apiClient.post("/api/v1/plots", {
        plotName: newZoneName,
        area: areaVal,
        parentPlotId: selectedPlot.id,
        farmId: selectedPlot.farmId,
      });

      const newZone: SubZone = {
        id: Math.random().toString(36).substring(2, 9),
        name: newZoneName,
        area: areaVal,
      };
      setSubZones([...subZones, newZone]);
      setNewZoneName("");
      setNewZoneArea("");

      toast.success(t("plots.allocation.saveSuccess", {
        defaultValue: "Đã phân bổ {{area}} m² cho {{name}}",
        area: areaVal.toLocaleString(),
        name: newZoneName
      }));
      
      onSaveSuccess?.();
    } catch (error: any) {
      toast.error(error?.response?.data?.message || t("plots.saveError", "Lỗi máy chủ khi lưu khu vực."));
    }
  };

  const handleRemoveSubZone = (id: string) => {
    // In a real app, you would call API DELETE here
    setSubZones(subZones.filter(z => z.id !== id));
    toast.info(t("plots.allocation.deleteSuccess", "Đã xóa khu vực."));
  };

  return (
    <Card className="border-none shadow-md h-full min-h-[600px]">
      <CardHeader className="border-b bg-muted/10">
        <div className="flex items-center gap-3">
          <div className="p-2 bg-primary/10 rounded-lg text-primary">
            <Layers className="w-6 h-6" />
          </div>
          <div>
            <CardTitle>{t("plots.allocation.title", "Phân Bổ Khu Vực (Area Allocation)")}</CardTitle>
            <CardDescription>
              {t("plots.allocation.parentPlot", "Thửa ruộng cha")}: <strong>{selectedPlot.name}</strong> - {t("plots.allocation.totalArea", "Tổng diện tích")}: <strong>{parentArea.toLocaleString()} m²</strong>
            </CardDescription>
          </div>
        </div>
      </CardHeader>
      <CardContent className="p-6 grid grid-cols-1 md:grid-cols-2 gap-8">
        
        {/* Left Side: Progress & List */}
        <div className="space-y-6">
          <div className="space-y-2">
            <div className="flex justify-between text-sm font-medium">
              <span className="text-primary">
                {t("plots.allocation.usedArea", "Đã phân bổ")}: {usedArea.toLocaleString()} m² ({progressPercentage.toFixed(1)}%)
              </span>
              <span className="text-muted-foreground">
                {t("plots.allocation.remainingArea", "Còn trống")}: {remainingArea.toLocaleString()} m²
              </span>
            </div>
            <Progress value={progressPercentage} className="h-4 rounded-full" />
          </div>

          <div className="bg-muted/30 rounded-xl p-4 border min-h-[300px]">
            <h3 className="font-semibold text-sm text-muted-foreground mb-4 uppercase tracking-wider">
              {t("plots.allocation.listTitle", "Danh sách khu vực")} ({subZones.length})
            </h3>
            {subZones.length === 0 ? (
              <div className="text-center text-muted-foreground text-sm py-12 italic">
                {t("plots.allocation.emptyList", "Chưa có khu vực nào được phân bổ.")}
              </div>
            ) : (
              <ul className="space-y-3">
                {subZones.map((zone) => (
                  <li key={zone.id} className="flex items-center justify-between bg-background p-3 rounded-lg border shadow-sm group">
                    <div>
                      <p className="font-medium">{zone.name}</p>
                      <p className="text-xs text-muted-foreground">{zone.area.toLocaleString()} m²</p>
                    </div>
                    <Button variant="ghost" size="icon" className="text-destructive opacity-0 group-hover:opacity-100 transition-opacity" onClick={() => handleRemoveSubZone(zone.id)}>
                      <Trash2 className="w-4 h-4" />
                    </Button>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </div>

        {/* Right Side: Add Form */}
        <div>
          <Card className="border-primary/20 shadow-sm">
            <CardHeader>
              <CardTitle className="text-lg">{t("plots.allocation.addTitle", "Thêm Khu Vực Mới")}</CardTitle>
              <CardDescription>{t("plots.allocation.addDesc", "Nhập thông tin để phân bổ diện tích từ thửa ruộng cha.")}</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="zoneName">{t("plots.allocation.zoneName", "Tên khu vực")}</Label>
                <Input 
                  id="zoneName" 
                  placeholder={t("plots.allocation.zoneNamePlaceholder", "VD: Khu trồng ngô, Khu phía Đông...")} 
                  value={newZoneName}
                  onChange={(e) => setNewZoneName(e.target.value)}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="zoneArea">{t("plots.allocation.zoneArea", "Diện tích (m²)")}</Label>
                <div className="relative">
                  <Input 
                    id="zoneArea" 
                    type="number"
                    min={1}
                    max={remainingArea}
                    placeholder="VD: 2000" 
                    value={newZoneArea}
                    onChange={(e) => setNewZoneArea(e.target.value ? Number(e.target.value) : "")}
                  />
                  <span className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground text-sm">m²</span>
                </div>
                {Number(newZoneArea) > remainingArea && (
                  <p className="text-xs text-destructive mt-1">
                    {t("plots.allocation.areaError", {
                      defaultValue: "Diện tích vượt quá số lượng trống ({{area}} m²)",
                      area: remainingArea.toLocaleString()
                    })}
                  </p>
                )}
              </div>
              <Button 
                className="w-full mt-4 gap-2" 
                onClick={handleAddSubZone}
                disabled={Number(newZoneArea) > remainingArea || !newZoneName.trim() || !newZoneArea}
              >
                <Plus className="w-4 h-4" />
                {t("plots.allocation.saveBtn", "Lưu Khu Vực")}
              </Button>
            </CardContent>
          </Card>
        </div>
      </CardContent>
    </Card>
  );
}
