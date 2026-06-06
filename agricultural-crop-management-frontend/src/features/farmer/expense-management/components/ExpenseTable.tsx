import {
    CheckCircle2,
    AlertCircle,
    Clock,
    Paperclip,
    Eye,
    Edit,
    Trash2,
    MoreVertical,
    ExternalLink,
    Tag,
    Link as LinkIcon,
    Receipt,
} from "lucide-react";
import { Button } from "@/shared/ui/button";
import { Badge } from "@/shared/ui/badge";
import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from "@/shared/ui/table";
import {
    Tooltip,
    TooltipContent,
    TooltipProvider,
    TooltipTrigger,
} from "@/shared/ui/tooltip";
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger,
    DropdownMenuSeparator,
} from "@/shared/ui/dropdown-menu";
import type { Expense } from "../types";
import { CATEGORY_COLORS } from "../constants";
import { usePreferences } from "@/shared/contexts";
import { formatMoney, convertToDisplayCurrency } from "@/shared/lib";
import { useI18n } from "@/shared/lib/hooks/useI18n";

interface ExpenseTableProps {
    filteredExpenses: Expense[];
    totalExpenses: number;
    handleEditExpense: (expense: Expense) => void;
    handleDeleteExpense: (expense: Expense) => void;
    handleViewExpense: (expense: Expense) => void;
    onAddExpense?: () => void;
}

export function ExpenseTable({
    filteredExpenses,
    totalExpenses,
    handleEditExpense,
    handleDeleteExpense,
    handleViewExpense,
    onAddExpense,
}: ExpenseTableProps) {
    const { preferences } = usePreferences();
    const { t } = useI18n();
    const getStatusBadge = (status: string) => {
        switch (status) {
            case "PAID":
                return (
                    <Badge className="bg-primary/10 text-primary border-primary/20">
                        <CheckCircle2 className="w-3 h-3 mr-1" />
                        {t("expenses.status.paid")}
                    </Badge>
                );
            case "UNPAID":
                return (
                    <Badge className="bg-destructive/10 text-destructive border-destructive/20">
                        <AlertCircle className="w-3 h-3 mr-1" />
                        {t("expenses.status.unpaid")}
                    </Badge>
                );
            case "PENDING":
                return (
                    <Badge className="bg-accent/10 text-foreground border-accent/20">
                        <Clock className="w-3 h-3 mr-1" />
                        {t("expenses.status.pending")}
                    </Badge>
                );
            default:
                return null;
        }
    };

    const getCategoryColor = (category: string) => {
        return CATEGORY_COLORS[category] || "var(--muted-foreground)";
    };

    const totalAmount = filteredExpenses.reduce(
        (sum, exp) => sum + exp.amount,
        0
    );
    const displayTotalAmount = formatMoney(
        convertToDisplayCurrency(totalAmount, preferences.currency),
        preferences.currency,
        preferences.locale
    );

    return (
        <div className="space-y-4">
            <div className="flex items-center justify-between mb-4">
                <div>
                    <p className="text-sm text-muted-foreground">
                        {t("expenses.table.showingCount", {
                            shown: filteredExpenses.length,
                            total: totalExpenses,
                        })}
                    </p>
                </div>
                <Badge className="bg-muted text-foreground border-border">
                    <span className="numeric">
                        {t("expenses.table.totalAmount", { amount: displayTotalAmount })}
                    </span>
                </Badge>
            </div>

            <div className="overflow-x-auto rounded-xl border border-border">
                <Table>
                    <TableHeader>
                        <TableRow className="bg-muted hover:bg-muted">
                            <TableHead className="text-foreground">{t("expenses.table.columns.date")}</TableHead>
                            <TableHead className="text-foreground">{t("expenses.table.columns.category")}</TableHead>
                            <TableHead className="text-foreground">{t("expenses.table.columns.description")}</TableHead>
                            <TableHead className="text-foreground">{t("expenses.table.columns.linkedTo")}</TableHead>
                            <TableHead className="text-foreground text-right">{t("expenses.table.columns.amount")}</TableHead>
                            <TableHead className="text-foreground">{t("expenses.table.columns.status")}</TableHead>
                            <TableHead className="text-foreground text-center">{t("expenses.table.columns.receipt")}</TableHead>
                            <TableHead className="text-foreground">{t("expenses.table.columns.actions")}</TableHead>
                        </TableRow>
                    </TableHeader>
                    <TableBody>
                        {filteredExpenses.length === 0 ? (
                            <TableRow>
                                <TableCell
                                    colSpan={8}
                                    className="text-center py-12 text-muted-foreground"
                                >
                                    <AlertCircle className="w-12 h-12 mx-auto mb-3 text-muted-foreground" />
                                    <p>{t("expenses.table.emptyTitle")}</p>
                                    <p className="text-sm mt-1">{t("expenses.table.emptyDescription")}</p>
                                    {onAddExpense && (
                                        <div className="mt-4">
                                            <Button variant="outline" onClick={onAddExpense}>
                                                {t("expenses.createButton")}
                                            </Button>
                                        </div>
                                    )}
                                </TableCell>
                            </TableRow>
                        ) : (
                            filteredExpenses.map((expense) => (
                                <TableRow key={expense.id} className="hover:bg-muted/50">
                                    <TableCell className="text-sm text-muted-foreground">
                                        {new Date(expense.date).toLocaleDateString(preferences.locale, {
                                            month: "short",
                                            day: "numeric",
                                            year: "numeric",
                                        })}
                                    </TableCell>
                                    <TableCell>
                                        <Badge
                                            variant="outline"
                                            style={{
                                                borderColor: `color-mix(in oklab, ${getCategoryColor(expense.category)} 20%, transparent)`,
                                                backgroundColor: `color-mix(in oklab, ${getCategoryColor(expense.category)} 10%, transparent)`,
                                                color: getCategoryColor(expense.category),
                                            }}
                                        >
                                            <Tag className="w-3 h-3 mr-1" />
                                            {expense.category}
                                        </Badge>
                                    </TableCell>
                                    <TableCell>
                                        <div className="flex flex-col">
                                            <span className="text-sm text-foreground">
                                                {expense.description}
                                            </span>
                                            {expense.vendor && (
                                                <span className="text-xs text-muted-foreground">
                                                    {expense.vendor}
                                                </span>
                                            )}
                                        </div>
                                    </TableCell>
                                    <TableCell>
                                        <div className="flex flex-col gap-1">
                                            {expense.linkedSeason && (
                                                <span className="text-xs text-muted-foreground flex items-center gap-1">
                                                    <LinkIcon className="w-3 h-3" />
                                                    {expense.linkedSeason}
                                                </span>
                                            )}
                                            {expense.linkedTask && (
                                                <span className="text-xs text-muted-foreground flex items-center gap-1">
                                                    <CheckCircle2 className="w-3 h-3" />
                                                    {expense.linkedTask}
                                                </span>
                                            )}
                                        </div>
                                    </TableCell>
                                    <TableCell className="text-right numeric text-foreground">
                                        {formatMoney(convertToDisplayCurrency(expense.amount, preferences.currency), preferences.currency, preferences.locale)}
                                    </TableCell>
                                    <TableCell>{getStatusBadge(expense.status)}</TableCell>
                                    <TableCell className="text-center">
                                        {expense.attachmentUrl ? (
                                            <TooltipProvider>
                                                <Tooltip>
                                                    <TooltipTrigger asChild>
                                                        <Button
                                                            variant="outline"
                                                            size="sm"
                                                            className="h-8 px-2 text-xs"
                                                            onClick={() => window.open(expense.attachmentUrl, "_blank", "noopener,noreferrer")}
                                                        >
                                                            <Paperclip className="w-3.5 h-3.5 mr-1 text-primary" />
                                                            <ExternalLink className="w-3.5 h-3.5 mr-1 text-primary" />
                                                            {t("expenses.table.openNewTab")}
                                                        </Button>
                                                    </TooltipTrigger>
                                                    <TooltipContent>
                                                        <p className="text-xs">
                                                            {t("expenses.table.receiptTooltip", {
                                                                name: expense.attachmentName ?? t("expenses.table.receiptFallback"),
                                                            })}
                                                        </p>
                                                    </TooltipContent>
                                                </Tooltip>
                                            </TooltipProvider>
                                        ) : (
                                            <span className="text-xs text-muted-foreground">-</span>
                                        )}
                                    </TableCell>
                                    <TableCell>
                                        <DropdownMenu>
                                            <DropdownMenuTrigger asChild>
                                                <Button
                                                    variant="ghost"
                                                    size="sm"
                                                    className="h-8 w-8 p-0 hover:bg-muted"
                                                >
                                                    <MoreVertical className="w-4 h-4 text-muted-foreground" />
                                                </Button>
                                            </DropdownMenuTrigger>
                                            <DropdownMenuContent align="end" className="w-48">
                                                <DropdownMenuItem
                                                    onClick={() => handleEditExpense(expense)}
                                                >
                                                    <Edit className="w-4 h-4 mr-2" />
                                                    {t("common.edit")}
                                                </DropdownMenuItem>
                                                <DropdownMenuItem
                                                    onClick={() => handleViewExpense(expense)}
                                                >
                                                    <Eye className="w-4 h-4 mr-2" />
                                                    {t("expenses.table.viewDetails")}
                                                </DropdownMenuItem>
                                                <DropdownMenuItem
                                                    disabled={!expense.attachmentUrl}
                                                    onClick={() => {
                                                        if (expense.attachmentUrl) {
                                                            window.open(expense.attachmentUrl, "_blank", "noopener,noreferrer");
                                                        }
                                                    }}
                                                >
                                                    <Receipt className="w-4 h-4 mr-2" />
                                                    <ExternalLink className="w-3.5 h-3.5 mr-2" />
                                                    {t("expenses.table.downloadReceipt")}
                                                </DropdownMenuItem>
                                                <DropdownMenuSeparator />
                                                <DropdownMenuItem
                                                    onClick={() => handleDeleteExpense(expense)}
                                                    className="text-destructive"
                                                >
                                                    <Trash2 className="w-4 h-4 mr-2" />
                                                    {t("common.delete")}
                                                </DropdownMenuItem>
                                            </DropdownMenuContent>
                                        </DropdownMenu>
                                    </TableCell>
                                </TableRow>
                            ))
                        )}
                    </TableBody>
                </Table>
            </div>
        </div>
    );
}
