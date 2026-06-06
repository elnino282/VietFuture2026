import type { HarvestGrade } from "./types";

export const GRADE_DISTRIBUTION_COLORS: Record<HarvestGrade, string> = {
    Premium: "var(--primary)",
    A: "var(--secondary)",
    B: "var(--accent)",
    C: "var(--muted-foreground)",
};

export const GRADE_POINTS_MAP: Record<HarvestGrade, number> = {
    Premium: 4,
    A: 3,
    B: 2,
    C: 1,
};

export const GRADE_OPTIONS: Array<{ value: HarvestGrade }> = [
    { value: "Premium" },
    { value: "A" },
    { value: "B" },
    { value: "C" },
];



