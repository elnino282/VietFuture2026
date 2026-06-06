import httpClient from '@/shared/api/http';
import { parseApiResponse, parsePageResponse, type PageResponse } from '@/shared/api/types';
import { z } from 'zod';
import {
    FieldLogListParamsSchema,
    FieldLogSchema,
    FieldLogCreateRequestSchema,
    FieldLogUpdateRequestSchema,
    SeasonMinimalSchema,
} from '../model/schemas';
import type {
    FieldLogListParams,
    FieldLog,
    FieldLogCreateRequest,
    FieldLogUpdateRequest,
    SeasonMinimal,
} from '../model/types';
import type { FieldLogScope } from '../model/keys';

const fieldLogPaths = {
    listBySeason: (seasonId: number, scope: FieldLogScope) =>
        scope === 'employee'
            ? `/api/v1/employee/seasons/${seasonId}/field-logs`
            : '/api/v1/field-logs',
    detail: (id: number, scope: FieldLogScope) =>
        scope === 'employee'
            ? `/api/v1/employee/field-logs/${id}`
            : `/api/v1/field-logs/${id}`,
    create: (seasonId: number, scope: FieldLogScope) =>
        scope === 'employee'
            ? `/api/v1/employee/seasons/${seasonId}/field-logs`
            : '/api/v1/field-logs',
};

export const fieldLogApi = {
    /**
     * List field logs for a season.
     * GET /api/v1/field-logs?seasonId=&type=&q=&from=&to=&page=&size=
     */
    listBySeason: async (
        seasonId: number,
        params?: FieldLogListParams,
        scope: FieldLogScope = 'farmer'
    ): Promise<PageResponse<FieldLog>> => {
        const validatedParams = params ? FieldLogListParamsSchema.parse(params) : {};
        const response = await httpClient.get(fieldLogPaths.listBySeason(seasonId, scope), {
            params: scope === 'employee' ? validatedParams : { seasonId, ...validatedParams }
        });
        return parsePageResponse(response.data, FieldLogSchema);
    },

    /**
     * Get field log by ID.
     * GET /api/v1/field-logs/{id}
     */
    getById: async (id: number, scope: FieldLogScope = 'farmer'): Promise<FieldLog> => {
        const response = await httpClient.get(fieldLogPaths.detail(id, scope));
        return parseApiResponse(response.data, FieldLogSchema);
    },

    /**
     * Create field log.
     * POST /api/v1/field-logs (body: {seasonId, logDate, logType, notes})
     */
    create: async (
        seasonId: number,
        data: FieldLogCreateRequest,
        scope: FieldLogScope = 'farmer'
    ): Promise<FieldLog> => {
        const validatedPayload = FieldLogCreateRequestSchema.parse({ ...data, seasonId });
        const response = await httpClient.post(fieldLogPaths.create(seasonId, scope), validatedPayload);
        return parseApiResponse(response.data, FieldLogSchema);
    },

    /**
     * Update field log.
     * PUT /api/v1/field-logs/{id} (body: {logDate, logType, notes})
     */
    update: async (
        id: number,
        data: FieldLogUpdateRequest,
        scope: FieldLogScope = 'farmer'
    ): Promise<FieldLog> => {
        const validatedPayload = FieldLogUpdateRequestSchema.parse(data);
        const response = await httpClient.put(fieldLogPaths.detail(id, scope), validatedPayload);
        return parseApiResponse(response.data, FieldLogSchema);
    },

    /**
     * Delete field log.
     * DELETE /api/v1/field-logs/{id}
     */
    delete: async (id: number, scope: FieldLogScope = 'farmer'): Promise<void> => {
        await httpClient.delete(fieldLogPaths.detail(id, scope));
    },

    /**
     * Get user's seasons for dropdown.
     * GET /api/v1/seasons/my
     */
    getUserSeasons: async (): Promise<SeasonMinimal[]> => {
        const response = await httpClient.get('/api/v1/seasons/my');
        const data = response.data;
        // Handle ApiResponse wrapper
        const result = data?.result ?? data?.data ?? data;
        return z.array(SeasonMinimalSchema).parse(result);
    },

    /**
     * Get active assigned seasons for employee workspace.
     * GET /api/v1/employee/seasons
     */
    getEmployeeAssignedSeasons: async (): Promise<SeasonMinimal[]> => {
        const response = await httpClient.get('/api/v1/employee/seasons');
        const data = response.data;
        const result = data?.result ?? data?.data ?? data;
        return z.array(SeasonMinimalSchema).parse(result);
    },
};


