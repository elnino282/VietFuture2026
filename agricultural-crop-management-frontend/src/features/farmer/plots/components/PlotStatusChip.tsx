import { CheckCircle2, Clock, Moon, AlertTriangle } from "lucide-react";
import { Badge } from "@/shared/ui/badge";
import type { TFunction } from "i18next";
import { useTranslation } from "react-i18next";
import type { PlotStatus } from "../types";

interface PlotStatusChipProps {
    status: PlotStatus;
    className?: string;
}

/**
 * PlotStatusChip Component
 * 
 * Enhanced status badge with icons for better accessibility and visual clarity.
 * Uses color + icon combination to ensure status is clear even in grayscale.
 * Updated with standardized WCAG-compliant color tokens.
 */
export function getPlotStatusLabel(status: PlotStatus, t: TFunction): string {
    const statusKey: Record<PlotStatus, string> = {
        active: "plots.status.active",
        dormant: "plots.status.dormant",
        planned: "plots.status.planned",
        "at-risk": "plots.status.atRisk",
    };

    return t(statusKey[status] ?? "plots.status.planned");
}

export function PlotStatusChip({ status, className = "" }: PlotStatusChipProps) {
    const { t } = useTranslation();
    const statusConfig = {
        active: {
            icon: CheckCircle2,
            className: "bg-emerald-50 text-emerald-700 border border-emerald-200",
        },
        dormant: {
            icon: Moon,
            className: "bg-slate-50 text-slate-600 border border-slate-200",
        },
        planned: {
            icon: Clock,
            className: "bg-blue-50 text-blue-600 border border-blue-200",
        },
        "at-risk": {
            icon: AlertTriangle,
            className: "bg-amber-50 text-amber-700 border border-amber-200",
        },
    };

    const config = statusConfig[status] || statusConfig.planned;
    const Icon = config.icon;

    return (
        <Badge className={`${config.className} ${className} inline-flex items-center gap-1.5 py-1 rounded-md text-xs font-semibold`} style={{ paddingLeft: '10px', paddingRight: '10px' }}>
            <Icon className="w-3.5 h-3.5" />
            <span>{getPlotStatusLabel(status, t)}</span>
        </Badge>
    );
}



