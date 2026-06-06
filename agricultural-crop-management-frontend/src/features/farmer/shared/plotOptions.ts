type TranslateFn = (key: string) => string;

export const SOIL_TYPE_OPTIONS = [
  { value: "FERRALSOLS", labelKey: "farms.soilTypes.FERRALSOLS" },
  { value: "CHERNOZEMS", labelKey: "farms.soilTypes.CHERNOZEMS" },
  { value: "FLUVISOLS", labelKey: "farms.soilTypes.FLUVISOLS" },
  { value: "PODZOL", labelKey: "farms.soilTypes.PODZOL" },
  { value: "PEAT", labelKey: "farms.soilTypes.PEAT" },
  { value: "ARENOSOLS", labelKey: "farms.soilTypes.ARENOSOLS" },
] as const;

export const PLOT_STATUS_OPTIONS = [
  { value: "IN_USE", labelKey: "farms.plotStatuses.IN_USE" },
  { value: "IDLE", labelKey: "farms.plotStatuses.IDLE" },
  { value: "AVAILABLE", labelKey: "farms.plotStatuses.AVAILABLE" },
  { value: "FALLOW", labelKey: "farms.plotStatuses.FALLOW" },
  { value: "MAINTENANCE", labelKey: "farms.plotStatuses.MAINTENANCE" },
] as const;

export type SoilTypeCode = (typeof SOIL_TYPE_OPTIONS)[number]["value"];
export type PlotStatusCode = (typeof PLOT_STATUS_OPTIONS)[number]["value"];

const SOIL_TYPE_ALIASES: Record<string, SoilTypeCode> = {
  FERRALSOLS: "FERRALSOLS",
  FERRALSOL: "FERRALSOLS",
  FERALIT: "FERRALSOLS",
  CHERNOZEMS: "CHERNOZEMS",
  CHERNOZEM: "CHERNOZEMS",
  FLUVISOLS: "FLUVISOLS",
  FLUVISOL: "FLUVISOLS",
  PODZOLS: "PODZOL",
  PODZOL: "PODZOL",
  PEAT: "PEAT",
  ARENOSOLS: "ARENOSOLS",
  ARENOSOL: "ARENOSOLS",
};

export function normalizeSoilTypeCode(value?: string | null): SoilTypeCode | null {
  if (!value) return null;
  const normalized = value.trim().toUpperCase().replace(/[\s-]+/g, "_");
  const normalizedAscii = value
    .trim()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/\u0110/g, "D")
    .replace(/\u0111/g, "d")
    .toUpperCase()
    .replace(/[\s-]+/g, "_");

  const directMatch = SOIL_TYPE_ALIASES[normalized] ?? SOIL_TYPE_ALIASES[normalizedAscii];
  if (directMatch) return directMatch;

  if (normalizedAscii.includes("FERRALSOL") || normalizedAscii.includes("FERALIT")) {
    return "FERRALSOLS";
  }
  if (normalizedAscii.includes("CHERNOZEM") || normalizedAscii.includes("DAT_DEN")) {
    return "CHERNOZEMS";
  }
  if (normalizedAscii.includes("FLUVISOL") || normalizedAscii.includes("PHU_SA")) {
    return "FLUVISOLS";
  }
  if (normalizedAscii.includes("PODZOL")) {
    return "PODZOL";
  }
  if (normalizedAscii.includes("PEAT") || normalizedAscii.includes("THAN_BUN")) {
    return "PEAT";
  }
  if (normalizedAscii.includes("ARENOSOL") || normalizedAscii.includes("DAT_CAT")) {
    return "ARENOSOLS";
  }

  return null;
}

export function getSoilTypeLabel(value: string | null | undefined, t: TranslateFn): string {
  const code = normalizeSoilTypeCode(value);
  return code ? t(`farms.soilTypes.${code}`) : value ?? "";
}
