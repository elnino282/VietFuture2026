import type { DiseaseRecordListParams, DiseaseTreatmentListParams } from "./types";

export type DiseaseScope = "farmer" | "employee";

export const diseaseKeys = {
  all: ["disease"] as const,
  lists: () => [...diseaseKeys.all, "list"] as const,
  listBySeasonBase: (seasonId: number, scope: DiseaseScope = "farmer") =>
    [...diseaseKeys.lists(), scope, "bySeason", seasonId] as const,
  listBySeason: (
    seasonId: number,
    params?: DiseaseRecordListParams,
    scope: DiseaseScope = "farmer",
  ) => [...diseaseKeys.lists(), scope, "bySeason", seasonId, params ?? {}] as const,
  details: () => [...diseaseKeys.all, "detail"] as const,
  detail: (id: number, scope: DiseaseScope = "farmer") => [...diseaseKeys.details(), scope, id] as const,
  treatments: () => [...diseaseKeys.all, "treatments"] as const,
  treatmentListBase: (diseaseRecordId: number, scope: DiseaseScope = "farmer") =>
    [...diseaseKeys.treatments(), scope, "byRecord", diseaseRecordId] as const,
  treatmentList: (
    diseaseRecordId: number,
    params?: DiseaseTreatmentListParams,
    scope: DiseaseScope = "farmer",
  ) => [...diseaseKeys.treatments(), scope, "byRecord", diseaseRecordId, params ?? {}] as const,
};
