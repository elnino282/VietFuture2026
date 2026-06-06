import type { FarmDetailResponse } from '@/entities/farm';
import { AddressDisplay, Badge, Button, Card, CardAction, CardContent, CardDescription, CardHeader, CardTitle } from '@/shared/ui';
import { Pencil, Plus, Trash2 } from 'lucide-react';
import { useTranslation } from 'react-i18next';

interface FarmInfoCardProps {
    farm: FarmDetailResponse;
    canEdit: boolean;
    canDelete: boolean;
    onEdit: () => void;
    onDelete: () => void;
    onCreatePlot: () => void;
}

/**
 * Farm information card component
 */
export function FarmInfoCard({
    farm,
    canEdit,
    canDelete,
    onEdit,
    onDelete,
    onCreatePlot,
}: FarmInfoCardProps) {
    const { t } = useTranslation();

    return (
        <Card>
            <CardHeader>
                <div>
                    <CardTitle>{farm.name}</CardTitle>
                    <CardDescription>{t('farmDetail.farmId', { id: farm.id })}</CardDescription>
                </div>
                <CardAction className="flex flex-wrap gap-2">
                    <Button
                        variant="outline"
                        size="sm"
                        onClick={onCreatePlot}
                        disabled={!farm.active}
                    >
                        <Plus className="h-4 w-4 mr-2" />
                        {t('farms.dialog.createPlotTitle')}
                    </Button>
                    {canEdit && (
                        <Button variant="outline" size="sm" onClick={onEdit}>
                            <Pencil className="h-4 w-4 mr-2" />
                            {t('common.edit')}
                        </Button>
                    )}
                    {canDelete && (
                        <Button variant="outline" size="sm" onClick={onDelete}>
                            <Trash2 className="h-4 w-4 mr-2" />
                            {t('common.delete')}
                        </Button>
                    )}
                </CardAction>
            </CardHeader>
            <CardContent className="space-y-4">
                <div className="grid grid-cols-2 gap-4">
                    <div>
                        <p className="text-sm font-medium text-muted-foreground">{t('farmDetail.overview.owner')}</p>
                        <p className="mt-1 text-sm">@{farm.ownerUsername}</p>
                    </div>
                    <div>
                        <p className="text-sm font-medium text-muted-foreground">{t('farmDetail.overview.status')}</p>
                        <div className="mt-1">
                            <Badge variant={farm.active ? 'default' : 'secondary'}>
                                {farm.active ? t('farmDetail.overview.active') : t('farmDetail.overview.inactive')}
                            </Badge>
                        </div>
                    </div>
                    <div>
                        <p className="text-sm font-medium text-muted-foreground">{t('farms.form.area')}</p>
                        <p className="mt-1 text-sm font-mono">
                            {farm.area ? `${farm.area} ha` : t('farmDetail.overview.notSpecified')}
                        </p>
                    </div>
                    <div>
                        <p className="text-sm font-medium text-muted-foreground">{t('farmDetail.stock.table.location')}</p>
                        <p className="mt-1 text-sm">
                            <AddressDisplay
                                wardCode={farm.wardId}
                                variant="full"
                                showIcon={true}
                                fallback={t('farmDetail.overview.notSpecified')}
                            />
                        </p>
                    </div>
                    {farm.totalPlots !== undefined && (
                        <div>
                            <p className="text-sm font-medium text-muted-foreground">{t('farmDetail.overview.totalPlots')}</p>
                            <p className="mt-1 text-sm font-mono">{farm.totalPlots}</p>
                        </div>
                    )}
                    {farm.activePlots !== undefined && (
                        <div>
                            <p className="text-sm font-medium text-muted-foreground">{t('farmDetail.overview.activePlots')}</p>
                            <p className="mt-1 text-sm font-mono">{farm.activePlots}</p>
                        </div>
                    )}
                </div>
            </CardContent>
        </Card>
    );
}



