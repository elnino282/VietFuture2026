import type { Plot } from '@/entities/plot';
import { Badge, Skeleton, Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/shared/ui';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { getSoilTypeLabel } from '@/features/farmer/shared/plotOptions';

interface FarmPlotsTableProps {
    plots: Plot[];
    isLoading?: boolean;
}

/**
 * Get status badge variant based on plot status
 */
function getStatusBadgeVariant(status: string | null | undefined): 'default' | 'secondary' | 'destructive' | 'outline' {
    switch (status?.toUpperCase()) {
        case 'IN_USE':
            return 'default';
        case 'IDLE':
        case 'AVAILABLE':
            return 'secondary';
        case 'FALLOW':
        case 'MAINTENANCE':
            return 'outline';
        default:
            return 'outline';
    }
}

/**
 * Get human-readable status label
 */
function getStatusLabel(status: string | null | undefined, t: (key: string) => string): string {
    const normalized = status?.toUpperCase();
    switch (normalized) {
        case 'IN_USE':
        case 'IDLE':
        case 'AVAILABLE':
        case 'FALLOW':
        case 'MAINTENANCE':
            return t(`farms.plotStatuses.${normalized}`);
        default:
            return status || t('farmDetail.seasons.statusLabels.Unknown');
    }
}

/**
 * Table showing plots belonging to a farm
 */
export function FarmPlotsTable({ plots, isLoading = false }: FarmPlotsTableProps) {
    const navigate = useNavigate();
    const { t } = useTranslation();

    // Loading state
    if (isLoading) {
        return (
            <div className="rounded-md border">
                <Table>
                    <TableHeader>
                        <TableRow>
                            <TableHead>{t('plots.table.name')}</TableHead>
                            <TableHead className="text-right">{t('plots.table.area')}</TableHead>
                            <TableHead>{t('plots.table.soilType')}</TableHead>
                            <TableHead>{t('plots.table.status')}</TableHead>
                        </TableRow>
                    </TableHeader>
                    <TableBody>
                        {[1, 2, 3].map((i) => (
                            <TableRow key={i}>
                                <TableCell><Skeleton className="h-4 w-32" /></TableCell>
                                <TableCell><Skeleton className="h-4 w-16 ml-auto" /></TableCell>
                                <TableCell><Skeleton className="h-4 w-20" /></TableCell>
                                <TableCell><Skeleton className="h-6 w-16" /></TableCell>
                            </TableRow>
                        ))}
                    </TableBody>
                </Table>
            </div>
        );
    }

    // Empty state
    if (!plots || plots.length === 0) {
        return (
            <div className="rounded-md border border-dashed p-8 text-center text-gray-500">
                <h3 className="text-lg font-semibold text-gray-900 mb-2">{t('farmDetail.plots.noPlots')}</h3>
                <p>{t('farmDetail.plots.createToStart')}</p>
            </div>
        );
    }

    return (
        <div className="rounded-md border">
            <Table>
                <TableHeader>
                    <TableRow>
                        <TableHead>{t('plots.table.name')}</TableHead>
                        <TableHead className="text-right">{t('plots.table.area')}</TableHead>
                        <TableHead>{t('plots.table.soilType')}</TableHead>
                        <TableHead>{t('plots.table.status')}</TableHead>
                    </TableRow>
                </TableHeader>
                <TableBody>
                    {plots.map((plot) => (
                        <TableRow
                            key={plot.id}
                            className="cursor-pointer hover:bg-gray-50"
                            onClick={() => navigate(`/farmer/plots?plotId=${plot.id}`)}
                        >
                            <TableCell className="font-medium">
                                {plot.plotName}
                            </TableCell>
                            <TableCell className="text-right font-mono">
                                {plot.area != null ? plot.area.toFixed(2) : '-'}
                            </TableCell>
                            <TableCell>
                                {getSoilTypeLabel(plot.soilType, t) || '-'}
                            </TableCell>
                            <TableCell>
                                <Badge variant={getStatusBadgeVariant(plot.status)}>
                                    {getStatusLabel(plot.status, t)}
                                </Badge>
                            </TableCell>
                        </TableRow>
                    ))}
                </TableBody>
            </Table>
        </div>
    );
}



