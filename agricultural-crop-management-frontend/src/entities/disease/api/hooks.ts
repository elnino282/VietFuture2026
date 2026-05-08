import {
  useMutation,
  useQuery,
  useQueryClient,
  type UseMutationOptions,
  type UseQueryOptions,
} from "@tanstack/react-query";
import type { PageResponse } from "@/shared/api/types";
import { diseaseKeys } from "../model/keys";
import { diseaseApi } from "./client";
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

export const useDiseaseRecords = (
  seasonId: number,
  params?: DiseaseRecordListParams,
  options?: Omit<UseQueryOptions<PageResponse<DiseaseRecord>, Error>, "queryKey" | "queryFn">,
) =>
  useQuery({
    queryKey: diseaseKeys.listBySeason(seasonId, params),
    queryFn: () => diseaseApi.listBySeason(seasonId, params),
    enabled: seasonId > 0,
    staleTime: 5 * 60 * 1000,
    ...options,
  });

export const useDiseaseRecordDetail = (
  id: number,
  options?: Omit<UseQueryOptions<DiseaseRecordDetail, Error>, "queryKey" | "queryFn">,
) =>
  useQuery({
    queryKey: diseaseKeys.detail(id),
    queryFn: () => diseaseApi.getDetail(id),
    enabled: id > 0,
    staleTime: 60 * 1000,
    ...options,
  });

export const useDiseaseTreatments = (
  diseaseRecordId: number,
  params?: DiseaseTreatmentListParams,
  options?: Omit<UseQueryOptions<PageResponse<DiseaseTreatment>, Error>, "queryKey" | "queryFn">,
) =>
  useQuery({
    queryKey: diseaseKeys.treatmentList(diseaseRecordId, params),
    queryFn: () => diseaseApi.listTreatments(diseaseRecordId, params),
    enabled: diseaseRecordId > 0,
    staleTime: 60 * 1000,
    ...options,
  });

export const useDiseaseAiSuggestion = (
  options?: UseMutationOptions<
    DiseaseSuggestionResponse,
    Error,
    { id: number; data?: DiseaseSuggestionRequest }
  >,
) =>
  useMutation({
    mutationFn: ({ id, data }) => diseaseApi.requestAiSuggestion(id, data),
    ...options,
  });

export const useCreateDiseaseRecord = (
  options?: UseMutationOptions<
    DiseaseRecord,
    Error,
    { seasonId: number; data: DiseaseRecordCreateRequest }
  >,
) => {
  const queryClient = useQueryClient();
  const { onSuccess, ...mutationOptions } = options ?? {};

  return useMutation({
    mutationFn: ({ seasonId, data }) => diseaseApi.createRecord(seasonId, data),
    ...mutationOptions,
    onSuccess: (data, variables, onMutateResult, context) => {
      queryClient.invalidateQueries({
        queryKey: diseaseKeys.listBySeasonBase(variables.seasonId),
        exact: false,
      });
      onSuccess?.(data, variables, onMutateResult, context);
    },
  });
};

export const useUpdateDiseaseRecord = (
  options?: UseMutationOptions<
    DiseaseRecord,
    Error,
    { seasonId: number; id: number; data: DiseaseRecordUpdateRequest }
  >,
) => {
  const queryClient = useQueryClient();
  const { onSuccess, ...mutationOptions } = options ?? {};

  return useMutation({
    mutationFn: ({ id, data }) => diseaseApi.updateRecord(id, data),
    ...mutationOptions,
    onSuccess: (data, variables, onMutateResult, context) => {
      queryClient.invalidateQueries({ queryKey: diseaseKeys.detail(variables.id) });
      queryClient.invalidateQueries({
        queryKey: diseaseKeys.listBySeasonBase(variables.seasonId),
        exact: false,
      });
      onSuccess?.(data, variables, onMutateResult, context);
    },
  });
};

export const useDeleteDiseaseRecord = (
  options?: UseMutationOptions<void, Error, { seasonId: number; id: number }>,
) => {
  const queryClient = useQueryClient();
  const { onSuccess, ...mutationOptions } = options ?? {};

  return useMutation({
    mutationFn: ({ id }) => diseaseApi.deleteRecord(id),
    ...mutationOptions,
    onSuccess: (data, variables, onMutateResult, context) => {
      queryClient.invalidateQueries({
        queryKey: diseaseKeys.listBySeasonBase(variables.seasonId),
        exact: false,
      });
      queryClient.removeQueries({ queryKey: diseaseKeys.detail(variables.id) });
      onSuccess?.(data, variables, onMutateResult, context);
    },
  });
};

export const useCreateDiseaseTreatment = (
  options?: UseMutationOptions<
    DiseaseTreatment,
    Error,
    { diseaseRecordId: number; data: DiseaseTreatmentCreateRequest }
  >,
) => {
  const queryClient = useQueryClient();
  const { onSuccess, ...mutationOptions } = options ?? {};

  return useMutation({
    mutationFn: ({ diseaseRecordId, data }) =>
      diseaseApi.createTreatment(diseaseRecordId, data),
    ...mutationOptions,
    onSuccess: (data, variables, onMutateResult, context) => {
      queryClient.invalidateQueries({
        queryKey: diseaseKeys.treatmentListBase(variables.diseaseRecordId),
        exact: false,
      });
      queryClient.invalidateQueries({
        queryKey: diseaseKeys.detail(variables.diseaseRecordId),
      });
      queryClient.invalidateQueries({ queryKey: diseaseKeys.lists(), exact: false });
      onSuccess?.(data, variables, onMutateResult, context);
    },
  });
};

export const useUpdateDiseaseTreatment = (
  options?: UseMutationOptions<
    DiseaseTreatment,
    Error,
    { diseaseRecordId: number; id: number; data: DiseaseTreatmentUpdateRequest }
  >,
) => {
  const queryClient = useQueryClient();
  const { onSuccess, ...mutationOptions } = options ?? {};

  return useMutation({
    mutationFn: ({ id, data }) => diseaseApi.updateTreatment(id, data),
    ...mutationOptions,
    onSuccess: (data, variables, onMutateResult, context) => {
      queryClient.invalidateQueries({
        queryKey: diseaseKeys.treatmentListBase(variables.diseaseRecordId),
        exact: false,
      });
      queryClient.invalidateQueries({
        queryKey: diseaseKeys.detail(variables.diseaseRecordId),
      });
      queryClient.invalidateQueries({ queryKey: diseaseKeys.lists(), exact: false });
      onSuccess?.(data, variables, onMutateResult, context);
    },
  });
};

export const useDeleteDiseaseTreatment = (
  options?: UseMutationOptions<void, Error, { diseaseRecordId: number; id: number }>,
) => {
  const queryClient = useQueryClient();
  const { onSuccess, ...mutationOptions } = options ?? {};

  return useMutation({
    mutationFn: ({ id }) => diseaseApi.deleteTreatment(id),
    ...mutationOptions,
    onSuccess: (data, variables, onMutateResult, context) => {
      queryClient.invalidateQueries({
        queryKey: diseaseKeys.treatmentListBase(variables.diseaseRecordId),
        exact: false,
      });
      queryClient.invalidateQueries({
        queryKey: diseaseKeys.detail(variables.diseaseRecordId),
      });
      queryClient.invalidateQueries({ queryKey: diseaseKeys.lists(), exact: false });
      onSuccess?.(data, variables, onMutateResult, context);
    },
  });
};
