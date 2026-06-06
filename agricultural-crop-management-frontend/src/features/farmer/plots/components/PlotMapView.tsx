import { ZoomIn, ZoomOut, Maximize, Layers, MapIcon, QrCode } from "lucide-react";
import { Button } from "@/shared/ui/button";
import { Card, CardContent } from "@/shared/ui/card";
import { useTranslation } from "react-i18next";
import { Plot } from "../types";
import { getPolygonColor } from "../utils";

interface PlotMapViewProps {
  plots: Plot[];
  onViewDetails: (plot: Plot) => void;
  onGenerateQR: (plot: Plot) => void;
}

/**
 * PlotMapView Component
 *
 * Displays plots on a map visualization with controls and legend.
 */
export function PlotMapView({ plots, onViewDetails, onGenerateQR }: PlotMapViewProps) {
  const { t } = useTranslation();

  return (
    <Card className="border-none shadow-md">
      <CardContent className="p-0">
        <div className="relative h-[600px] bg-muted rounded-lg overflow-hidden">
          {/* Map Controls */}
          <div className="absolute top-4 right-4 z-10 flex flex-col gap-2">
            <Button variant="outline" size="icon" className="bg-card shadow-lg" aria-label={t("plots.map.zoomIn")}>
              <ZoomIn className="w-4 h-4" />
            </Button>
            <Button variant="outline" size="icon" className="bg-card shadow-lg" aria-label={t("plots.map.zoomOut")}>
              <ZoomOut className="w-4 h-4" />
            </Button>
            <Button variant="outline" size="icon" className="bg-card shadow-lg" aria-label={t("plots.map.fullscreen")}>
              <Maximize className="w-4 h-4" />
            </Button>
            <Button variant="outline" size="icon" className="bg-card shadow-lg" aria-label={t("plots.map.layers")}>
              <Layers className="w-4 h-4" />
            </Button>
          </div>

          {/* Map Legend */}
          <div className="absolute top-4 left-4 z-10 bg-card rounded-lg shadow-lg p-4">
            <h4 className="text-sm text-foreground mb-3">{t("plots.map.legendTitle")}</h4>
            <div className="space-y-2">
              <div className="flex items-center gap-2">
                <div className="w-4 h-4 bg-primary rounded"></div>
                <span className="text-xs text-muted-foreground">{t("plots.status.active")}</span>
              </div>
              <div className="flex items-center gap-2">
                <div className="w-4 h-4 bg-accent rounded"></div>
                <span className="text-xs text-muted-foreground">{t("plots.status.dormant")}</span>
              </div>
              <div className="flex items-center gap-2">
                <div className="w-4 h-4 bg-secondary rounded"></div>
                <span className="text-xs text-muted-foreground">{t("plots.status.planned")}</span>
              </div>
            </div>
          </div>

          {/* Mock Map - Visual Representation */}
          <div className="absolute inset-0 flex items-center justify-center">
            <div className="text-center space-y-4">
              <MapIcon className="w-24 h-24 text-muted-foreground mx-auto opacity-30" />
              <p className="text-muted-foreground">{t("plots.map.title")}</p>
              <p className="text-sm text-muted-foreground max-w-md">
                {t("plots.map.description")}
              </p>
            </div>
          </div>

          {/* Plot Markers/Polygons Grid */}
          <div className="absolute inset-0 p-12">
            <div className="grid grid-cols-3 gap-8 h-full">
              {plots.slice(0, 6).map((plot) => (
                <div
                  key={plot.id}
                  className="relative cursor-pointer group"
                  onClick={() => onViewDetails(plot)}
                >
                  <div
                    className="w-full h-full rounded-lg border-4 opacity-50 group-hover:opacity-100 transition-all duration-200 group-hover:scale-105"
                    style={{
                      borderColor: getPolygonColor(plot.status),
                      backgroundColor: `color-mix(in oklab, ${getPolygonColor(plot.status)} 20%, transparent)`,
                    }}
                  >
                    <div className="absolute inset-0 flex items-center justify-center">
                      <div
                        className="backdrop-blur-sm rounded-lg p-3 shadow-lg opacity-0 group-hover:opacity-100 transition-opacity"
                        style={{
                          backgroundColor: "color-mix(in oklab, var(--card) 90%, transparent)",
                        }}
                      >
                        <p className="text-sm text-foreground">{plot.name}</p>
                        <p className="text-xs text-muted-foreground">
                          {plot.area.toFixed(1)} ha
                        </p>
                        <p className="text-xs text-muted-foreground">
                          {plot.crop || t("plots.noCrop")}
                        </p>
                        <div className="mt-2 pt-2 border-t border-border flex gap-2">
                          <Button
                            size="sm"
                            className="h-7 text-xs bg-primary hover:bg-primary/90 text-white"
                            onClick={(e) => {
                              e.stopPropagation();
                              onViewDetails(plot);
                            }}
                          >
                            {t("plots.actions.viewDetail")}
                          </Button>
                          <Button
                            size="sm"
                            variant="outline"
                            className="h-7 text-xs"
                            onClick={(e) => {
                              e.stopPropagation();
                              onGenerateQR(plot);
                            }}
                          >
                            <QrCode className="w-3 h-3" />
                            <span className="sr-only">{t("plots.detail.generatePrintQR")}</span>
                          </Button>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}




