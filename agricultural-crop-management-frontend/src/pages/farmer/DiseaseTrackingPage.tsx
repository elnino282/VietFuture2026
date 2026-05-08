import {
  useCreateDiseaseRecord,
  useCreateDiseaseTreatment,
  useDeleteDiseaseRecord,
  useDeleteDiseaseTreatment,
  useDiseaseAiSuggestion,
  useDiseaseRecordDetail,
  useDiseaseRecords,
  useDiseaseTreatments,
  useUpdateDiseaseRecord,
  useUpdateDiseaseTreatment,
  type DiseaseRecord,
  type DiseaseRecordCreateRequest,
  type DiseaseRecordUpdateRequest,
  type DiseaseStatus,
  type DiseaseSuggestionResponse,
  type DiseaseTreatment,
  type DiseaseTreatmentCreateRequest,
  type DiseaseTreatmentUpdateRequest,
} from "@/entities/disease";
import { useSeasonById } from "@/entities/season";
import { useAllSupplyItems, useSupplyLots } from "@/entities/supplies";
import { useOptionalSeason } from "@/shared/contexts";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AsyncState,
  Badge,
  Button,
  Card,
  CardContent,
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  Input,
  Label,
  PageContainer,
  PageHeader,
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
  Switch,
  Textarea,
} from "@/shared/ui";
import {
  AlertCircle,
  Bug,
  ChevronDown,
  ChevronUp,
  FlaskConical,
  Loader2,
  Pencil,
  Plus,
  Search,
  ShieldAlert,
  Sparkles,
  Stethoscope,
  Trash2,
} from "lucide-react";
import { isAxiosError } from "axios";
import { useEffect, useMemo, useState } from "react";
import { useParams } from "react-router-dom";
import { toast } from "sonner";

const DISEASE_SEVERITY_OPTIONS = [
  { value: "LOW", label: "Thap" },
  { value: "MEDIUM", label: "Trung binh" },
  { value: "HIGH", label: "Cao" },
  { value: "CRITICAL", label: "Nghiem trong" },
] as const;

const DISEASE_STATUS_OPTIONS = [
  { value: "OPEN", label: "Moi phat hien" },
  { value: "UNDER_TREATMENT", label: "Dang dieu tri" },
  { value: "MONITORING", label: "Dang theo doi" },
  { value: "RESOLVED", label: "Da on dinh" },
  { value: "CLOSED", label: "Dong ho so" },
] as const;

const TREATMENT_EFFECTIVENESS_OPTIONS = [
  { value: "UNKNOWN", label: "Chua danh gia" },
  { value: "POOR", label: "Kem" },
  { value: "FAIR", label: "Trung binh" },
  { value: "GOOD", label: "Tot" },
  { value: "EXCELLENT", label: "Rat tot" },
] as const;

interface DiseaseRecordFormState {
  diseaseName: string;
  symptomSummary: string;
  severity: string;
  status: string;
  detectedAt: string;
  affectedPlantCount: string;
  affectedAreaPercent: string;
  evidenceUrl: string;
  notes: string;
}

interface DiseaseTreatmentFormState {
  treatedAt: string;
  method: string;
  supplyItemId: string;
  supplyLotId: string;
  quantityUsed: string;
  unit: string;
  dosageNote: string;
  costAmount: string;
  resultNote: string;
  effectiveness: string;
}

const getCurrentDateTimeInput = () => {
  const now = new Date();
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, "0");
  const day = String(now.getDate()).padStart(2, "0");
  const hours = String(now.getHours()).padStart(2, "0");
  const minutes = String(now.getMinutes()).padStart(2, "0");
  return `${year}-${month}-${day}T${hours}:${minutes}`;
};

const initialRecordFormState = (): DiseaseRecordFormState => ({
  diseaseName: "",
  symptomSummary: "",
  severity: "MEDIUM",
  status: "OPEN",
  detectedAt: getCurrentDateTimeInput(),
  affectedPlantCount: "",
  affectedAreaPercent: "",
  evidenceUrl: "",
  notes: "",
});

const initialTreatmentFormState = (): DiseaseTreatmentFormState => ({
  treatedAt: getCurrentDateTimeInput(),
  method: "",
  supplyItemId: "none",
  supplyLotId: "none",
  quantityUsed: "",
  unit: "",
  dosageNote: "",
  costAmount: "",
  resultNote: "",
  effectiveness: "UNKNOWN",
});

const toDateTimeInput = (value?: string | null) => {
  if (!value) return "";
  const directValue = value.replace(" ", "T");
  if (directValue.length >= 16) return directValue.slice(0, 16);
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) return "";
  const year = parsed.getFullYear();
  const month = String(parsed.getMonth() + 1).padStart(2, "0");
  const day = String(parsed.getDate()).padStart(2, "0");
  const hours = String(parsed.getHours()).padStart(2, "0");
  const minutes = String(parsed.getMinutes()).padStart(2, "0");
  return `${year}-${month}-${day}T${hours}:${minutes}`;
};

const toApiDateTime = (value: string) => {
  if (!value) return value;
  return value.length === 16 ? `${value}:00` : value;
};

const formatDateTime = (value?: string | null) => {
  if (!value) return "-";
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) return value;
  return parsed.toLocaleString("vi-VN");
};

const formatNumber = (value?: number | null, suffix?: string) => {
  if (value === null || value === undefined) return "-";
  return `${value.toLocaleString("vi-VN")}${suffix ? ` ${suffix}` : ""}`;
};

interface ParsedNumberInput {
  value: number | undefined;
  isInvalid: boolean;
}

const parseNonNegativeIntegerInput = (value: string): ParsedNumberInput => {
  const trimmed = value.trim();
  if (!trimmed) return { value: undefined, isInvalid: false };
  const numeric = Number(trimmed);
  if (!Number.isFinite(numeric) || !Number.isInteger(numeric) || numeric < 0) {
    return { value: undefined, isInvalid: true };
  }
  return { value: numeric, isInvalid: false };
};

const parseNonNegativeDecimalInput = (value: string): ParsedNumberInput => {
  const trimmed = value.trim().replace(",", ".");
  if (!trimmed) return { value: undefined, isInvalid: false };
  const numeric = Number(trimmed);
  if (!Number.isFinite(numeric) || numeric < 0) {
    return { value: undefined, isInvalid: true };
  }
  return { value: numeric, isInvalid: false };
};

const parseOptionalSelectId = (value: string) => {
  if (!value || value === "none") return undefined;
  const numeric = Number(value);
  if (!Number.isFinite(numeric) || numeric <= 0) return undefined;
  return numeric;
};

const PERMISSION_ERROR_CODES = new Set(["ERR_FORBIDDEN", "ERR_UNAUTHORIZED", "ERR_UNAUTHENTICATED", "NOT_OWNER"]);
const VALIDATION_ERROR_CODES = new Set([
  "ERR_KEY_INVALID",
  "ERR_INVALID_DISEASE_SEVERITY",
  "ERR_INVALID_DISEASE_STATUS",
  "ERR_INVALID_TREATMENT_EFFECTIVENESS",
  "ERR_INVALID_DISEASE_DETECTED_AT",
  "ERR_INVALID_DISEASE_TREATED_AT",
  "ERR_DISEASE_REFERENCE_SEASON_MISMATCH",
  "ERR_DISEASE_SUPPLY_ITEM_LOT_MISMATCH",
]);

interface ApiErrorPayload {
  status?: number;
  code?: string;
  message?: string;
}

const extractApiErrorPayload = (error: unknown): ApiErrorPayload | null => {
  if (!isAxiosError(error)) return null;

  const rawData = error.response?.data;
  if (!rawData || typeof rawData !== "object") {
    return { status: error.response?.status };
  }

  const data = rawData as { status?: unknown; code?: unknown; message?: unknown };
  return {
    status:
      error.response?.status
      ?? (typeof data.status === "number" ? data.status : undefined),
    code: typeof data.code === "string" ? data.code : undefined,
    message: typeof data.message === "string" ? data.message : undefined,
  };
};

const toReadableError = (error: unknown, fallback: string) => {
  const payload = extractApiErrorPayload(error);
  const code = payload?.code;
  const status = payload?.status;

  if (status === 401 || code === "ERR_UNAUTHENTICATED" || code === "ERR_UNAUTHORIZED") {
    return "Phien dang nhap da het han. Vui long dang nhap lai.";
  }

  if (status === 403 || (code && PERMISSION_ERROR_CODES.has(code))) {
    return "Ban khong co quyen truy cap hoac sua du lieu mua vu/benh nay.";
  }

  if (status === 400 || (code && VALIDATION_ERROR_CODES.has(code))) {
    return payload?.message ?? "Du lieu khong hop le. Vui long kiem tra thong tin va thu lai.";
  }

  if (payload?.message) return payload.message;
  if (error instanceof Error && error.message) return error.message;
  return fallback;
};

const getSeverityBadgeVariant = (severity: string) => {
  switch (severity) {
    case "CRITICAL":
    case "HIGH":
      return "destructive";
    case "MEDIUM":
      return "warning";
    case "LOW":
      return "success";
    default:
      return "outline";
  }
};

const getStatusBadgeVariant = (status: string) => {
  switch (status) {
    case "OPEN":
      return "destructive";
    case "UNDER_TREATMENT":
      return "warning";
    case "MONITORING":
      return "info";
    case "RESOLVED":
      return "success";
    case "CLOSED":
      return "secondary";
    default:
      return "outline";
  }
};

const getSeverityLabel = (severity?: string | null) =>
  DISEASE_SEVERITY_OPTIONS.find((option) => option.value === severity)?.label ?? severity ?? "-";

const getStatusLabel = (status?: string | null) =>
  DISEASE_STATUS_OPTIONS.find((option) => option.value === status)?.label ?? status ?? "-";

const getEffectivenessLabel = (effectiveness?: string | null) =>
  TREATMENT_EFFECTIVENESS_OPTIONS.find((option) => option.value === effectiveness)?.label
  ?? effectiveness
  ?? "-";

export function DiseaseTrackingPage() {
  const seasonContext = useOptionalSeason();
  const { seasonId: seasonIdParam } = useParams();

  const workspaceSeasonId = Number(seasonIdParam);
  const hasValidSeasonId = Number.isFinite(workspaceSeasonId) && workspaceSeasonId > 0;
  const selectedSeasonId = hasValidSeasonId ? workspaceSeasonId : 0;

  const [keyword, setKeyword] = useState("");
  const [statusFilter, setStatusFilter] = useState<string>("all");
  const [severityFilter, setSeverityFilter] = useState<string>("all");

  const [isRecordDialogOpen, setIsRecordDialogOpen] = useState(false);
  const [editingRecord, setEditingRecord] = useState<DiseaseRecord | null>(null);
  const [recordForm, setRecordForm] = useState<DiseaseRecordFormState>(initialRecordFormState);
  const [deleteRecordId, setDeleteRecordId] = useState<number | null>(null);

  const [expandedRecordId, setExpandedRecordId] = useState<number | null>(null);

  const [isTreatmentDialogOpen, setIsTreatmentDialogOpen] = useState(false);
  const [activeTreatmentRecordId, setActiveTreatmentRecordId] = useState<number | null>(null);
  const [editingTreatment, setEditingTreatment] = useState<DiseaseTreatment | null>(null);
  const [treatmentForm, setTreatmentForm] = useState<DiseaseTreatmentFormState>(
    initialTreatmentFormState,
  );
  const [deleteTreatmentTarget, setDeleteTreatmentTarget] = useState<{
    diseaseRecordId: number;
    treatmentId: number;
  } | null>(null);
  const [aiAdditionalNote, setAiAdditionalNote] = useState("");
  const [aiIncludeInventory, setAiIncludeInventory] = useState(true);
  const [aiSuggestion, setAiSuggestion] = useState<DiseaseSuggestionResponse | null>(null);
  const [aiSuggestionError, setAiSuggestionError] = useState<string | null>(null);

  useEffect(() => {
    if (!hasValidSeasonId) return;
    if (seasonContext?.selectedSeasonId === selectedSeasonId) return;
    seasonContext?.setSelectedSeasonId(selectedSeasonId);
  }, [hasValidSeasonId, seasonContext, selectedSeasonId]);

  const { data: seasonDetail } = useSeasonById(selectedSeasonId, {
    enabled: hasValidSeasonId,
  });
  const selectedSeasonStatus =
    seasonDetail?.status
    ?? seasonContext?.seasons.find((season) => season.id === selectedSeasonId)?.status
    ?? null;

  const isSeasonWriteLocked =
    selectedSeasonStatus === "COMPLETED"
    || selectedSeasonStatus === "CANCELLED"
    || selectedSeasonStatus === "ARCHIVED";
  const seasonWriteLockReason = isSeasonWriteLocked
    ? "Mua vu da khoa. Khong the tao, sua hoac xoa benh an."
    : undefined;

  const {
    data: recordsData,
    isLoading: isRecordsLoading,
    error: recordsError,
    refetch: refetchRecords,
  } = useDiseaseRecords(
    selectedSeasonId,
    {
      status: statusFilter !== "all" ? (statusFilter as DiseaseStatus) : undefined,
      severity:
        severityFilter !== "all"
          ? (severityFilter as DiseaseRecordCreateRequest["severity"])
          : undefined,
      q: keyword.trim() || undefined,
      page: 0,
      size: 100,
    },
    { enabled: hasValidSeasonId },
  );

  const detailRecordId = expandedRecordId ?? 0;
  const {
    data: detailData,
    isLoading: isDetailLoading,
    error: detailError,
  } = useDiseaseRecordDetail(detailRecordId, {
    enabled: detailRecordId > 0,
  });

  const {
    data: treatmentListData,
    isLoading: isTreatmentListLoading,
    error: treatmentListError,
  } = useDiseaseTreatments(detailRecordId, { page: 0, size: 100 }, {
    enabled: detailRecordId > 0,
  });

  useEffect(() => {
    setAiSuggestion(null);
    setAiSuggestionError(null);
    setAiAdditionalNote("");
    setAiIncludeInventory(true);
  }, [detailRecordId]);

  const { data: supplyItems = [] } = useAllSupplyItems();

  const selectedSupplyItemId = parseOptionalSelectId(treatmentForm.supplyItemId);
  const { data: supplyLotsData } = useSupplyLots(
    selectedSupplyItemId
      ? { itemId: selectedSupplyItemId, page: 0, size: 100 }
      : undefined,
  );
  const supplyLots = supplyLotsData?.items ?? [];

  const createRecordMutation = useCreateDiseaseRecord({
    onSuccess: () => {
      toast.success("Da tao ho so benh.");
      closeRecordDialog();
    },
    onError: (error) => {
      toast.error(toReadableError(error, "Khong the tao ho so benh."));
    },
  });

  const updateRecordMutation = useUpdateDiseaseRecord({
    onSuccess: () => {
      toast.success("Da cap nhat ho so benh.");
      closeRecordDialog();
    },
    onError: (error) => {
      toast.error(toReadableError(error, "Khong the cap nhat ho so benh."));
    },
  });

  const deleteRecordMutation = useDeleteDiseaseRecord({
    onSuccess: () => {
      toast.success("Da xoa ho so benh.");
      setDeleteRecordId(null);
      if (expandedRecordId && expandedRecordId === deleteRecordId) {
        setExpandedRecordId(null);
      }
    },
    onError: (error) => {
      toast.error(toReadableError(error, "Khong the xoa ho so benh."));
    },
  });

  const createTreatmentMutation = useCreateDiseaseTreatment({
    onSuccess: () => {
      toast.success("Da them lich su dieu tri.");
      closeTreatmentDialog();
    },
    onError: (error) => {
      toast.error(toReadableError(error, "Khong the them lich su dieu tri."));
    },
  });

  const updateTreatmentMutation = useUpdateDiseaseTreatment({
    onSuccess: () => {
      toast.success("Da cap nhat lich su dieu tri.");
      closeTreatmentDialog();
    },
    onError: (error) => {
      toast.error(toReadableError(error, "Khong the cap nhat lich su dieu tri."));
    },
  });

  const deleteTreatmentMutation = useDeleteDiseaseTreatment({
    onSuccess: () => {
      toast.success("Da xoa lich su dieu tri.");
      setDeleteTreatmentTarget(null);
    },
    onError: (error) => {
      toast.error(toReadableError(error, "Khong the xoa lich su dieu tri."));
    },
  });

  const aiSuggestionMutation = useDiseaseAiSuggestion({
    onSuccess: (result) => {
      setAiSuggestion(result);
      setAiSuggestionError(null);
    },
    onError: (error) => {
      const message = toReadableError(error, "Khong the tao goi y AI luc nay.");
      setAiSuggestion(null);
      setAiSuggestionError(message);
      toast.error(message);
    },
  });

  const records = recordsData?.items ?? [];
  const treatmentTimeline = treatmentListData?.items ?? [];
  const activeRecordDetail = detailData?.record ?? null;
  const totalTreatmentCost = detailData?.totalTreatmentCost ?? null;
  const aiSuggestionForActiveRecord = aiSuggestion?.diseaseRecordId === detailRecordId
    ? aiSuggestion
    : null;
  const aiDisclaimerText =
    "G\u1ee3i \u00fd ch\u1ec9 mang t\u00ednh tham kh\u1ea3o, kh\u00f4ng thay th\u1ebf t\u01b0 v\u1ea5n chuy\u00ean gia.";
  const isMutating =
    createRecordMutation.isPending
    || updateRecordMutation.isPending
    || deleteRecordMutation.isPending
    || createTreatmentMutation.isPending
    || updateTreatmentMutation.isPending
    || deleteTreatmentMutation.isPending;
  const parsedAffectedPlantCount = parseNonNegativeIntegerInput(recordForm.affectedPlantCount);
  const parsedAffectedAreaPercent = parseNonNegativeDecimalInput(recordForm.affectedAreaPercent);
  const parsedTreatmentQuantityUsed = parseNonNegativeDecimalInput(treatmentForm.quantityUsed);
  const parsedTreatmentCostAmount = parseNonNegativeDecimalInput(treatmentForm.costAmount);
  const isRecordDetectedAtValid =
    Boolean(recordForm.detectedAt)
    && !Number.isNaN(new Date(toApiDateTime(recordForm.detectedAt)).getTime());
  const isTreatmentDateValid =
    Boolean(treatmentForm.treatedAt)
    && !Number.isNaN(new Date(toApiDateTime(treatmentForm.treatedAt)).getTime());
  const isRecordFormSubmittable =
    Boolean(recordForm.diseaseName.trim())
    && isRecordDetectedAtValid
    && !parsedAffectedPlantCount.isInvalid
    && !parsedAffectedAreaPercent.isInvalid;
  const isTreatmentFormSubmittable =
    Boolean(activeTreatmentRecordId)
    && Boolean(treatmentForm.method.trim())
    && isTreatmentDateValid
    && !parsedTreatmentQuantityUsed.isInvalid
    && !parsedTreatmentCostAmount.isInvalid;
  const recordsErrorForView = recordsError
    ? new Error(toReadableError(recordsError, "Khong the tai danh sach ho so benh."))
    : null;
  const detailPanelErrorMessage = toReadableError(
    detailError ?? treatmentListError,
    "Khong the tai chi tiet benh an. Vui long thu lai."
  );

  const summary = useMemo(() => {
    const total = records.length;
    const open = records.filter((item) => item.status === "OPEN").length;
    const underTreatment = records.filter((item) => item.status === "UNDER_TREATMENT").length;
    const resolved = records.filter((item) => item.status === "RESOLVED" || item.status === "CLOSED").length;
    return { total, open, underTreatment, resolved };
  }, [records]);

  const ensureWritable = () => {
    if (!isSeasonWriteLocked) return true;
    toast.error(seasonWriteLockReason);
    return false;
  };

  const openCreateRecordDialog = () => {
    if (!ensureWritable()) return;
    setEditingRecord(null);
    setRecordForm(initialRecordFormState());
    setIsRecordDialogOpen(true);
  };

  const openEditRecordDialog = (record: DiseaseRecord) => {
    if (!ensureWritable()) return;
    setEditingRecord(record);
    setRecordForm({
      diseaseName: record.diseaseName ?? "",
      symptomSummary: record.symptomSummary ?? "",
      severity: record.severity ?? "MEDIUM",
      status: record.status ?? "OPEN",
      detectedAt: toDateTimeInput(record.detectedAt),
      affectedPlantCount:
        record.affectedPlantCount !== undefined && record.affectedPlantCount !== null
          ? String(record.affectedPlantCount)
          : "",
      affectedAreaPercent:
        record.affectedAreaValue !== undefined && record.affectedAreaValue !== null
          ? String(record.affectedAreaValue)
          : "",
      evidenceUrl: record.evidenceUrl ?? "",
      notes: record.notes ?? "",
    });
    setIsRecordDialogOpen(true);
  };

  const closeRecordDialog = () => {
    setIsRecordDialogOpen(false);
    setEditingRecord(null);
    setRecordForm(initialRecordFormState());
  };

  const handleSubmitRecord = () => {
    if (!ensureWritable()) return;
    if (!recordForm.diseaseName.trim()) {
      toast.error("Ten benh bat buoc.");
      return;
    }
    if (!recordForm.detectedAt) {
      toast.error("Thoi diem phat hien bat buoc.");
      return;
    }
    if (!isRecordDetectedAtValid) {
      toast.error("Thoi diem phat hien khong hop le.");
      return;
    }
    if (parsedAffectedPlantCount.isInvalid) {
      toast.error("So cay bi anh huong phai la so nguyen >= 0.");
      return;
    }
    if (parsedAffectedAreaPercent.isInvalid) {
      toast.error("Ty le dien tich anh huong phai la so >= 0.");
      return;
    }
    const affectedAreaValue = parsedAffectedAreaPercent.value;

    const payloadBase: DiseaseRecordCreateRequest = {
      diseaseName: recordForm.diseaseName.trim(),
      symptomSummary: recordForm.symptomSummary.trim() || undefined,
      severity: recordForm.severity as DiseaseRecordCreateRequest["severity"],
      status: recordForm.status as DiseaseRecordCreateRequest["status"],
      detectedAt: toApiDateTime(recordForm.detectedAt),
      affectedPlantCount: parsedAffectedPlantCount.value,
      affectedAreaValue,
      affectedAreaUnit: affectedAreaValue !== undefined ? "%" : undefined,
      evidenceUrl: recordForm.evidenceUrl.trim() || undefined,
      notes: recordForm.notes.trim() || undefined,
    };

    if (editingRecord) {
      const updatePayload: DiseaseRecordUpdateRequest = payloadBase;
      updateRecordMutation.mutate({
        seasonId: selectedSeasonId,
        id: editingRecord.id,
        data: updatePayload,
      });
      return;
    }

    createRecordMutation.mutate({
      seasonId: selectedSeasonId,
      data: payloadBase,
    });
  };

  const handleDeleteRecord = () => {
    if (!ensureWritable()) {
      setDeleteRecordId(null);
      return;
    }
    if (!deleteRecordId) return;
    deleteRecordMutation.mutate({
      seasonId: selectedSeasonId,
      id: deleteRecordId,
    });
  };

  const toggleRecordDetail = (recordId: number) => {
    setExpandedRecordId((previous) => (previous === recordId ? null : recordId));
  };

  const handleGenerateAiSuggestion = (recordId: number) => {
    const trimmedAdditionalNote = aiAdditionalNote.trim();
    if (trimmedAdditionalNote.length > 4000) {
      const message = "Additional note toi da 4000 ky tu.";
      setAiSuggestion(null);
      setAiSuggestionError(message);
      toast.error(message);
      return;
    }
    setAiSuggestion(null);
    setAiSuggestionError(null);
    aiSuggestionMutation.mutate({
      id: recordId,
      data: {
        includeInventory: aiIncludeInventory,
        additionalNote: trimmedAdditionalNote || undefined,
      },
    });
  };

  const openCreateTreatmentDialog = (recordId: number) => {
    if (!ensureWritable()) return;
    setActiveTreatmentRecordId(recordId);
    setEditingTreatment(null);
    setTreatmentForm(initialTreatmentFormState());
    setIsTreatmentDialogOpen(true);
  };

  const openEditTreatmentDialog = (recordId: number, treatment: DiseaseTreatment) => {
    if (!ensureWritable()) return;
    setActiveTreatmentRecordId(recordId);
    setEditingTreatment(treatment);
    setTreatmentForm({
      treatedAt: toDateTimeInput(treatment.treatedAt),
      method: treatment.method ?? "",
      supplyItemId:
        treatment.supplyItemId !== undefined && treatment.supplyItemId !== null
          ? String(treatment.supplyItemId)
          : "none",
      supplyLotId:
        treatment.supplyLotId !== undefined && treatment.supplyLotId !== null
          ? String(treatment.supplyLotId)
          : "none",
      quantityUsed:
        treatment.quantityUsed !== undefined && treatment.quantityUsed !== null
          ? String(treatment.quantityUsed)
          : "",
      unit: treatment.unit ?? "",
      dosageNote: treatment.notes ?? "",
      costAmount:
        treatment.costAmount !== undefined && treatment.costAmount !== null
          ? String(treatment.costAmount)
          : "",
      resultNote: treatment.resultSummary ?? "",
      effectiveness: treatment.effectiveness ?? "UNKNOWN",
    });
    setIsTreatmentDialogOpen(true);
  };

  const closeTreatmentDialog = () => {
    setIsTreatmentDialogOpen(false);
    setActiveTreatmentRecordId(null);
    setEditingTreatment(null);
    setTreatmentForm(initialTreatmentFormState());
  };

  const handleSubmitTreatment = () => {
    if (!ensureWritable()) return;
    if (!activeTreatmentRecordId) {
      toast.error("Khong tim thay ho so benh de cap nhat dieu tri.");
      return;
    }
    if (!treatmentForm.treatedAt) {
      toast.error("Ngay dieu tri bat buoc.");
      return;
    }
    if (!treatmentForm.method.trim()) {
      toast.error("Phuong phap dieu tri bat buoc.");
      return;
    }
    if (!isTreatmentDateValid) {
      toast.error("Ngay dieu tri khong hop le.");
      return;
    }
    if (parsedTreatmentQuantityUsed.isInvalid) {
      toast.error("So luong su dung phai la so >= 0.");
      return;
    }
    if (parsedTreatmentCostAmount.isInvalid) {
      toast.error("Chi phi dieu tri phai la so >= 0.");
      return;
    }

    const payloadBase: DiseaseTreatmentCreateRequest = {
      treatedAt: toApiDateTime(treatmentForm.treatedAt),
      method: treatmentForm.method.trim(),
      supplyItemId: parseOptionalSelectId(treatmentForm.supplyItemId),
      supplyLotId: parseOptionalSelectId(treatmentForm.supplyLotId),
      quantityUsed: parsedTreatmentQuantityUsed.value,
      unit: treatmentForm.unit.trim() || undefined,
      notes: treatmentForm.dosageNote.trim() || undefined,
      costAmount: parsedTreatmentCostAmount.value,
      resultSummary: treatmentForm.resultNote.trim() || undefined,
      effectiveness: treatmentForm.effectiveness as DiseaseTreatmentCreateRequest["effectiveness"],
    };

    if (editingTreatment) {
      const updatePayload: DiseaseTreatmentUpdateRequest = payloadBase;
      updateTreatmentMutation.mutate({
        diseaseRecordId: activeTreatmentRecordId,
        id: editingTreatment.id,
        data: updatePayload,
      });
      return;
    }

    createTreatmentMutation.mutate({
      diseaseRecordId: activeTreatmentRecordId,
      data: payloadBase,
    });
  };

  const handleDeleteTreatment = () => {
    if (!ensureWritable()) {
      setDeleteTreatmentTarget(null);
      return;
    }
    if (!deleteTreatmentTarget) return;
    deleteTreatmentMutation.mutate({
      diseaseRecordId: deleteTreatmentTarget.diseaseRecordId,
      id: deleteTreatmentTarget.treatmentId,
    });
  };

  if (!hasValidSeasonId) {
    return (
      <PageContainer>
        <Card className="border border-destructive/20 bg-destructive/5">
          <CardContent className="py-6 text-sm text-destructive">
            Mua vu khong hop le.
          </CardContent>
        </Card>
      </PageContainer>
    );
  }

  return (
    <PageContainer>
      <Card className="mb-6 border border-border rounded-xl shadow-sm">
        <CardContent className="px-6 py-4">
          <PageHeader
            className="mb-0"
            icon={<Bug className="w-8 h-8" />}
            title="Theo doi dich benh"
            subtitle="Quan ly lich su benh va qua trinh dieu tri trong mua vu."
            actions={(
              <Button
                onClick={openCreateRecordDialog}
                disabled={isSeasonWriteLocked}
                title={isSeasonWriteLocked ? seasonWriteLockReason : undefined}
              >
                <Plus className="w-4 h-4 mr-2" />
                Them ho so benh
              </Button>
            )}
          />
        </CardContent>
      </Card>

      <Card className="mb-6 border border-border rounded-xl shadow-sm">
        <CardContent className="px-6 py-4 space-y-4">
          <div className="rounded-lg border border-border bg-card px-3 py-2 text-sm">
            Mua vu: <span className="font-medium">{seasonDetail?.seasonName ?? `#${selectedSeasonId}`}</span>
          </div>

          {isSeasonWriteLocked && (
            <div className="rounded-lg border border-amber-300 bg-amber-50 px-3 py-2 text-sm text-amber-900">
              {seasonWriteLockReason}
            </div>
          )}

          <div className="grid grid-cols-1 gap-3 md:grid-cols-4">
            <div className="rounded-lg border border-border bg-card px-3 py-3">
              <p className="text-sm text-muted-foreground">Tong ho so</p>
              <p className="text-2xl font-semibold">{summary.total}</p>
            </div>
            <div className="rounded-lg border border-border bg-card px-3 py-3">
              <p className="text-sm text-muted-foreground">Moi phat hien</p>
              <p className="text-2xl font-semibold">{summary.open}</p>
            </div>
            <div className="rounded-lg border border-border bg-card px-3 py-3">
              <p className="text-sm text-muted-foreground">Dang dieu tri</p>
              <p className="text-2xl font-semibold">{summary.underTreatment}</p>
            </div>
            <div className="rounded-lg border border-border bg-card px-3 py-3">
              <p className="text-sm text-muted-foreground">Da on dinh / dong</p>
              <p className="text-2xl font-semibold">{summary.resolved}</p>
            </div>
          </div>

          <div className="flex flex-wrap items-center gap-3">
            <div className="relative w-full md:w-[320px]">
              <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
              <Input
                value={keyword}
                onChange={(event) => setKeyword(event.target.value)}
                className="pl-10"
                placeholder="Tim theo ten benh/trieu chung..."
              />
            </div>

            <Select value={statusFilter} onValueChange={setStatusFilter}>
              <SelectTrigger className="w-full md:w-[220px]">
                <SelectValue placeholder="Trang thai" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">Tat ca trang thai</SelectItem>
                {DISEASE_STATUS_OPTIONS.map((option) => (
                  <SelectItem key={option.value} value={option.value}>
                    {option.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>

            <Select value={severityFilter} onValueChange={setSeverityFilter}>
              <SelectTrigger className="w-full md:w-[220px]">
                <SelectValue placeholder="Muc do" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">Tat ca muc do</SelectItem>
                {DISEASE_SEVERITY_OPTIONS.map((option) => (
                  <SelectItem key={option.value} value={option.value}>
                    {option.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>

            <Button variant="outline" onClick={() => refetchRecords()}>
              Lam moi
            </Button>
          </div>
        </CardContent>
      </Card>

      <Card className="border border-border rounded-xl shadow-sm">
        <CardContent className="px-6 py-4">
          <AsyncState
            isLoading={isRecordsLoading}
            isEmpty={records.length === 0}
            error={recordsErrorForView}
            onRetry={() => refetchRecords()}
            loadingText="Dang tai ho so benh..."
            emptyIcon={<ShieldAlert className="w-6 h-6 text-muted-foreground" />}
            emptyTitle="Chua co ho so benh nao trong mua vu"
            emptyDescription="Bat dau bang cach them ho so benh dau tien de theo doi qua trinh xu ly."
            emptyAction={(
              <Button
                onClick={openCreateRecordDialog}
                disabled={isSeasonWriteLocked}
              >
                <Plus className="w-4 h-4 mr-2" />
                Them ho so benh
              </Button>
            )}
          >
            <div className="space-y-4">
              {records.map((record) => {
                const isExpanded = expandedRecordId === record.id;
                const usingDetail = isExpanded && activeRecordDetail && activeRecordDetail.id === record.id;
                const recordToRender = usingDetail ? activeRecordDetail : record;
                return (
                  <div key={record.id} className="rounded-xl border border-border bg-card p-4">
                    <div className="flex flex-wrap items-start justify-between gap-3">
                      <div className="space-y-2">
                        <div className="flex items-center gap-2 flex-wrap">
                          <h3 className="text-lg font-semibold text-foreground">
                            {recordToRender.diseaseName}
                          </h3>
                          <Badge variant={getSeverityBadgeVariant(recordToRender.severity)}>
                            {getSeverityLabel(recordToRender.severity)}
                          </Badge>
                          <Badge variant={getStatusBadgeVariant(recordToRender.status)}>
                            {getStatusLabel(recordToRender.status)}
                          </Badge>
                        </div>
                        <p className="text-sm text-muted-foreground">
                          Phat hien: {formatDateTime(recordToRender.detectedAt)}
                        </p>
                        {recordToRender.symptomSummary && (
                          <p className="text-sm text-foreground">{recordToRender.symptomSummary}</p>
                        )}
                        <div className="flex flex-wrap gap-4 text-sm text-muted-foreground">
                          <span>So cay anh huong: {formatNumber(recordToRender.affectedPlantCount)}</span>
                          <span>
                            Dien tich anh huong:
                            {" "}
                            {recordToRender.affectedAreaValue !== undefined && recordToRender.affectedAreaValue !== null
                              ? `${recordToRender.affectedAreaValue} ${recordToRender.affectedAreaUnit ?? ""}`.trim()
                              : "-"}
                          </span>
                          <span>Lan dieu tri: {recordToRender.treatmentCount ?? 0}</span>
                        </div>
                      </div>

                      <div className="flex flex-wrap items-center gap-2">
                        <Button
                          variant="outline"
                          onClick={() => openCreateTreatmentDialog(record.id)}
                          disabled={isSeasonWriteLocked}
                          title={isSeasonWriteLocked ? seasonWriteLockReason : undefined}
                        >
                          <FlaskConical className="w-4 h-4 mr-2" />
                          Them dieu tri
                        </Button>
                        <Button
                          variant="outline"
                          onClick={() => openEditRecordDialog(record)}
                          disabled={isSeasonWriteLocked}
                          title={isSeasonWriteLocked ? seasonWriteLockReason : undefined}
                        >
                          <Pencil className="w-4 h-4 mr-2" />
                          Sua
                        </Button>
                        <Button
                          variant="outline"
                          className="text-destructive"
                          onClick={() => setDeleteRecordId(record.id)}
                          disabled={isSeasonWriteLocked}
                          title={isSeasonWriteLocked ? seasonWriteLockReason : undefined}
                        >
                          <Trash2 className="w-4 h-4 mr-2" />
                          Xoa
                        </Button>
                        <Button
                          variant="ghost"
                          onClick={() => toggleRecordDetail(record.id)}
                        >
                          {isExpanded ? (
                            <>
                              An chi tiet
                              <ChevronUp className="w-4 h-4 ml-2" />
                            </>
                          ) : (
                            <>
                              Xem chi tiet
                              <ChevronDown className="w-4 h-4 ml-2" />
                            </>
                          )}
                        </Button>
                      </div>
                    </div>

                    {isExpanded && (
                      <div className="mt-4 rounded-lg border border-border bg-muted/20 p-4 space-y-4">
                        {isDetailLoading || isTreatmentListLoading ? (
                          <div className="flex items-center gap-2 text-sm text-muted-foreground">
                            <Loader2 className="w-4 h-4 animate-spin" />
                            Dang tai chi tiet benh va lich su dieu tri...
                          </div>
                        ) : detailError || treatmentListError ? (
                          <div className="rounded-md border border-destructive/20 bg-destructive/5 p-3 text-sm text-destructive">
                            {detailPanelErrorMessage}
                          </div>
                        ) : (
                          <div className="space-y-4">
                            <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
                              <div className="rounded-lg border border-border bg-card p-3">
                                <p className="text-xs text-muted-foreground">Nguoi ghi nhan</p>
                                <p className="text-sm font-medium">
                                  {recordToRender.reportedByUsername ?? "-"}
                                </p>
                              </div>
                              <div className="rounded-lg border border-border bg-card p-3">
                                <p className="text-xs text-muted-foreground">Tong chi phi dieu tri</p>
                                <p className="text-sm font-medium">
                                  {totalTreatmentCost !== null && totalTreatmentCost !== undefined
                                    ? `${totalTreatmentCost.toLocaleString("vi-VN")} VND`
                                    : "-"}
                                </p>
                              </div>
                              <div className="rounded-lg border border-border bg-card p-3">
                                <p className="text-xs text-muted-foreground">Lan dieu tri gan nhat</p>
                                <p className="text-sm font-medium">
                                  {formatDateTime(detailData?.latestTreatmentAt)}
                                </p>
                              </div>
                              <div className="rounded-lg border border-border bg-card p-3">
                                <p className="text-xs text-muted-foreground">Evidence URL</p>
                                <p className="text-sm break-all">
                                  {recordToRender.evidenceUrl ?? "-"}
                                </p>
                              </div>
                            </div>

                            {recordToRender.notes && (
                              <div className="rounded-lg border border-border bg-card p-3">
                                <p className="text-xs text-muted-foreground mb-1">Ghi chu ca benh</p>
                                <p className="text-sm">{recordToRender.notes}</p>
                              </div>
                            )}

                            <div className="rounded-lg border border-border bg-card p-3 space-y-3">
                              <div className="flex flex-wrap items-start justify-between gap-3">
                                <div>
                                  <h4 className="text-sm font-semibold flex items-center gap-2">
                                    <Sparkles className="w-4 h-4" />
                                    Goi y xu ly bang AI
                                  </h4>
                                  <p className="text-xs text-muted-foreground mt-1">
                                    AI chi ho tro quyet dinh tham khao. Khong tao treatment tu dong.
                                  </p>
                                </div>
                                <Button
                                  variant="outline"
                                  size="sm"
                                  onClick={() => handleGenerateAiSuggestion(record.id)}
                                  disabled={aiSuggestionMutation.isPending}
                                >
                                  {aiSuggestionMutation.isPending ? (
                                    <Loader2 className="w-4 h-4 mr-1 animate-spin" />
                                  ) : (
                                    <Sparkles className="w-4 h-4 mr-1" />
                                  )}
                                  Goi y xu ly bang AI
                                </Button>
                              </div>

                              <div className="space-y-2">
                                <Label htmlFor={`ai-note-${record.id}`}>Additional note (tuy chon)</Label>
                                <Textarea
                                  id={`ai-note-${record.id}`}
                                  rows={2}
                                  value={aiAdditionalNote}
                                  onChange={(event) => setAiAdditionalNote(event.target.value)}
                                  placeholder="Vi du: Da phun thuoc 1 lan truoc do, tinh hinh chua cai thien..."
                                />
                              </div>

                              <div className="rounded-md border border-border px-3 py-2 flex items-center justify-between gap-3">
                                <div>
                                  <p className="text-sm font-medium">Bao gom context ton kho</p>
                                  <p className="text-xs text-muted-foreground">
                                    Bat de AI chi de xuat vat tu trong danh muc ton kho noi bo.
                                  </p>
                                </div>
                                <Switch
                                  checked={aiIncludeInventory}
                                  onCheckedChange={(checked) => setAiIncludeInventory(Boolean(checked))}
                                  aria-label="Bao gom inventory context"
                                />
                              </div>

                              {aiSuggestionError && (
                                <div className="rounded-md border border-destructive/20 bg-destructive/5 px-3 py-2 text-sm text-destructive">
                                  Khong the tao goi y AI: {aiSuggestionError}
                                </div>
                              )}

                              {aiSuggestionForActiveRecord && (
                                <div className="rounded-md border border-primary/20 bg-primary/5 p-3 space-y-2">
                                  <p className="text-xs text-muted-foreground">
                                    Tao luc: {formatDateTime(aiSuggestionForActiveRecord.generatedAt)}
                                  </p>
                                  <p className="text-sm whitespace-pre-wrap leading-6">
                                    {aiSuggestionForActiveRecord.suggestionText}
                                  </p>
                                  <div className="rounded-md border border-amber-300 bg-amber-50 px-3 py-2 text-xs text-amber-900">
                                    {aiSuggestionForActiveRecord.warning ?? aiDisclaimerText}
                                  </div>
                                  <p className="text-xs text-muted-foreground">
                                    {aiDisclaimerText}
                                  </p>
                                </div>
                              )}
                            </div>

                            <div className="space-y-3">
                              <div className="flex items-center justify-between">
                                <h4 className="text-sm font-semibold flex items-center gap-2">
                                  <Stethoscope className="w-4 h-4" />
                                  Timeline dieu tri
                                </h4>
                                <Button
                                  variant="outline"
                                  size="sm"
                                  onClick={() => openCreateTreatmentDialog(record.id)}
                                  disabled={isSeasonWriteLocked}
                                  title={isSeasonWriteLocked ? seasonWriteLockReason : undefined}
                                >
                                  <Plus className="w-4 h-4 mr-1" />
                                  Them
                                </Button>
                              </div>

                              {treatmentTimeline.length === 0 ? (
                                <div className="rounded-md border border-dashed border-border p-4 text-sm text-muted-foreground">
                                  Chua co lich su dieu tri.
                                </div>
                              ) : (
                                <div className="space-y-2">
                                  {treatmentTimeline.map((treatment) => (
                                    <div
                                      key={treatment.id}
                                      className="rounded-lg border border-border bg-card p-3"
                                    >
                                      <div className="flex flex-wrap items-start justify-between gap-3">
                                        <div className="space-y-1">
                                          <p className="text-sm font-medium">
                                            {formatDateTime(treatment.treatedAt)} - {treatment.method}
                                          </p>
                                          <p className="text-xs text-muted-foreground">
                                            Hieu qua: {getEffectivenessLabel(treatment.effectiveness)}
                                          </p>
                                          <p className="text-xs text-muted-foreground">
                                            Vat tu:
                                            {" "}
                                            {treatment.supplyItemName
                                              ?? treatment.materialName
                                              ?? "-"}
                                            {treatment.batchCode ? ` (Lo ${treatment.batchCode})` : ""}
                                          </p>
                                          <p className="text-xs text-muted-foreground">
                                            So luong:
                                            {" "}
                                            {treatment.quantityUsed !== undefined && treatment.quantityUsed !== null
                                              ? `${treatment.quantityUsed} ${treatment.unit ?? ""}`.trim()
                                              : "-"}
                                          </p>
                                          <p className="text-xs text-muted-foreground">
                                            Chi phi:
                                            {" "}
                                            {treatment.costAmount !== undefined && treatment.costAmount !== null
                                              ? `${treatment.costAmount.toLocaleString("vi-VN")} VND`
                                              : "-"}
                                          </p>
                                        </div>
                                        <div className="flex gap-2">
                                          <Button
                                            variant="ghost"
                                            size="icon"
                                            onClick={() => openEditTreatmentDialog(record.id, treatment)}
                                            disabled={isSeasonWriteLocked}
                                            title={isSeasonWriteLocked ? seasonWriteLockReason : undefined}
                                          >
                                            <Pencil className="w-4 h-4" />
                                          </Button>
                                          <Button
                                            variant="ghost"
                                            size="icon"
                                            className="text-destructive"
                                            onClick={() => setDeleteTreatmentTarget({
                                              diseaseRecordId: record.id,
                                              treatmentId: treatment.id,
                                            })}
                                            disabled={isSeasonWriteLocked}
                                            title={isSeasonWriteLocked ? seasonWriteLockReason : undefined}
                                          >
                                            <Trash2 className="w-4 h-4" />
                                          </Button>
                                        </div>
                                      </div>
                                      {treatment.resultSummary && (
                                        <p className="text-sm mt-2">{treatment.resultSummary}</p>
                                      )}
                                      {treatment.notes && (
                                        <p className="text-xs mt-2 text-muted-foreground">
                                          Dosage note: {treatment.notes}
                                        </p>
                                      )}
                                    </div>
                                  ))}
                                </div>
                              )}
                            </div>
                          </div>
                        )}
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          </AsyncState>
        </CardContent>
      </Card>

      <Dialog open={isRecordDialogOpen} onOpenChange={setIsRecordDialogOpen}>
        <DialogContent className="sm:max-w-2xl">
          <DialogHeader>
            <DialogTitle>{editingRecord ? "Cap nhat ho so benh" : "Tao ho so benh moi"}</DialogTitle>
            <DialogDescription>
              Ghi nhan thong tin phat hien benh de theo doi xu ly trong mua vu.
            </DialogDescription>
          </DialogHeader>

          <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
            <div className="space-y-2 md:col-span-2">
              <Label htmlFor="disease-name">
                Ten benh <span className="text-destructive">*</span>
              </Label>
              <Input
                id="disease-name"
                value={recordForm.diseaseName}
                onChange={(event) =>
                  setRecordForm((previous) => ({ ...previous, diseaseName: event.target.value }))
                }
                placeholder="Vi du: Dao on la"
              />
            </div>

            <div className="space-y-2 md:col-span-2">
              <Label htmlFor="disease-symptoms">Trieu chung</Label>
              <Textarea
                id="disease-symptoms"
                rows={3}
                value={recordForm.symptomSummary}
                onChange={(event) =>
                  setRecordForm((previous) => ({ ...previous, symptomSummary: event.target.value }))
                }
                placeholder="Mo ta trieu chung quan sat duoc..."
              />
            </div>

            <div className="space-y-2">
              <Label>Muc do</Label>
              <Select
                value={recordForm.severity}
                onValueChange={(value) =>
                  setRecordForm((previous) => ({ ...previous, severity: value }))
                }
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {DISEASE_SEVERITY_OPTIONS.map((option) => (
                    <SelectItem key={option.value} value={option.value}>
                      {option.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-2">
              <Label>Trang thai</Label>
              <Select
                value={recordForm.status}
                onValueChange={(value) =>
                  setRecordForm((previous) => ({ ...previous, status: value }))
                }
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {DISEASE_STATUS_OPTIONS.map((option) => (
                    <SelectItem key={option.value} value={option.value}>
                      {option.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-2">
              <Label htmlFor="disease-detected-at">
                Thoi diem phat hien <span className="text-destructive">*</span>
              </Label>
              <Input
                id="disease-detected-at"
                type="datetime-local"
                value={recordForm.detectedAt}
                onChange={(event) =>
                  setRecordForm((previous) => ({ ...previous, detectedAt: event.target.value }))
                }
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="disease-affected-count">So cay bi anh huong</Label>
              <Input
                id="disease-affected-count"
                inputMode="numeric"
                value={recordForm.affectedPlantCount}
                onChange={(event) =>
                  setRecordForm((previous) => ({ ...previous, affectedPlantCount: event.target.value }))
                }
                placeholder="Vi du: 120"
              />
              {parsedAffectedPlantCount.isInvalid && (
                <p className="text-xs text-destructive">
                  Vui long nhap so nguyen {">= 0"}.
                </p>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor="disease-affected-area">Ty le dien tich anh huong (%)</Label>
              <Input
                id="disease-affected-area"
                inputMode="decimal"
                value={recordForm.affectedAreaPercent}
                onChange={(event) =>
                  setRecordForm((previous) => ({ ...previous, affectedAreaPercent: event.target.value }))
                }
                placeholder="Vi du: 35"
              />
              {parsedAffectedAreaPercent.isInvalid && (
                <p className="text-xs text-destructive">
                  Vui long nhap so {">= 0"}.
                </p>
              )}
            </div>

            <div className="space-y-2 md:col-span-2">
              <Label htmlFor="disease-evidence-url">Evidence URL</Label>
              <Input
                id="disease-evidence-url"
                value={recordForm.evidenceUrl}
                onChange={(event) =>
                  setRecordForm((previous) => ({ ...previous, evidenceUrl: event.target.value }))
                }
                placeholder="https://..."
              />
            </div>

            <div className="space-y-2 md:col-span-2">
              <Label htmlFor="disease-notes">Ghi chu</Label>
              <Textarea
                id="disease-notes"
                rows={3}
                value={recordForm.notes}
                onChange={(event) =>
                  setRecordForm((previous) => ({ ...previous, notes: event.target.value }))
                }
                placeholder="Thong tin bo sung cho ca benh..."
              />
            </div>
          </div>

          <DialogFooter>
            <Button variant="outline" onClick={closeRecordDialog}>
              Huy
            </Button>
            <Button
              onClick={handleSubmitRecord}
              disabled={isMutating || !isRecordFormSubmittable}
            >
              {isMutating && <Loader2 className="w-4 h-4 mr-2 animate-spin" />}
              {editingRecord ? "Cap nhat" : "Tao moi"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog open={isTreatmentDialogOpen} onOpenChange={setIsTreatmentDialogOpen}>
        <DialogContent className="sm:max-w-2xl">
          <DialogHeader>
            <DialogTitle>{editingTreatment ? "Cap nhat dieu tri" : "Them lich su dieu tri"}</DialogTitle>
            <DialogDescription>
              Ghi nhan vat tu su dung, lieu luong, chi phi va ket qua dieu tri.
            </DialogDescription>
          </DialogHeader>

          <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
            <div className="space-y-2">
              <Label htmlFor="treatment-date">
                Ngay dieu tri <span className="text-destructive">*</span>
              </Label>
              <Input
                id="treatment-date"
                type="datetime-local"
                value={treatmentForm.treatedAt}
                onChange={(event) =>
                  setTreatmentForm((previous) => ({ ...previous, treatedAt: event.target.value }))
                }
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="treatment-method">
                Phuong phap <span className="text-destructive">*</span>
              </Label>
              <Input
                id="treatment-method"
                value={treatmentForm.method}
                onChange={(event) =>
                  setTreatmentForm((previous) => ({ ...previous, method: event.target.value }))
                }
                placeholder="Phun, cat tia, xu ly sinh hoc..."
              />
            </div>

            <div className="space-y-2">
              <Label>Supply item</Label>
              <Select
                value={treatmentForm.supplyItemId}
                onValueChange={(value) =>
                  setTreatmentForm((previous) => ({
                    ...previous,
                    supplyItemId: value,
                    supplyLotId: "none",
                  }))
                }
              >
                <SelectTrigger>
                  <SelectValue placeholder="Chon vat tu" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="none">Khong chon</SelectItem>
                  {supplyItems.map((item) => (
                    <SelectItem key={item.id} value={String(item.id)}>
                      {item.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-2">
              <Label>Supply lot</Label>
              <Select
                value={treatmentForm.supplyLotId}
                onValueChange={(value) =>
                  setTreatmentForm((previous) => ({ ...previous, supplyLotId: value }))
                }
                disabled={!selectedSupplyItemId}
              >
                <SelectTrigger>
                  <SelectValue placeholder={selectedSupplyItemId ? "Chon lo vat tu" : "Chon supply item truoc"} />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="none">Khong chon</SelectItem>
                  {supplyLots.map((lot) => (
                    <SelectItem key={lot.id} value={String(lot.id)}>
                      {lot.batchCode ?? `Lot #${lot.id}`}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-2">
              <Label htmlFor="treatment-quantity">Quantity used</Label>
              <Input
                id="treatment-quantity"
                inputMode="decimal"
                value={treatmentForm.quantityUsed}
                onChange={(event) =>
                  setTreatmentForm((previous) => ({ ...previous, quantityUsed: event.target.value }))
                }
                placeholder="Vi du: 2.5"
              />
              {parsedTreatmentQuantityUsed.isInvalid && (
                <p className="text-xs text-destructive">
                  Vui long nhap so {">= 0"}.
                </p>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor="treatment-unit">Unit</Label>
              <Input
                id="treatment-unit"
                value={treatmentForm.unit}
                onChange={(event) =>
                  setTreatmentForm((previous) => ({ ...previous, unit: event.target.value }))
                }
                placeholder="ml, g, chai..."
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="treatment-cost">Cost amount (VND)</Label>
              <Input
                id="treatment-cost"
                inputMode="decimal"
                value={treatmentForm.costAmount}
                onChange={(event) =>
                  setTreatmentForm((previous) => ({ ...previous, costAmount: event.target.value }))
                }
                placeholder="Vi du: 150000"
              />
              {parsedTreatmentCostAmount.isInvalid && (
                <p className="text-xs text-destructive">
                  Vui long nhap so {">= 0"}.
                </p>
              )}
            </div>

            <div className="space-y-2">
              <Label>Effectiveness</Label>
              <Select
                value={treatmentForm.effectiveness}
                onValueChange={(value) =>
                  setTreatmentForm((previous) => ({ ...previous, effectiveness: value }))
                }
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {TREATMENT_EFFECTIVENESS_OPTIONS.map((option) => (
                    <SelectItem key={option.value} value={option.value}>
                      {option.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-2 md:col-span-2">
              <Label htmlFor="treatment-dosage-note">Dosage note</Label>
              <Textarea
                id="treatment-dosage-note"
                rows={2}
                value={treatmentForm.dosageNote}
                onChange={(event) =>
                  setTreatmentForm((previous) => ({ ...previous, dosageNote: event.target.value }))
                }
                placeholder="Chi tiet lieu dung / cach dung..."
              />
            </div>

            <div className="space-y-2 md:col-span-2">
              <Label htmlFor="treatment-result-note">Result note</Label>
              <Textarea
                id="treatment-result-note"
                rows={3}
                value={treatmentForm.resultNote}
                onChange={(event) =>
                  setTreatmentForm((previous) => ({ ...previous, resultNote: event.target.value }))
                }
                placeholder="Ket qua sau dieu tri..."
              />
            </div>
          </div>

          <DialogFooter>
            <Button variant="outline" onClick={closeTreatmentDialog}>
              Huy
            </Button>
            <Button
              onClick={handleSubmitTreatment}
              disabled={isMutating || !isTreatmentFormSubmittable}
            >
              {isMutating && <Loader2 className="w-4 h-4 mr-2 animate-spin" />}
              {editingTreatment ? "Cap nhat" : "Them dieu tri"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <AlertDialog open={deleteRecordId !== null} onOpenChange={() => setDeleteRecordId(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Xoa ho so benh</AlertDialogTitle>
            <AlertDialogDescription>
              Ban chac chan muon xoa ho so benh nay? Hanh dong nay khong the phuc hoi.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Huy</AlertDialogCancel>
            <AlertDialogAction
              className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
              onClick={handleDeleteRecord}
            >
              {deleteRecordMutation.isPending && <Loader2 className="w-4 h-4 mr-2 animate-spin" />}
              Xoa
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      <AlertDialog
        open={deleteTreatmentTarget !== null}
        onOpenChange={() => setDeleteTreatmentTarget(null)}
      >
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Xoa lich su dieu tri</AlertDialogTitle>
            <AlertDialogDescription>
              Ban chac chan muon xoa muc dieu tri nay? Du lieu se bi xoa vinh vien.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Huy</AlertDialogCancel>
            <AlertDialogAction
              className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
              onClick={handleDeleteTreatment}
            >
              {deleteTreatmentMutation.isPending && <Loader2 className="w-4 h-4 mr-2 animate-spin" />}
              Xoa
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      {isMutating && (
        <div className="acm-page-loading-overlay" role="status" aria-live="polite" aria-label="Dang xu ly du lieu dich benh">
          <div className="acm-page-loading-card acm-body-text">
            <Loader2 className="h-5 w-5 animate-spin text-primary" />
            <span>Dang xu ly du lieu dich benh...</span>
          </div>
        </div>
      )}

      {!hasValidSeasonId && (
        <div className="rounded-md border border-destructive/20 bg-destructive/5 p-3 text-sm text-destructive flex items-center gap-2">
          <AlertCircle className="w-4 h-4" />
          Mua vu khong hop le.
        </div>
      )}
    </PageContainer>
  );
}
