import {
  BackButton,
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
  CardContent,
} from "@/shared/ui";
import {
  AdminContentCard,
  AdminFilterCard,
  AdminHeaderCard,
  AdminPageContainer,
} from "@/features/admin/shared/ui";
import { useI18n } from "@/shared/lib/hooks/useI18n";
import {
  adminDocumentApi,
  type AdminDocument,
  type AdminDocumentCreateRequest,
} from "@/features/admin/shared/api";
import {
  AlertCircle,
  AlertTriangle,
  Ban,
  CheckCircle2,
  Edit,
  ExternalLink,
  FileText,
  Loader2,
  MoreVertical,
  Plus,
  RefreshCw,
  Search,
  Trash2,
  X,
} from "lucide-react";
import { useEffect, useRef, useState } from "react";
import { useSearchParams } from "react-router-dom";

// Document Type options
const DOCUMENT_TYPES = ["POLICY", "GUIDE", "MANUAL", "LEGAL", "OTHER"] as const;
type DocumentType = (typeof DOCUMENT_TYPES)[number];

// Status options
const DOCUMENT_STATUSES = ["ACTIVE", "INACTIVE"] as const;
type DocumentStatus = (typeof DOCUMENT_STATUSES)[number];

// Type badges styling
const typeBadgeColors: Record<DocumentType, string> = {
  POLICY:
    "bg-emerald-100 text-emerald-800 dark:bg-emerald-900/30 dark:text-emerald-400",
  GUIDE: "bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400",
  MANUAL:
    "bg-purple-100 text-purple-800 dark:bg-purple-900/30 dark:text-purple-400",
  LEGAL:
    "bg-orange-100 text-orange-800 dark:bg-orange-900/30 dark:text-orange-400",
  OTHER: "bg-gray-100 text-gray-800 dark:bg-gray-800/50 dark:text-gray-400",
};

// Status badges styling
const statusBadgeColors: Record<DocumentStatus, string> = {
  ACTIVE:
    "bg-emerald-100 text-emerald-800 dark:bg-emerald-900/30 dark:text-emerald-400",
  INACTIVE: "bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400",
};

export function AdminDocumentsPage() {
  const { t, locale } = useI18n();
  // Data states
  const [documents, setDocuments] = useState<AdminDocument[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Pagination states
  const [page, setPage] = useState(0);
  const [size] = useState(10);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  // Filter states
  const [searchQuery, setSearchQuery] = useState("");
  const [filterType, setFilterType] = useState<string>("");
  const [filterStatus, setFilterStatus] = useState<string>("");

  // Modal states
  const [showForm, setShowForm] = useState(false);
  const [editingDocument, setEditingDocument] = useState<AdminDocument | null>(
    null,
  );
  const [formLoading, setFormLoading] = useState(false);

  // Form fields
  const [formTitle, setFormTitle] = useState("");
  const [formDescription, setFormDescription] = useState("");
  const [formUrl, setFormUrl] = useState("");
  const [formType, setFormType] = useState<DocumentType>("POLICY");
  const [formStatus, setFormStatus] = useState<DocumentStatus>("ACTIVE");
  const [formErrors, setFormErrors] = useState<Record<string, string>>({});
  const initialFormRef = useRef<string | null>(null);

  // Confirmation dialog (Deactivate - Soft Delete)
  const [deactivateConfirm, setDeactivateConfirm] =
    useState<AdminDocument | null>(null);
  const [deactivateLoading, setDeactivateLoading] = useState(false);

  // Hard Delete confirmation dialog
  const [hardDeleteConfirm, setHardDeleteConfirm] =
    useState<AdminDocument | null>(null);
  const [hardDeleteLoading, setHardDeleteLoading] = useState(false);
  const [hardDeleteConfirmText, setHardDeleteConfirmText] = useState("");

  // Toast notification
  const [toast, setToast] = useState<{
    type: "success" | "error";
    message: string;
  } | null>(null);

  const [searchParams, setSearchParams] = useSearchParams();
  const qParam = searchParams.get("q") ?? "";
  const documentIdParam = Number(searchParams.get("documentId"));
  const parsedDocumentId = Number.isFinite(documentIdParam)
    ? documentIdParam
    : null;

  const serializeForm = () => JSON.stringify({
    title: formTitle,
    description: formDescription,
    url: formUrl,
    type: formType,
    status: formStatus,
  });

  const closeForm = ({ skipConfirm = false }: { skipConfirm?: boolean } = {}) => {
    const isDirty = showForm && !!initialFormRef.current && initialFormRef.current !== serializeForm();
    if (
      !skipConfirm &&
      isDirty &&
      !window.confirm(t("common.unsavedChangesConfirm", "You have unsaved changes. Leave this page?"))
    ) {
      return;
    }
    // If opened via deep-link (?documentId=...), clear it so closing stays closed.
    if (searchParams.get("documentId")) {
      const next = new URLSearchParams(searchParams);
      next.delete("documentId");
      setSearchParams(next, { replace: true });
    }
    setShowForm(false);
    setEditingDocument(null);
    initialFormRef.current = null;
  };

  // Fetch documents
  const fetchDocuments = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await adminDocumentApi.list({
        q: searchQuery.length >= 2 ? searchQuery : undefined,
        type: filterType || undefined,
        status: filterStatus || undefined,
        page,
        size,
      });
      setDocuments(response.items);
      setTotalPages(response.totalPages);
      setTotalElements(response.totalElements);
    } catch (err: any) {
      console.error("Failed to load documents:", err);
      setError(err?.response?.data?.message || t("admin.documents.error.load"));
    } finally {
      setLoading(false);
    }
  };

  // Effect: Load on mount and when filters change
  useEffect(() => {
    fetchDocuments();
  }, [page, searchQuery, filterType, filterStatus]);

  // Debounced search
  const [debouncedSearch, setDebouncedSearch] = useState("");
  useEffect(() => {
    const timer = setTimeout(() => {
      if (debouncedSearch !== searchQuery) {
        setSearchQuery(debouncedSearch);
        setPage(0);
      }
    }, 300);
    return () => clearTimeout(timer);
  }, [debouncedSearch]);

  useEffect(() => {
    if (qParam === searchQuery && qParam === debouncedSearch) {
      return;
    }
    setDebouncedSearch(qParam);
    setSearchQuery(qParam);
    setPage(0);
  }, [qParam, searchQuery, debouncedSearch]);

  // Open form for create/edit
  const openForm = (doc?: AdminDocument) => {
    if (doc) {
      setEditingDocument(doc);
      setFormTitle(doc.title);
      setFormDescription(doc.description || "");
      setFormUrl(doc.documentUrl);
      setFormType(doc.documentType as DocumentType);
      setFormStatus(doc.status as DocumentStatus);
      initialFormRef.current = JSON.stringify({
        title: doc.title,
        description: doc.description || "",
        url: doc.documentUrl,
        type: doc.documentType as DocumentType,
        status: doc.status as DocumentStatus,
      });
    } else {
      setEditingDocument(null);
      setFormTitle("");
      setFormDescription("");
      setFormUrl("");
      setFormType("POLICY");
      setFormStatus("ACTIVE");
      initialFormRef.current = JSON.stringify({
        title: "",
        description: "",
        url: "",
        type: "POLICY",
        status: "ACTIVE",
      });
    }
    setFormErrors({});
    setShowForm(true);
  };

  useEffect(() => {
    if (!parsedDocumentId) return;
    if (editingDocument?.id === parsedDocumentId && showForm) return;
    const match = documents.find((doc) => doc.id === parsedDocumentId);
    if (match) {
      openForm(match);
      return;
    }

    adminDocumentApi
      .getById(parsedDocumentId)
      .then((doc) => {
        if (doc) {
          openForm(doc);
        }
      })
      .catch(() => {});
  }, [parsedDocumentId, documents, editingDocument?.id, showForm, openForm]);

  // Validate form
  const validateForm = (): boolean => {
    const errors: Record<string, string> = {};

    if (!formTitle.trim() || formTitle.length < 3) {
      errors.title = t("admin.documents.validation.titleMin");
    }

    if (!formUrl.trim()) {
      errors.url = t("admin.documents.validation.urlRequired");
    } else {
      try {
        new URL(formUrl);
      } catch {
        errors.url = t("admin.documents.validation.urlInvalid");
      }
    }

    setFormErrors(errors);
    return Object.keys(errors).length === 0;
  };

  // Save document
  const handleSave = async () => {
    if (!validateForm()) return;

    setFormLoading(true);
    try {
      const payload: AdminDocumentCreateRequest = {
        title: formTitle.trim(),
        description: formDescription.trim() || undefined,
        documentUrl: formUrl.trim(),
        documentType: formType,
        status: formStatus,
      };

      if (editingDocument) {
        await adminDocumentApi.update(editingDocument.id, payload);
        setToast({ type: "success", message: t("admin.documents.toast.updated") });
      } else {
        await adminDocumentApi.create(payload);
        setToast({ type: "success", message: t("admin.documents.toast.created") });
      }

      closeForm({ skipConfirm: true });
      fetchDocuments();
    } catch (err: any) {
      console.error("Failed to save document:", err);
      setToast({
        type: "error",
        message: err?.response?.data?.message || t("admin.documents.toast.saveFailed"),
      });
    } finally {
      setFormLoading(false);
    }
  };

  // Deactivate document (soft delete)
  const handleDeactivate = async () => {
    if (!deactivateConfirm) return;

    setDeactivateLoading(true);
    try {
      await adminDocumentApi.delete(deactivateConfirm.id);
      setToast({
        type: "success",
        message: t("admin.documents.toast.deactivated", { title: deactivateConfirm.title }),
      });
      setDeactivateConfirm(null);
      fetchDocuments();
    } catch (err: any) {
      console.error("Failed to deactivate document:", err);
      setToast({
        type: "error",
        message:
          err?.response?.data?.message || t("admin.documents.toast.deactivateFailed"),
      });
    } finally {
      setDeactivateLoading(false);
    }
  };

  // Hard delete document (permanent)
  const handleHardDelete = async () => {
    if (!hardDeleteConfirm || hardDeleteConfirmText !== "DELETE") return;

    setHardDeleteLoading(true);
    try {
      await adminDocumentApi.hardDelete(hardDeleteConfirm.id);
      setToast({
        type: "success",
        message: t("admin.documents.toast.permanentlyDeleted", { title: hardDeleteConfirm.title }),
      });
      setHardDeleteConfirm(null);
      setHardDeleteConfirmText("");
      fetchDocuments();
    } catch (err: any) {
      console.error("Failed to hard delete document:", err);
      setToast({
        type: "error",
        message:
          err?.response?.data?.message ||
          t("admin.documents.toast.permanentDeleteFailed"),
      });
    } finally {
      setHardDeleteLoading(false);
    }
  };

  // Reset filters
  const resetFilters = () => {
    setDebouncedSearch("");
    setSearchQuery("");
    setFilterType("");
    setFilterStatus("");
    setPage(0);
  };

  // Auto-hide toast
  useEffect(() => {
    if (toast) {
      const timer = setTimeout(() => setToast(null), 5000);
      return () => clearTimeout(timer);
    }
  }, [toast]);

  // Format date
  const formatDate = (dateStr: string | null | undefined) => {
    if (!dateStr) return "-";
    try {
      return new Date(dateStr).toLocaleDateString(locale, {
        year: "numeric",
        month: "short",
        day: "numeric",
      });
    } catch {
      return dateStr;
    }
  };

  return (
    <AdminPageContainer>
      <AdminHeaderCard
        title={t("admin.documents.title")}
        description={t("admin.documents.subtitle")}
      />

      {/* Filters & Actions */}
      <AdminFilterCard>
        <CardContent className="p-4">
          <div className="flex flex-col gap-3 sm:flex-row sm:flex-wrap sm:items-center">
            {/* Search */}
            <div className="relative w-full sm:flex-1 sm:min-w-[200px] sm:max-w-[300px]">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
              <input
                type="text"
                value={debouncedSearch}
                onChange={(e) => setDebouncedSearch(e.target.value)}
                placeholder={t("admin.documents.searchPlaceholder")}
                className="w-full pl-10 pr-4 py-2 border border-border rounded-[14px] bg-card text-sm focus:outline-none focus:ring-2 focus:ring-primary/20"
              />
            </div>

        {/* Type Filter */}
        <select
          value={filterType}
          onChange={(e) => {
            setFilterType(e.target.value);
            setPage(0);
          }}
          className="w-full sm:w-auto px-3 py-2 border border-border rounded-[14px] bg-card text-sm"
        >
          <option value="">{t("admin.documents.filters.allTypes")}</option>
          {DOCUMENT_TYPES.map((type) => (
            <option key={type} value={type}>
              {t(`admin.documents.types.${type}`, type)}
            </option>
          ))}
        </select>

        {/* Status Filter */}
        <select
          value={filterStatus}
          onChange={(e) => {
            setFilterStatus(e.target.value);
            setPage(0);
          }}
          className="w-full sm:w-auto px-3 py-2 border border-border rounded-[14px] bg-card text-sm"
        >
          <option value="">{t("admin.documents.filters.allStatuses")}</option>
          {DOCUMENT_STATUSES.map((status) => (
            <option key={status} value={status}>
              {t(`admin.documents.status.${status}`, status)}
            </option>
          ))}
        </select>

        {/* Reset Filters */}
        {(searchQuery || filterType || filterStatus) && (
          <button
            onClick={resetFilters}
            className="w-full sm:w-auto px-3 py-2 text-sm text-muted-foreground hover:text-foreground"
          >
            {t("common.reset")}
          </button>
        )}

        <div className="hidden sm:block flex-1" />

        {/* Create Button */}
        <button
          onClick={() => openForm()}
          className="w-full sm:w-auto inline-flex items-center justify-center gap-2 px-4 py-2 bg-primary text-primary-foreground rounded-[14px] text-sm hover:bg-primary/90 transition-colors"
        >
          <Plus className="h-4 w-4" />
          {t("admin.documents.actions.create")}
        </button>

        {/* Refresh */}
        <button
          onClick={fetchDocuments}
          className="w-full sm:w-auto p-2 border border-border rounded-[14px] hover:bg-muted/50 transition-colors"
          title={t("common.refresh")}
        >
          <RefreshCw className={`h-4 w-4 ${loading ? "animate-spin" : ""}`} />
        </button>
          </div>
        </CardContent>
      </AdminFilterCard>

      {/* Stats */}
      <div className="flex items-center gap-4">
        <span className="text-sm text-muted-foreground">
          {t("admin.documents.resultsFound", { count: totalElements })}
        </span>
      </div>

      {/* Table */}
      <AdminContentCard>
        <div className="overflow-x-auto">
        <table className="w-full min-w-[840px]">
          <thead className="bg-muted/50 border-b border-border">
            <tr>
              <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">
                {t("admin.documents.table.title")}
              </th>
              <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">
                {t("common.type")}
              </th>
              <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">
                {t("common.status")}
              </th>
              <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">
                URL
              </th>
              <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">
                {t("admin.alerts.table.created")}
              </th>
              <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">
                {t("common.actions")}
              </th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td
                  colSpan={6}
                  className="px-4 py-12 text-center text-muted-foreground"
                >
                  <RefreshCw className="h-6 w-6 animate-spin mx-auto mb-2" />
                  {t("admin.documents.loading")}
                </td>
              </tr>
            ) : error ? (
              <tr>
                <td colSpan={6} className="px-4 py-12 text-center">
                  <div className="flex flex-col items-center gap-2 text-destructive">
                    <AlertCircle className="h-6 w-6" />
                    {error}
                    <button
                      onClick={fetchDocuments}
                      className="text-sm text-primary hover:underline"
                    >
                      {t("common.tryAgain")}
                    </button>
                  </div>
                </td>
              </tr>
            ) : documents.length === 0 ? (
              <tr>
                <td
                  colSpan={6}
                  className="px-4 py-12 text-center text-muted-foreground"
                >
                  <FileText className="h-8 w-8 mx-auto mb-2 opacity-50" />
                  {t("admin.documents.empty")}
                </td>
              </tr>
            ) : (
              documents.map((doc) => (
                <tr
                  key={doc.id}
                  className="border-b border-border hover:bg-muted/30 transition-colors"
                >
                  <td className="px-4 py-3">
                    <div className="font-medium text-sm">{doc.title}</div>
                    {doc.description && (
                      <div className="text-xs text-muted-foreground line-clamp-1 mt-0.5">
                        {doc.description}
                      </div>
                    )}
                  </td>
                  <td className="px-4 py-3">
                    <span
                      className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                        typeBadgeColors[doc.documentType as DocumentType] ||
                        typeBadgeColors.OTHER
                      }`}
                    >
                      {t(`admin.documents.types.${doc.documentType}`, doc.documentType)}
                    </span>
                  </td>
                  <td className="px-4 py-3">
                    <span
                      className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-medium ${
                        statusBadgeColors[doc.status as DocumentStatus] ||
                        statusBadgeColors.INACTIVE
                      }`}
                    >
                      {doc.status === "ACTIVE" ? (
                        <CheckCircle2 className="h-3 w-3" />
                      ) : (
                        <Ban className="h-3 w-3" />
                      )}
                      {t(`admin.documents.status.${doc.status}`, doc.status)}
                    </span>
                  </td>
                  <td className="px-4 py-3">
                    <a
                      href={doc.documentUrl}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="inline-flex items-center gap-1 text-sm text-primary hover:underline"
                    >
                      <ExternalLink className="h-3 w-3" />
                      {t("admin.documents.actions.open")}
                    </a>
                  </td>
                  <td className="px-4 py-3 text-sm text-muted-foreground">
                    {formatDate(doc.createdAt)}
                  </td>
                  <td className="px-4 py-3">
                    <DropdownMenu>
                      <DropdownMenuTrigger asChild>
                        <button
                          className="p-2 rounded-lg hover:bg-muted transition-colors focus:outline-none focus:ring-2 focus:ring-primary/20"
                          aria-label={t("common.actions")}
                        >
                          <MoreVertical className="h-4 w-4 text-muted-foreground" />
                        </button>
                      </DropdownMenuTrigger>
                      <DropdownMenuContent align="end" className="w-48">
                        <DropdownMenuItem
                          onClick={() => openForm(doc)}
                          className="cursor-pointer"
                        >
                          <Edit className="mr-2 h-4 w-4" />
                          {t("common.edit")}
                        </DropdownMenuItem>
                        <DropdownMenuSeparator />
                        <DropdownMenuItem
                          onClick={() => setDeactivateConfirm(doc)}
                          className="cursor-pointer text-destructive focus:text-destructive"
                          disabled={doc.status === "INACTIVE"}
                        >
                          <Ban className="mr-2 h-4 w-4" />
                          {t("admin.documents.actions.deactivate")}
                        </DropdownMenuItem>
                        <TooltipProvider>
                          <Tooltip>
                            <TooltipTrigger asChild>
                              <span className="w-full">
                                <DropdownMenuItem
                                  onClick={() => setHardDeleteConfirm(doc)}
                                  className="cursor-pointer text-destructive focus:text-destructive"
                                  disabled={doc.status !== "INACTIVE"}
                                >
                                  <Trash2 className="mr-2 h-4 w-4" />
                                  {t("admin.documents.actions.hardDelete")}
                                </DropdownMenuItem>
                              </span>
                            </TooltipTrigger>
                            {doc.status !== "INACTIVE" && (
                              <TooltipContent side="left">
                                <p>{t("admin.documents.tooltip.deactivateFirstLine1")}</p>
                                <p>{t("admin.documents.tooltip.deactivateFirstLine2")}</p>
                              </TooltipContent>
                            )}
                          </Tooltip>
                        </TooltipProvider>
                      </DropdownMenuContent>
                    </DropdownMenu>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
        </div>
      </AdminContentCard>

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between mt-4">
          <span className="text-sm text-muted-foreground">
            {t("pagination.page")} {page + 1} {t("pagination.of")} {totalPages}
          </span>
          <div className="flex items-center gap-2">
            <button
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              disabled={page === 0}
              className="px-3 py-1 border border-border rounded-lg text-sm disabled:opacity-50 disabled:cursor-not-allowed hover:bg-muted/50"
            >
              {t("pagination.previousPage")}
            </button>
            <button
              onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
              disabled={page >= totalPages - 1}
              className="px-3 py-1 border border-border rounded-lg text-sm disabled:opacity-50 disabled:cursor-not-allowed hover:bg-muted/50"
            >
              {t("pagination.nextPage")}
            </button>
          </div>
        </div>
      )}

      {/* Create/Edit Modal */}
      {showForm && (
        <div className="fixed inset-0 z-50 bg-background/80 backdrop-blur-sm flex items-center justify-center">
          <div className="bg-card border border-border rounded-lg shadow-lg w-full max-w-md mx-4">
            <div className="flex items-center justify-between p-4 border-b border-border">
              <h3 className="text-lg font-semibold">
                {editingDocument ? t("admin.documents.form.editTitle") : t("admin.documents.form.createTitle")}
              </h3>
              <BackButton onClick={() => closeForm()} />
              <button
                onClick={() => closeForm()}
                className="p-1 hover:bg-muted rounded"
              >
                <X className="h-4 w-4" />
              </button>
            </div>

            <div className="p-4 space-y-4 max-h-[70vh] overflow-y-auto">
              {/* Title */}
              <div>
                <label className="block text-sm font-medium mb-1">
                  {t("admin.documents.table.title")} <span className="text-destructive">*</span>
                </label>
                <input
                  type="text"
                  value={formTitle}
                  onChange={(e) => setFormTitle(e.target.value)}
                  className={`w-full px-3 py-2 border rounded-lg bg-background text-sm ${
                    formErrors.title ? "border-destructive" : "border-border"
                  }`}
                  placeholder={t("admin.documents.form.titlePlaceholder")}
                />
                {formErrors.title && (
                  <p className="text-xs text-destructive mt-1">
                    {formErrors.title}
                  </p>
                )}
              </div>

              {/* Description */}
              <div>
                <label className="block text-sm font-medium mb-1">
                  {t("common.description")}
                </label>
                <textarea
                  value={formDescription}
                  onChange={(e) => setFormDescription(e.target.value)}
                  className="w-full px-3 py-2 border border-border rounded-lg bg-background text-sm min-h-[60px] resize-none"
                  placeholder={t("admin.crops.form.descriptionPlaceholder")}
                />
              </div>

              {/* Document URL */}
              <div>
                <label className="block text-sm font-medium mb-1">
                  {t("admin.documents.form.documentUrl")} <span className="text-destructive">*</span>
                </label>
                <input
                  type="url"
                  value={formUrl}
                  onChange={(e) => setFormUrl(e.target.value)}
                  className={`w-full px-3 py-2 border rounded-lg bg-background text-sm ${
                    formErrors.url ? "border-destructive" : "border-border"
                  }`}
                  placeholder={t("admin.documents.form.urlPlaceholder")}
                />
                {formErrors.url && (
                  <p className="text-xs text-destructive mt-1">
                    {formErrors.url}
                  </p>
                )}
              </div>

              {/* Document Type & Status - 2 columns */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium mb-1">
                    {t("common.type")} <span className="text-destructive">*</span>
                  </label>
                  <select
                    value={formType}
                    onChange={(e) =>
                      setFormType(e.target.value as DocumentType)
                    }
                    className="w-full px-3 py-2 border border-border rounded-lg bg-background text-sm"
                  >
                    {DOCUMENT_TYPES.map((type) => (
                      <option key={type} value={type}>
                        {t(`admin.documents.types.${type}`, type)}
                      </option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="block text-sm font-medium mb-1">
                    {t("common.status")} <span className="text-destructive">*</span>
                  </label>
                  <select
                    value={formStatus}
                    onChange={(e) =>
                      setFormStatus(e.target.value as DocumentStatus)
                    }
                    className="w-full px-3 py-2 border border-border rounded-lg bg-background text-sm"
                  >
                    {DOCUMENT_STATUSES.map((status) => (
                      <option key={status} value={status}>
                        {t(`admin.documents.status.${status}`, status)}
                      </option>
                    ))}
                  </select>
                </div>
              </div>
            </div>

            <div className="flex justify-end gap-2 p-4 border-t border-border">
              <button
                onClick={() => closeForm()}
                className="px-4 py-2 border border-border rounded-lg text-sm hover:bg-muted/50"
              >
                {t("common.cancel")}
              </button>
              <button
                onClick={handleSave}
                disabled={formLoading}
                className="inline-flex items-center gap-2 px-4 py-2 bg-primary text-primary-foreground rounded-lg text-sm hover:bg-primary/90 disabled:opacity-50"
              >
                {formLoading && <Loader2 className="h-4 w-4 animate-spin" />}
                {editingDocument ? t("common.update") : t("common.create")}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Deactivate Confirmation Dialog */}
      {deactivateConfirm && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="bg-card border border-border rounded-lg shadow-xl w-full max-w-md mx-4">
            <div className="p-4 border-b border-border">
              <h2 className="text-lg font-semibold text-destructive">
                {t("admin.documents.deactivate.title")}
              </h2>
            </div>
            <div className="p-4">
              <p className="text-sm text-muted-foreground mb-4">
                {t("admin.documents.deactivate.confirm")}{" "}
                <strong className="text-foreground">
                  "{deactivateConfirm.title}"
                </strong>
                ?
              </p>
              <p className="text-xs text-muted-foreground">
                {t("admin.documents.deactivate.description")}
              </p>
            </div>
            <div className="flex justify-end gap-2 p-4 border-t border-border">
              <button
                onClick={() => setDeactivateConfirm(null)}
                disabled={deactivateLoading}
                className="px-4 py-2 border border-border rounded-lg text-sm hover:bg-muted/50 disabled:opacity-50"
              >
                {t("common.cancel")}
              </button>
              <button
                onClick={handleDeactivate}
                disabled={deactivateLoading}
                className="inline-flex items-center gap-2 px-4 py-2 bg-destructive text-destructive-foreground rounded-lg text-sm hover:bg-destructive/90 disabled:opacity-50"
              >
                {deactivateLoading ? (
                  <>
                    <Loader2 className="h-4 w-4 animate-spin" />
                    {t("admin.documents.actions.deactivating")}
                  </>
                ) : (
                  <>
                    <Ban className="h-4 w-4" />
                    {t("admin.documents.actions.deactivate")}
                  </>
                )}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Hard Delete Confirmation Dialog */}
      {hardDeleteConfirm && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="bg-card border border-border rounded-lg shadow-xl w-full max-w-md mx-4">
            <div className="p-4 border-b border-border bg-destructive/5">
              <div className="flex items-center gap-2">
                <AlertTriangle className="h-5 w-5 text-destructive" />
                <h2 className="text-lg font-semibold text-destructive">
                  {t("admin.documents.hardDelete.title")}
                </h2>
              </div>
            </div>
            <div className="p-4 space-y-4">
              <p className="text-sm text-foreground">
                {t("admin.documents.hardDelete.aboutToDelete")}{" "}
                <strong>"{hardDeleteConfirm.title}"</strong>.
              </p>
              <div className="bg-destructive/10 border border-destructive/20 rounded-lg p-3">
                <p className="text-sm text-destructive font-medium">
                  {t("admin.documents.hardDelete.cannotUndo")}
                </p>
                <p className="text-xs text-destructive/80 mt-1">
                  {t("admin.documents.hardDelete.description")}
                </p>
              </div>
              <div>
                <label className="block text-sm font-medium mb-2">
                  {t("admin.documents.hardDelete.typeDeletePrefix")}{" "}
                  <strong className="text-destructive">DELETE</strong>{" "}
                  {t("admin.documents.hardDelete.typeDeleteSuffix")}
                </label>
                <input
                  type="text"
                  value={hardDeleteConfirmText}
                  onChange={(e) => setHardDeleteConfirmText(e.target.value)}
                  placeholder={t("admin.documents.hardDelete.placeholder")}
                  className="w-full px-3 py-2 border border-border rounded-lg bg-background text-sm focus:outline-none focus:ring-2 focus:ring-destructive/20"
                  autoComplete="off"
                />
              </div>
            </div>
            <div className="flex justify-end gap-2 p-4 border-t border-border">
              <button
                onClick={() => {
                  setHardDeleteConfirm(null);
                  setHardDeleteConfirmText("");
                }}
                disabled={hardDeleteLoading}
                className="px-4 py-2 border border-border rounded-lg text-sm hover:bg-muted/50 disabled:opacity-50"
              >
                {t("common.cancel")}
              </button>
              <button
                onClick={handleHardDelete}
                disabled={
                  hardDeleteConfirmText !== "DELETE" || hardDeleteLoading
                }
                className="inline-flex items-center gap-2 px-4 py-2 bg-destructive text-destructive-foreground rounded-lg text-sm hover:bg-destructive/90 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {hardDeleteLoading ? (
                  <>
                    <Loader2 className="h-4 w-4 animate-spin" />
                    {t("common.deleting")}
                  </>
                ) : (
                  <>
                    <Trash2 className="h-4 w-4" />
                    {t("admin.documents.actions.permanentlyDelete")}
                  </>
                )}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Toast Notification */}
      {toast && (
        <div
          className={`fixed bottom-4 right-4 z-50 px-4 py-3 rounded-lg shadow-lg flex items-center gap-2 ${
            toast.type === "success"
              ? "bg-green-600 text-white"
              : "bg-destructive text-destructive-foreground"
          }`}
        >
          {toast.type === "success" ? (
            <CheckCircle2 className="h-4 w-4" />
          ) : (
            <AlertCircle className="h-4 w-4" />
          )}
          <span className="text-sm">{toast.message}</span>
          <button
            onClick={() => setToast(null)}
            className="ml-2 hover:opacity-70"
          >
            <X className="h-4 w-4" />
          </button>
        </div>
      )}
    </AdminPageContainer>
  );
}

export default AdminDocumentsPage;
