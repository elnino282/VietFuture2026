import type { DiseaseRecordListParams, DiseaseTreatmentListParams } from "./types";

export const diseaseKeys = {
  all: ["disease"] as const,
  lists: () => [...diseaseKeys.all, "list"] as const,
  listBySeasonBase: (seasonId: number) =>
    [...diseaseKeys.lists(), "bySeason", seasonId] as const,
  listBySeason: (seasonId: number, params?: DiseaseRecordListParams) =>
    [...diseaseKeys.lists(), "bySeason", seasonId, params ?? {}] as const,
  details: () => [...diseaseKeys.all, "detail"] as const,
  detail: (id: number) => [...diseaseKeys.details(), id] as const,
  treatments: () => [...diseaseKeys.all, "treatments"] as const,
  treatmentListBase: (diseaseRecordId: number) =>
    [...diseaseKeys.treatments(), "byRecord", diseaseRecordId] as const,
  treatmentList: (diseaseRecordId: number, params?: DiseaseTreatmentListParams) =>
    [...diseaseKeys.treatments(), "byRecord", diseaseRecordId, params ?? {}] as const,
};
