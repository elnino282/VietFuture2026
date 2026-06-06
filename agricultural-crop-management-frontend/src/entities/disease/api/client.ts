import httpClient from "@/shared/api/http";
import { parseApiResponse, parsePageResponse, type PageResponse } from "@/shared/api/types";
import {
  DiseaseRecordCreateRequestSchema,
  DiseaseRecordDetailSchema,
  DiseaseRecordListParamsSchema,
  DiseaseRecordSchema,
  DiseaseSuggestionRequestSchema,
  DiseaseSuggestionResponseSchema,
  DiseaseRecordUpdateRequestSchema,
  DiseaseTreatmentCreateRequestSchema,
  DiseaseTreatmentListParamsSchema,
  DiseaseTreatmentSchema,
  DiseaseTreatmentUpdateRequestSchema,
} from "../model/schemas";
import type {
  DiseaseRecord,
  DiseaseRecordCreateRequest,
  DiseaseRecordDetail,
  DiseaseRecordListParams,
  DiseaseSuggestionRequest,
  DiseaseSuggestionResponse,
  DiseaseRecordUpdateRequest,
  DiseaseTreatment,
  DiseaseTreatmentCreateRequest,
  DiseaseTreatmentListParams,
  DiseaseTreatmentUpdateRequest,
} from "../model/types";
import type { DiseaseScope } from "../model/keys";

const diseasePaths = {
  seasonRecords: (seasonId: number, scope: DiseaseScope) =>
    scope === "employee"
      ? `/api/v1/employee/seasons/${seasonId}/disease-records`
      : `/api/v1/seasons/${seasonId}/disease-records`,
  record: (id: number, scope: DiseaseScope) =>
    scope === "employee"
      ? `/api/v1/employee/disease-records/${id}`
      : `/api/v1/disease-records/${id}`,
  recordTreatments: (id: number, scope: DiseaseScope) =>
    scope === "employee"
      ? `/api/v1/employee/disease-records/${id}/treatments`
      : `/api/v1/disease-records/${id}/treatments`,
  treatment: (id: number, scope: DiseaseScope) =>
    scope === "employee"
      ? `/api/v1/employee/disease-treatments/${id}`
      : `/api/v1/disease-treatments/${id}`,
  aiSuggestion: (id: number, scope: DiseaseScope) =>
    scope === "employee"
      ? `/api/v1/employee/disease-records/${id}/ai-suggestion`
      : `/api/v1/disease-records/${id}/ai-suggestion`,
};

export const diseaseApi = {
  listBySeason: async (
    seasonId: number,
    params?: DiseaseRecordListParams,
    scope: DiseaseScope = "farmer",
  ): Promise<PageResponse<DiseaseRecord>> => {
    const validatedParams = params
      ? DiseaseRecordListParamsSchema.parse(params)
      : undefined;
    const response = await httpClient.get(diseasePaths.seasonRecords(seasonId, scope), { params: validatedParams });
    return parsePageResponse(response.data, DiseaseRecordSchema);
  },

  getDetail: async (id: number, scope: DiseaseScope = "farmer"): Promise<DiseaseRecordDetail> => {
    const response = await httpClient.get(diseasePaths.record(id, scope));
    return parseApiResponse(response.data, DiseaseRecordDetailSchema);
  },

  createRecord: async (
    seasonId: number,
    data: DiseaseRecordCreateRequest,
    scope: DiseaseScope = "farmer",
  ): Promise<DiseaseRecord> => {
    const payload = DiseaseRecordCreateRequestSchema.parse(data);
    const response = await httpClient.post(diseasePaths.seasonRecords(seasonId, scope), payload);
    return parseApiResponse(response.data, DiseaseRecordSchema);
  },

  updateRecord: async (
    id: number,
    data: DiseaseRecordUpdateRequest,
    scope: DiseaseScope = "farmer",
  ): Promise<DiseaseRecord> => {
    const payload = DiseaseRecordUpdateRequestSchema.parse(data);
    const response = await httpClient.put(diseasePaths.record(id, scope), payload);
    return parseApiResponse(response.data, DiseaseRecordSchema);
  },

  deleteRecord: async (id: number, scope: DiseaseScope = "farmer"): Promise<void> => {
    await httpClient.delete(diseasePaths.record(id, scope));
  },

  listTreatments: async (
    diseaseRecordId: number,
    params?: DiseaseTreatmentListParams,
    scope: DiseaseScope = "farmer",
  ): Promise<PageResponse<DiseaseTreatment>> => {
    const validatedParams = params
      ? DiseaseTreatmentListParamsSchema.parse(params)
      : undefined;
    const response = await httpClient.get(diseasePaths.recordTreatments(diseaseRecordId, scope), {
      params: validatedParams,
    });
    return parsePageResponse(response.data, DiseaseTreatmentSchema);
  },

  createTreatment: async (
    diseaseRecordId: number,
    data: DiseaseTreatmentCreateRequest,
    scope: DiseaseScope = "farmer",
  ): Promise<DiseaseTreatment> => {
    const payload = DiseaseTreatmentCreateRequestSchema.parse(data);
    const response = await httpClient.post(diseasePaths.recordTreatments(diseaseRecordId, scope), payload);
    return parseApiResponse(response.data, DiseaseTreatmentSchema);
  },

  updateTreatment: async (
    id: number,
    data: DiseaseTreatmentUpdateRequest,
    scope: DiseaseScope = "farmer",
  ): Promise<DiseaseTreatment> => {
    const payload = DiseaseTreatmentUpdateRequestSchema.parse(data);
    const response = await httpClient.put(diseasePaths.treatment(id, scope), payload);
    return parseApiResponse(response.data, DiseaseTreatmentSchema);
  },

  deleteTreatment: async (id: number, scope: DiseaseScope = "farmer"): Promise<void> => {
    await httpClient.delete(diseasePaths.treatment(id, scope));
  },

  requestAiSuggestion: async (
    diseaseRecordId: number,
    data?: DiseaseSuggestionRequest,
    scope: DiseaseScope = "farmer",
  ): Promise<DiseaseSuggestionResponse> => {
    const payload = data ? DiseaseSuggestionRequestSchema.parse(data) : undefined;
    const response = await httpClient.post(diseasePaths.aiSuggestion(diseaseRecordId, scope), payload);
    return parseApiResponse(response.data, DiseaseSuggestionResponseSchema);
  },
};
