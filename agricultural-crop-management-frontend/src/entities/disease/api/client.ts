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

export const diseaseApi = {
  listBySeason: async (
    seasonId: number,
    params?: DiseaseRecordListParams,
  ): Promise<PageResponse<DiseaseRecord>> => {
    const validatedParams = params
      ? DiseaseRecordListParamsSchema.parse(params)
      : undefined;
    const response = await httpClient.get(
      `/api/v1/seasons/${seasonId}/disease-records`,
      { params: validatedParams },
    );
    return parsePageResponse(response.data, DiseaseRecordSchema);
  },

  getDetail: async (id: number): Promise<DiseaseRecordDetail> => {
    const response = await httpClient.get(`/api/v1/disease-records/${id}`);
    return parseApiResponse(response.data, DiseaseRecordDetailSchema);
  },

  createRecord: async (
    seasonId: number,
    data: DiseaseRecordCreateRequest,
  ): Promise<DiseaseRecord> => {
    const payload = DiseaseRecordCreateRequestSchema.parse(data);
    const response = await httpClient.post(
      `/api/v1/seasons/${seasonId}/disease-records`,
      payload,
    );
    return parseApiResponse(response.data, DiseaseRecordSchema);
  },

  updateRecord: async (
    id: number,
    data: DiseaseRecordUpdateRequest,
  ): Promise<DiseaseRecord> => {
    const payload = DiseaseRecordUpdateRequestSchema.parse(data);
    const response = await httpClient.put(`/api/v1/disease-records/${id}`, payload);
    return parseApiResponse(response.data, DiseaseRecordSchema);
  },

  deleteRecord: async (id: number): Promise<void> => {
    await httpClient.delete(`/api/v1/disease-records/${id}`);
  },

  listTreatments: async (
    diseaseRecordId: number,
    params?: DiseaseTreatmentListParams,
  ): Promise<PageResponse<DiseaseTreatment>> => {
    const validatedParams = params
      ? DiseaseTreatmentListParamsSchema.parse(params)
      : undefined;
    const response = await httpClient.get(
      `/api/v1/disease-records/${diseaseRecordId}/treatments`,
      { params: validatedParams },
    );
    return parsePageResponse(response.data, DiseaseTreatmentSchema);
  },

  createTreatment: async (
    diseaseRecordId: number,
    data: DiseaseTreatmentCreateRequest,
  ): Promise<DiseaseTreatment> => {
    const payload = DiseaseTreatmentCreateRequestSchema.parse(data);
    const response = await httpClient.post(
      `/api/v1/disease-records/${diseaseRecordId}/treatments`,
      payload,
    );
    return parseApiResponse(response.data, DiseaseTreatmentSchema);
  },

  updateTreatment: async (
    id: number,
    data: DiseaseTreatmentUpdateRequest,
  ): Promise<DiseaseTreatment> => {
    const payload = DiseaseTreatmentUpdateRequestSchema.parse(data);
    const response = await httpClient.put(`/api/v1/disease-treatments/${id}`, payload);
    return parseApiResponse(response.data, DiseaseTreatmentSchema);
  },

  deleteTreatment: async (id: number): Promise<void> => {
    await httpClient.delete(`/api/v1/disease-treatments/${id}`);
  },

  requestAiSuggestion: async (
    diseaseRecordId: number,
    data?: DiseaseSuggestionRequest,
  ): Promise<DiseaseSuggestionResponse> => {
    const payload = data ? DiseaseSuggestionRequestSchema.parse(data) : undefined;
    const response = await httpClient.post(
      `/api/v1/disease-records/${diseaseRecordId}/ai-suggestion`,
      payload,
    );
    return parseApiResponse(response.data, DiseaseSuggestionResponseSchema);
  },
};
