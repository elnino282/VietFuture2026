import { Fragment, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { Package, AlertCircle, AlertTriangle, MoreVertical, RefreshCw } from 'lucide-react';
import {
    Alert,
    AlertDescription,
    AlertTitle,
    Badge,
    Button,
    CardContent,
    CardDescription,
    CardHeader,
    CardTitle,
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger,
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
    Skeleton,
    Switch,
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from '@/shared/ui';
import { AdminContentCard } from '@/features/admin/shared/ui';
import { adminDashboardApi } from '@/features/admin/shared/api';
import { usePreferences } from '@/shared/contexts';
import { useI18n } from '@/shared/lib/hooks/useI18n';

const WINDOW_OPTIONS = ['7', '14', '30', '60', '90'];

/**
 * InventoryHealthCards - Display inventory health per farm with real warehouse risk metrics.
 */
export function InventoryHealthCards() {
    const navigate = useNavigate();
    const { preferences } = usePreferences();
    const { t } = useI18n();
    const [windowDays, setWindowDays] = useState('30');
    const [expiredOnly, setExpiredOnly] = useState(false);
    const includeExpiring = !expiredOnly;

    const query = useQuery({
        queryKey: ['adminDashboard', 'inventoryHealth', windowDays, includeExpiring],
        queryFn: () =>
            adminDashboardApi.getInventoryHealth({
                windowDays: Number(windowDays),
                includeExpiring,
                limit: 5,
            }),
    });

    const farms = query.data?.farms ?? [];
    const summary = query.data?.summary;
    const dataQuality = query.data?.dataQuality;
    const missingExpiryDateCount = dataQuality?.missingExpiryDateCount ?? 0;
    const missingMovementHistoryCount = dataQuality?.missingMovementHistoryCount ?? 0;
    const hasDataQualityWarning = missingExpiryDateCount > 0 || missingMovementHistoryCount > 0;

    const formatNumber = useMemo(
        () => (value: number) => new Intl.NumberFormat(preferences.locale).format(value),
        [preferences.locale],
    );

    const buildInventoryUrl = (farmId?: number) => {
        const params = new URLSearchParams();
        if (farmId != null) {
            params.set('farmId', String(farmId));
        }
        params.set('windowDays', windowDays);
        params.set('status', includeExpiring ? 'RISK' : 'EXPIRED');
        return `/admin/inventory?${params.toString()}`;
    };

    return (
        <AdminContentCard>
            <CardHeader className="space-y-3">
                <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                    <div>
                        <div className="flex items-center gap-2">
                            <Package className="h-5 w-5 text-primary" />
                            <CardTitle className="text-lg">{t('admin.dashboard.inventoryHealth.title')}</CardTitle>
                            {query.isFetching && (
                                <RefreshCw className="h-4 w-4 text-muted-foreground animate-spin" />
                            )}
                        </div>
                        <CardDescription>
                            {t('admin.dashboard.inventoryHealth.description')}
                        </CardDescription>
                    </div>
                    <div className="flex flex-wrap items-center gap-3 justify-start sm:justify-end">
                        <Select value={windowDays} onValueChange={setWindowDays}>
                            <SelectTrigger className="h-8 w-[92px]">
                                <SelectValue />
                            </SelectTrigger>
                            <SelectContent>
                                {WINDOW_OPTIONS.map((option) => (
                                    <SelectItem key={option} value={option}>
                                        {t('admin.dashboard.inventoryHealth.daysOption', { count: option })}
                                    </SelectItem>
                                ))}
                            </SelectContent>
                        </Select>
                        <div className="flex items-center gap-2 text-sm text-muted-foreground">
                            <Switch checked={expiredOnly} onCheckedChange={setExpiredOnly} />
                            <span>{t('admin.dashboard.inventoryHealth.expiredOnly')}</span>
                        </div>
                        <Button
                            variant="ghost"
                            size="sm"
                            className="text-primary"
                            onClick={() => navigate(buildInventoryUrl())}
                        >
                            {t('admin.dashboard.inventoryHealth.viewAll')}
                        </Button>
                    </div>
                </div>
            </CardHeader>
            <CardContent>
                {query.isLoading && (
                    <div className="space-y-3">
                        {[...Array(4)].map((_, i) => (
                            <Skeleton key={i} className="h-10 w-full" />
                        ))}
                    </div>
                )}

                {query.isError && (
                    <Alert variant="destructive">
                        <AlertCircle className="h-4 w-4" />
                        <AlertTitle>{t('admin.dashboard.inventoryHealth.errorTitle')}</AlertTitle>
                        <AlertDescription className="mt-2 flex items-center justify-between gap-3">
                            <span>{query.error?.message || t('common.tryAgain')}</span>
                            <Button
                                variant="outline"
                                size="sm"
                                onClick={() => query.refetch()}
                            >
                                {t('common.retry')}
                            </Button>
                        </AlertDescription>
                    </Alert>
                )}

                {!query.isLoading && !query.isError && hasDataQualityWarning && (
                    <div className="mb-3 rounded border border-amber-200 bg-amber-50 px-3 py-2 text-xs text-amber-900">
                        <div className="flex items-center gap-2 font-medium">
                            <AlertTriangle className="h-4 w-4" />
                            {t('admin.dashboard.inventoryHealth.dataQuality.title')}
                        </div>
                        <p className="mt-1">
                            {t('admin.dashboard.inventoryHealth.dataQuality.missingExpiryDate')}: <span className="font-semibold">{formatNumber(missingExpiryDateCount)}</span>
                            {' '}| {t('admin.dashboard.inventoryHealth.dataQuality.missingMovementHistory')}: <span className="font-semibold">{formatNumber(missingMovementHistoryCount)}</span>
                            {typeof dataQuality?.coveragePercent === 'number' && (
                                <>
                                    {' '}| {t('admin.dashboard.inventoryHealth.dataQuality.coverage')}: <span className="font-semibold">{dataQuality.coveragePercent.toFixed(1)}%</span>
                                </>
                            )}
                        </p>
                    </div>
                )}

                {!query.isLoading && !query.isError && farms.length === 0 && (
                    <div className="flex flex-col items-center justify-center py-8 text-center">
                        <div className="rounded-full bg-emerald-50 p-3 mb-3">
                            <Package className="h-6 w-6 text-emerald-500" />
                        </div>
                        <p className="text-sm font-medium text-muted-foreground">
                            {t('admin.dashboard.inventoryHealth.empty')}
                        </p>
                        <Button
                            variant="outline"
                            size="sm"
                            className="mt-4"
                            onClick={() => navigate(buildInventoryUrl())}
                        >
                            {t('admin.dashboard.inventoryHealth.openInventory')}
                        </Button>
                    </div>
                )}

                {!query.isLoading && !query.isError && farms.length > 0 && (
                    <div className="space-y-3">
                        {summary && (
                            <div className="grid grid-cols-2 gap-2 text-xs sm:grid-cols-3 lg:grid-cols-5">
                                <div className="rounded border bg-muted/20 px-2 py-1">
                                    {t('admin.dashboard.inventoryHealth.summary.expired')}: <span className="font-semibold">{formatNumber(summary.expiredLots)}</span>
                                </div>
                                <div className="rounded border bg-muted/20 px-2 py-1">
                                    {t('admin.dashboard.inventoryHealth.summary.expiringSoon')}: <span className="font-semibold">{formatNumber(summary.expiringSoonLots)}</span>
                                </div>
                                <div className="rounded border bg-muted/20 px-2 py-1">
                                    {t('admin.dashboard.inventoryHealth.summary.lowStock')}: <span className="font-semibold">{formatNumber(summary.lowStockLots)}</span>
                                </div>
                                <div className="rounded border bg-muted/20 px-2 py-1">
                                    {t('admin.dashboard.inventoryHealth.summary.noMovement')}: <span className="font-semibold">{formatNumber(summary.noMovementLots)}</span>
                                </div>
                                <div className="rounded border bg-muted/20 px-2 py-1">
                                    {t('admin.dashboard.inventoryHealth.summary.slowMovement')}: <span className="font-semibold">{formatNumber(summary.slowMovementLots)}</span>
                                </div>
                            </div>
                        )}

                        <Table>
                            <TableHeader>
                                <TableRow>
                                    <TableHead>{t('admin.dashboard.inventoryHealth.table.farm')}</TableHead>
                                    <TableHead>{t('admin.dashboard.inventoryHealth.summary.expired')}</TableHead>
                                    {includeExpiring && <TableHead>{t('admin.dashboard.inventoryHealth.summary.expiringSoon')}</TableHead>}
                                    <TableHead>{t('admin.dashboard.inventoryHealth.summary.lowStock')}</TableHead>
                                    <TableHead>{t('admin.dashboard.inventoryHealth.summary.noMovement')}</TableHead>
                                    <TableHead>{t('admin.dashboard.inventoryHealth.summary.slowMovement')}</TableHead>
                                    <TableHead className="text-right">{t('admin.dashboard.inventoryHealth.table.qtyAtRisk')}</TableHead>
                                    <TableHead className="text-right">{t('common.actions')}</TableHead>
                                </TableRow>
                            </TableHeader>
                            <TableBody>
                                {farms.map((farm) => {
                                    const topLots = farm.topRiskLots?.slice(0, 2) ?? [];
                                    const colSpan = includeExpiring ? 8 : 7;

                                    return (
                                        <Fragment key={farm.farmId}>
                                            <TableRow>
                                                <TableCell className="font-medium">{farm.farmName}</TableCell>
                                                <TableCell>
                                                    {farm.expiredLots > 0 ? (
                                                        <Badge variant="destructive">{farm.expiredLots}</Badge>
                                                    ) : (
                                                        <span className="text-muted-foreground">0</span>
                                                    )}
                                                </TableCell>
                                                {includeExpiring && (
                                                    <TableCell>
                                                        {farm.expiringSoonLots > 0 ? (
                                                            <Badge variant="outline" className="text-amber-600 border-amber-300">
                                                                {farm.expiringSoonLots}
                                                            </Badge>
                                                        ) : (
                                                            <span className="text-muted-foreground">0</span>
                                                        )}
                                                    </TableCell>
                                                )}
                                                <TableCell>{formatNumber(farm.lowStockLots)}</TableCell>
                                                <TableCell>{formatNumber(farm.noMovementLots)}</TableCell>
                                                <TableCell>{formatNumber(farm.slowMovementLots)}</TableCell>
                                                <TableCell className="text-right font-mono">
                                                    {farm.qtyAtRisk == null
                                                        ? '--'
                                                        : formatNumber(Number(farm.qtyAtRisk))}
                                                </TableCell>
                                                <TableCell className="text-right">
                                                    <DropdownMenu>
                                                        <DropdownMenuTrigger asChild>
                                                            <Button
                                                                type="button"
                                                                variant="ghost"
                                                                size="icon"
                                                                className="h-8 w-8 rounded-[14px]"
                                                                aria-label={t('admin.dashboard.inventoryHealth.actionsFor', { name: farm.farmName })}
                                                            >
                                                                <MoreVertical className="h-4 w-4" />
                                                            </Button>
                                                        </DropdownMenuTrigger>
                                                        <DropdownMenuContent align="end" className="w-44">
                                                            <DropdownMenuItem
                                                                onSelect={() => navigate(buildInventoryUrl(farm.farmId))}
                                                            >
                                                                {t('admin.dashboard.inventoryHealth.openInventory')}
                                                            </DropdownMenuItem>
                                                        </DropdownMenuContent>
                                                    </DropdownMenu>
                                                </TableCell>
                                            </TableRow>
                                            {topLots.length > 0 && (
                                                <TableRow>
                                                    <TableCell colSpan={colSpan} className="text-xs text-muted-foreground">
                                                        {topLots.map((lot) => (
                                                            <span key={lot.lotId} className="mr-4">
                                                                {lot.itemName} - {t(`admin.dashboard.inventoryHealth.lotStatus.${lot.status}`, lot.status)}
                                                                {lot.expiryDate ? ` (${lot.expiryDate})` : ''}
                                                            </span>
                                                        ))}
                                                    </TableCell>
                                                </TableRow>
                                            )}
                                        </Fragment>
                                    );
                                })}
                            </TableBody>
                        </Table>
                    </div>
                )}
            </CardContent>
        </AdminContentCard>
    );
}
