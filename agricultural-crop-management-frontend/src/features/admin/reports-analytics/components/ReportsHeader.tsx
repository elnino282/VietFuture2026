import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Download, RefreshCw, Save } from "lucide-react";

interface ReportsHeaderProps {
  onRefresh: () => void;
  onExport: () => void;
  onSaveView?: () => void;
  isLoading?: boolean;
  isExporting?: boolean;
  lastUpdated?: string;
}

export const ReportsHeader: React.FC<ReportsHeaderProps> = ({
  onRefresh,
  onExport,
  onSaveView,
  isLoading,
  isExporting,
  lastUpdated,
}) => {
  // Format current date for display
  const formattedDate =
    lastUpdated ||
    new Date().toLocaleString("en-US", {
      month: "short",
      day: "numeric",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit",
      hour12: true,
    });

  return (
    <Card className="!rounded-[18px] border-border bg-card shadow-sm mb-6">
      <CardContent className="p-4">
        <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
          {/* Left Section - Title & Description */}
          <div className="flex flex-col gap-0.5">
            <h1 className="text-2xl font-bold mb-1 text-foreground">
              {" "}
              <b>Reports</b>
            </h1>
            <p className="text-muted-foreground">
              Analyze yield, costs, and revenue. Last updated {formattedDate}
            </p>
          </div>

          {/* Right Section - Action Buttons */}
          <div className="flex flex-col sm:flex-row sm:items-center gap-2 w-full md:w-auto">
            {onSaveView && (
              <Button
                variant="outline"
                size="sm"
                onClick={onSaveView}
                className="w-full sm:w-auto h-8 px-3 rounded-[14px] border-border bg-muted hover:bg-muted/80 text-foreground font-medium text-sm"
              >
                <Save className="w-4 h-4 mr-2" />
                Save View
              </Button>
            )}
            <Button
              size="sm"
              onClick={onExport}
              disabled={isExporting}
              className="w-full sm:w-auto h-8 px-3 rounded-[14px] bg-primary hover:bg-primary/90 text-primary-foreground font-medium text-sm"
            >
              <Download
                className={`w-4 h-4 mr-2 ${isExporting ? "animate-pulse" : ""}`}
              />
              {isExporting ? "Exporting..." : "Export CSV"}
            </Button>
            <Button
              variant="ghost"
              size="icon"
              onClick={onRefresh}
              disabled={isLoading}
              className="h-9 w-full sm:w-9 rounded-[14px] hover:bg-muted"
            >
              <RefreshCw
                className={`w-4 h-4 text-muted-foreground ${isLoading ? "animate-spin" : ""}`}
              />
            </Button>
          </div>
        </div>
      </CardContent>
    </Card>
  );
};
