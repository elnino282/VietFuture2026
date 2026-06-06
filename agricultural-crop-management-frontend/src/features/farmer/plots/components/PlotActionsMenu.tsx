import { MoreVertical, Eye, Edit, Copy, Trash2 } from "lucide-react";
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from "@/shared/ui/dropdown-menu";
import { Button } from "@/shared/ui/button";
import { useTranslation } from "react-i18next";
import type { Plot } from "../types";

interface PlotActionsMenuProps {
    plot: Plot;
    onViewDetails: (plot: Plot) => void;
    onEdit?: (plot: Plot) => void;
    onDuplicate?: (plot: Plot) => void;
    onDelete: (id: string) => void;
}

/**
 * PlotActionsMenu Component
 * 
 * Kebab menu (⋮) for plot row actions.
 * Provides cleaner UI than horizontal icon buttons and prevents accidental clicks.
 */
export function PlotActionsMenu({
    plot,
    onViewDetails,
    onEdit,
    onDuplicate,
    onDelete,
}: PlotActionsMenuProps) {
    const { t } = useTranslation();

    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <Button
                    variant="ghost"
                    size="sm"
                    className="h-10 w-10 p-0 hover:bg-gray-100"
                    onClick={(e) => {
                        // Prevent row click from triggering
                        e.stopPropagation();
                    }}
                >
                    <MoreVertical className="h-4 w-4" />
                    <span className="sr-only">{t("plots.actions.openMenu")}</span>
                </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end" className="w-[180px]">
                <DropdownMenuItem
                    onClick={(e) => {
                        e.stopPropagation();
                        onViewDetails(plot);
                    }}
                >
                    <Eye className="mr-2 h-4 w-4" />
                    <span>{t("plots.actions.viewDetails")}</span>
                </DropdownMenuItem>

                {onEdit && (
                    <DropdownMenuItem
                        onClick={(e) => {
                            e.stopPropagation();
                            onEdit(plot);
                        }}
                    >
                        <Edit className="mr-2 h-4 w-4" />
                        <span>{t("common.edit")}</span>
                    </DropdownMenuItem>
                )}

                {onDuplicate && (
                    <DropdownMenuItem
                        onClick={(e) => {
                            e.stopPropagation();
                            onDuplicate(plot);
                        }}
                    >
                        <Copy className="mr-2 h-4 w-4" />
                        <span>{t("plots.actions.duplicate")}</span>
                    </DropdownMenuItem>
                )}

                <DropdownMenuSeparator />

                <DropdownMenuItem
                    onClick={(e) => {
                        e.stopPropagation();
                        onDelete(plot.id);
                    }}
                    className="text-red-600 focus:text-red-600 focus:bg-red-50"
                >
                    <Trash2 className="mr-2 h-4 w-4" />
                    <span>{t("common.delete")}</span>
                </DropdownMenuItem>
            </DropdownMenuContent>
        </DropdownMenu>
    );
}



