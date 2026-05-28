import { Button } from "@/shared/ui";
import { AdminHeaderCard } from "@/features/admin/shared/ui";
import { Download, RefreshCw } from "lucide-react";

interface ReportsHeaderProps {
  onRefresh: () => void;
  onExport: () => void;
  isLoading?: boolean;
  isExporting?: boolean;
  lastUpdated?: string;
}

export const ReportsHeader: React.FC<ReportsHeaderProps> = ({
  onRefresh,
  onExport,
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
    <AdminHeaderCard
      title="Reports"
      description={`Analyze yield, costs, and revenue. Last updated ${formattedDate}`}
      actions={
        <>
          <Button
            size="sm"
            onClick={onExport}
            disabled={isExporting}
            className="h-8 w-full rounded-[14px] px-3 font-medium sm:w-auto"
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
            className="h-9 w-full rounded-[14px] hover:bg-muted sm:w-9"
          >
            <RefreshCw
              className={`w-4 h-4 text-muted-foreground ${isLoading ? "animate-spin" : ""}`}
            />
          </Button>
        </>
      }
    />
  );
};
