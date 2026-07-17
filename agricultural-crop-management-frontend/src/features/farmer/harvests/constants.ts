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

export const QUALITY_GRADE_OPTIONS = [
    { value: "PASSED", label: "Đạt chuẩn (PASSED)" },
    { value: "SUBSTANDARD", label: "Không đạt (SUBSTANDARD)" },
    { value: "REJECTED", label: "Hủy bỏ (REJECTED)" },
];

export const SUBSTANDARD_DISPOSITION_OPTIONS = [
    { value: "SELL_LIVESTOCK_FEED", label: "Bán làm thức ăn chăn nuôi" },
    { value: "COMPOSTING", label: "Ủ phân hữu cơ" },
    { value: "DISCARDED", label: "Tiêu hủy" },
    { value: "SELL_DISCOUNT", label: "Bán hạ giá" },
];

export const PACKAGING_TYPE_OPTIONS = [
    { value: "NONE", label: "Không đóng gói" },
    { value: "BULK_BAG", label: "Bao lớn (Bulk Bag)" },
    { value: "CRATE", label: "Kệ (Crate)" },
    { value: "PALLET_BOX", label: "Thùng Pallet" },
    { value: "BASKET", label: "Rổ/Sọt" },
    { value: "CARTON", label: "Thùng Carton" },
    { value: "NET_BAG", label: "Túi lưới" },
    { value: "VACUUM_BAG", label: "Túi hút chân không" },
    { value: "PUNNET", label: "Hộp nhỏ (Punnet)" },
    { value: "DRUM", label: "Thùng phuy" },
];

export const PROCESSING_TYPE_OPTIONS = [
    { value: "NONE", label: "Chưa sơ chế" },
    { value: "WASHED", label: "Đã rửa sạch" },
    { value: "TRIMMED", label: "Cắt tỉa" },
    { value: "SORTED", label: "Đã phân loại" },
    { value: "CURED", label: "Bảo dưỡng (Cured)" },
    { value: "FUMIGATED", label: "Xông hơi (Fumigated)" },
];
