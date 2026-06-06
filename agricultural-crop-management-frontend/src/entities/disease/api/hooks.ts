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
import type { DiseaseScope } from "../model/keys";

const visiblePageRefetchOptions = {
  refetchInterval: 30 * 1000,
  refetchIntervalInBackground: false,
  refetchOnWindowFocus: true,
} as const;

export const useDiseaseRecords = (
  seasonId: number,
  params?: DiseaseRecordListParams,
  options?: Omit<UseQueryOptions<PageResponse<DiseaseRecord>, Error>, "queryKey" | "queryFn">,
  scope: DiseaseScope = "farmer",
) =>
  useQuery({
    queryKey: diseaseKeys.listBySeason(seasonId, params, scope),
    queryFn: () => diseaseApi.listBySeason(seasonId, params, scope),
    enabled: seasonId > 0,
    staleTime: 5 * 60 * 1000,
    ...visiblePageRefetchOptions,
    ...options,
  });

export const useDiseaseRecordDetail = (
  id: number,
  options?: Omit<UseQueryOptions<DiseaseRecordDetail, Error>, "queryKey" | "queryFn">,
  scope: DiseaseScope = "farmer",
) =>
  useQuery({
    queryKey: diseaseKeys.detail(id, scope),
    queryFn: () => diseaseApi.getDetail(id, scope),
    enabled: id > 0,
    staleTime: 60 * 1000,
    refetchOnWindowFocus: true,
    ...options,
  });

export const useDiseaseTreatments = (
  diseaseRecordId: number,
  params?: DiseaseTreatmentListParams,
  options?: Omit<UseQueryOptions<PageResponse<DiseaseTreatment>, Error>, "queryKey" | "queryFn">,
  scope: DiseaseScope = "farmer",
) =>
  useQuery({
    queryKey: diseaseKeys.treatmentList(diseaseRecordId, params, scope),
    queryFn: () => diseaseApi.listTreatments(diseaseRecordId, params, scope),
    enabled: diseaseRecordId > 0,
    staleTime: 60 * 1000,
    ...visiblePageRefetchOptions,
    ...options,
  });

export const useDiseaseAiSuggestion = (
  options?: UseMutationOptions<
    DiseaseSuggestionResponse,
    Error,
    { id: number; data?: DiseaseSuggestionRequest }
  >,
  scope: DiseaseScope = "farmer",
) =>
  useMutation({
    mutationFn: ({ id, data }) => diseaseApi.requestAiSuggestion(id, data, scope),
    ...options,
  });

export const useCreateDiseaseRecord = (
  options?: UseMutationOptions<
    DiseaseRecord,
    Error,
    { seasonId: number; data: DiseaseRecordCreateRequest }
  >,
  scope: DiseaseScope = "farmer",
) => {
  const queryClient = useQueryClient();
  const { onSuccess, ...mutationOptions } = options ?? {};

  return useMutation({
    mutationFn: ({ seasonId, data }) => diseaseApi.createRecord(seasonId, data, scope),
    ...mutationOptions,
    onSuccess: (data, variables, onMutateResult, context) => {
      queryClient.invalidateQueries({
        queryKey: diseaseKeys.listBySeasonBase(variables.seasonId, scope),
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
  scope: DiseaseScope = "farmer",
) => {
  const queryClient = useQueryClient();
  const { onSuccess, ...mutationOptions } = options ?? {};

  return useMutation({
    mutationFn: ({ id, data }) => diseaseApi.updateRecord(id, data, scope),
    ...mutationOptions,
    onSuccess: (data, variables, onMutateResult, context) => {
      queryClient.invalidateQueries({ queryKey: diseaseKeys.detail(variables.id, scope) });
      queryClient.invalidateQueries({
        queryKey: diseaseKeys.listBySeasonBase(variables.seasonId, scope),
        exact: false,
      });
      onSuccess?.(data, variables, onMutateResult, context);
    },
  });
};

export const useDeleteDiseaseRecord = (
  options?: UseMutationOptions<void, Error, { seasonId: number; id: number }>,
  scope: DiseaseScope = "farmer",
) => {
  const queryClient = useQueryClient();
  const { onSuccess, ...mutationOptions } = options ?? {};

  return useMutation({
    mutationFn: ({ id }) => diseaseApi.deleteRecord(id, scope),
    ...mutationOptions,
    onSuccess: (data, variables, onMutateResult, context) => {
      queryClient.invalidateQueries({
        queryKey: diseaseKeys.listBySeasonBase(variables.seasonId, scope),
        exact: false,
      });
      queryClient.removeQueries({ queryKey: diseaseKeys.detail(variables.id, scope) });
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
  scope: DiseaseScope = "farmer",
) => {
  const queryClient = useQueryClient();
  const { onSuccess, ...mutationOptions } = options ?? {};

  return useMutation({
    mutationFn: ({ diseaseRecordId, data }) =>
      diseaseApi.createTreatment(diseaseRecordId, data, scope),
    ...mutationOptions,
    onSuccess: (data, variables, onMutateResult, context) => {
      queryClient.invalidateQueries({
        queryKey: diseaseKeys.treatmentListBase(variables.diseaseRecordId, scope),
        exact: false,
      });
      queryClient.invalidateQueries({
        queryKey: diseaseKeys.detail(variables.diseaseRecordId, scope),
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
  scope: DiseaseScope = "farmer",
) => {
  const queryClient = useQueryClient();
  const { onSuccess, ...mutationOptions } = options ?? {};

  return useMutation({
    mutationFn: ({ id, data }) => diseaseApi.updateTreatment(id, data, scope),
    ...mutationOptions,
    onSuccess: (data, variables, onMutateResult, context) => {
      queryClient.invalidateQueries({
        queryKey: diseaseKeys.treatmentListBase(variables.diseaseRecordId, scope),
        exact: false,
      });
      queryClient.invalidateQueries({
        queryKey: diseaseKeys.detail(variables.diseaseRecordId, scope),
      });
      queryClient.invalidateQueries({ queryKey: diseaseKeys.lists(), exact: false });
      onSuccess?.(data, variables, onMutateResult, context);
    },
  });
};

export const useDeleteDiseaseTreatment = (
  options?: UseMutationOptions<void, Error, { diseaseRecordId: number; id: number }>,
  scope: DiseaseScope = "farmer",
) => {
  const queryClient = useQueryClient();
  const { onSuccess, ...mutationOptions } = options ?? {};

  return useMutation({
    mutationFn: ({ id }) => diseaseApi.deleteTreatment(id, scope),
    ...mutationOptions,
    onSuccess: (data, variables, onMutateResult, context) => {
      queryClient.invalidateQueries({
        queryKey: diseaseKeys.treatmentListBase(variables.diseaseRecordId, scope),
        exact: false,
      });
      queryClient.invalidateQueries({
        queryKey: diseaseKeys.detail(variables.diseaseRecordId, scope),
      });
      queryClient.invalidateQueries({ queryKey: diseaseKeys.lists(), exact: false });
      onSuccess?.(data, variables, onMutateResult, context);
    },
  });
};
