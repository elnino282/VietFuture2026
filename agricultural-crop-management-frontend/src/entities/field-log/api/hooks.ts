import {
    useMutation,
    useQuery,
    useQueryClient,
    type UseMutationOptions,
    type UseQueryOptions,
} from '@tanstack/react-query';
import type { PageResponse } from '@/shared/api/types';
import { fieldLogKeys } from '../model/keys';
import { fieldLogApi } from './client';
import type {
    FieldLogListParams,
    FieldLog,
    FieldLogCreateRequest,
    FieldLogUpdateRequest,
    SeasonMinimal,
} from '../model/types';
import type { FieldLogScope } from '../model/keys';

const visiblePageRefetchOptions = {
    refetchInterval: 30 * 1000,
    refetchIntervalInBackground: false,
    refetchOnWindowFocus: true,
} as const;

export const useFieldLogsBySeason = (
    seasonId: number,
    params?: FieldLogListParams,
    options?: Omit<UseQueryOptions<PageResponse<FieldLog>, Error>, 'queryKey' | 'queryFn'>,
    scope: FieldLogScope = 'farmer',
) => useQuery({
    queryKey: fieldLogKeys.listBySeason(seasonId, params, scope),
    queryFn: () => fieldLogApi.listBySeason(seasonId, params, scope),
    enabled: seasonId > 0,
    staleTime: 5 * 60 * 1000,
    ...visiblePageRefetchOptions,
    ...options,
});

export const useFieldLogById = (
    id: number,
    options?: Omit<UseQueryOptions<FieldLog, Error>, 'queryKey' | 'queryFn'>,
    scope: FieldLogScope = 'farmer',
) => useQuery({
    queryKey: fieldLogKeys.detail(id, scope),
    queryFn: () => fieldLogApi.getById(id, scope),
    enabled: id > 0,
    staleTime: 5 * 60 * 1000,
    ...options,
});

export const useUserSeasons = (
    options?: Omit<UseQueryOptions<SeasonMinimal[], Error>, 'queryKey' | 'queryFn'>
) => useQuery({
    queryKey: ['field-logs', 'user-seasons'],
    queryFn: () => fieldLogApi.getUserSeasons(),
    staleTime: 10 * 60 * 1000,
    ...options,
});

export const useEmployeeAssignedSeasons = (
    options?: Omit<UseQueryOptions<SeasonMinimal[], Error>, 'queryKey' | 'queryFn'>
) => useQuery({
    queryKey: fieldLogKeys.employeeSeasons(),
    queryFn: () => fieldLogApi.getEmployeeAssignedSeasons(),
    staleTime: 2 * 60 * 1000,
    refetchOnWindowFocus: true,
    ...options,
});

export const useCreateFieldLog = (
    seasonId: number,
    options?: UseMutationOptions<FieldLog, Error, FieldLogCreateRequest>,
    scope: FieldLogScope = 'farmer',
) => {
    const queryClient = useQueryClient();
    const { onSuccess, ...mutationOptions } = options ?? {};
    return useMutation({
        mutationFn: (data) => fieldLogApi.create(seasonId, data, scope),
        ...mutationOptions,
        onSuccess: (data, variables, onMutateResult, context) => {
            queryClient.invalidateQueries({
                queryKey: fieldLogKeys.scopedListBySeasonBase(scope, seasonId),
                exact: false,
            });
            onSuccess?.(data, variables, onMutateResult, context);
        },
    });
};

export const useUpdateFieldLog = (
    seasonId: number,
    options?: UseMutationOptions<FieldLog, Error, { id: number; data: FieldLogUpdateRequest }>,
    scope: FieldLogScope = 'farmer',
) => {
    const queryClient = useQueryClient();
    const { onSuccess, ...mutationOptions } = options ?? {};
    return useMutation({
        mutationFn: ({ id, data }) => fieldLogApi.update(id, data, scope),
        ...mutationOptions,
        onSuccess: (data, variables, onMutateResult, context) => {
            const { id } = variables;
            queryClient.invalidateQueries({ queryKey: fieldLogKeys.detail(id, scope) });
            queryClient.invalidateQueries({
                queryKey: fieldLogKeys.scopedListBySeasonBase(scope, seasonId),
                exact: false,
            });
            onSuccess?.(data, variables, onMutateResult, context);
        },
    });
};

export const useDeleteFieldLog = (
    seasonId: number,
    options?: UseMutationOptions<void, Error, number>,
    scope: FieldLogScope = 'farmer',
) => {
    const queryClient = useQueryClient();
    const { onSuccess, ...mutationOptions } = options ?? {};
    return useMutation({
        mutationFn: (id) => fieldLogApi.delete(id, scope),
        ...mutationOptions,
        onSuccess: (data, variables, onMutateResult, context) => {
            queryClient.invalidateQueries({
                queryKey: fieldLogKeys.scopedListBySeasonBase(scope, seasonId),
                exact: false,
            });
            onSuccess?.(data, variables, onMutateResult, context);
        },
    });
};
