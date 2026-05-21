import type { HarvestListParams } from './types';

type HarvestListScopeParams = Omit<HarvestListParams, 'seasonId'>;

const withoutSeasonId = (
    params?: HarvestListParams
): HarvestListScopeParams | undefined => {
    if (!params) return undefined;
    const { seasonId: _ignoredSeasonId, ...rest } = params;
    return rest;
};

export const harvestKeys = {
    all: ['harvest'] as const,
    lists: () => [...harvestKeys.all, 'list'] as const,
    listAllBase: () => [...harvestKeys.lists(), 'all'] as const,
    listAll: (params?: HarvestListParams) =>
        [...harvestKeys.listAllBase(), params] as const,
    listBySeasonBase: (seasonId: number) =>
        [...harvestKeys.lists(), 'season', seasonId] as const,
    listBySeason: (seasonId: number, params?: HarvestListParams) =>
        [...harvestKeys.listBySeasonBase(seasonId), withoutSeasonId(params)] as const,
    summaries: () => [...harvestKeys.all, 'summary'] as const,
    summary: (seasonId?: number) => [...harvestKeys.summaries(), seasonId] as const,
    stockContexts: () => [...harvestKeys.all, 'stock-context'] as const,
    stockContext: (seasonId: number, warehouseId: number, productName: string, lotCode: string) =>
        [...harvestKeys.stockContexts(), seasonId, warehouseId, productName, lotCode] as const,
    details: () => [...harvestKeys.all, 'detail'] as const,
    detail: (id: number) => [...harvestKeys.details(), id] as const,
};
