import type { FieldLogListParams } from './types';

export type FieldLogScope = 'farmer' | 'employee';

export const fieldLogKeys = {
    all: ['field-log'] as const,
    lists: () => [...fieldLogKeys.all, 'list'] as const,
    listBySeasonBase: (seasonId: number) =>
        [...fieldLogKeys.lists(), 'farmer', 'bySeason', seasonId] as const,
    scopedListBySeasonBase: (scope: FieldLogScope, seasonId: number) =>
        [...fieldLogKeys.lists(), scope, 'bySeason', seasonId] as const,
    listBySeason: (seasonId: number, params?: FieldLogListParams, scope: FieldLogScope = 'farmer') =>
        [...fieldLogKeys.lists(), scope, 'bySeason', seasonId, params] as const,
    details: () => [...fieldLogKeys.all, 'detail'] as const,
    detail: (id: number, scope: FieldLogScope = 'farmer') => [...fieldLogKeys.details(), scope, id] as const,
    employeeSeasons: () => [...fieldLogKeys.all, 'employee-seasons'] as const,
};
