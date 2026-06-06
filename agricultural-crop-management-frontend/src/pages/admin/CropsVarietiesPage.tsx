import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
  CardContent,
} from "@/shared/ui";
import {
  AdminContentCard,
  AdminFilterCard,
  AdminHeaderCard,
  AdminPageContainer,
  adminTabsListClass,
} from "@/features/admin/shared/ui";
import { cn } from "@/shared/lib";
import { useI18n } from "@/shared/lib/hooks/useI18n";
import { adminCropApi, adminVarietyApi } from "@/features/admin/shared/api";
import {
  AlertCircle,
  Edit,
  Leaf,
  Loader2,
  MoreVertical,
  Plus,
  RefreshCw,
  Search,
  Sprout,
  Trash2,
  X,
} from "lucide-react";
import { useEffect, useState } from "react";

interface Crop {
  id: number;
  cropName: string;
  description: string | null;
}

interface Variety {
  id: number;
  name: string;
  description: string | null;
  cropId: number;
  cropName: string;
}

export function CropsVarietiesPage() {
  const { t } = useI18n();
  const [activeTab, setActiveTab] = useState<"crops" | "varieties">("crops");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [crops, setCrops] = useState<Crop[]>([]);
  const [varieties, setVarieties] = useState<Variety[]>([]);

  // Filter state
  const [selectedCropId, setSelectedCropId] = useState<number | null>(null);

  // Search state
  const [cropSearchQuery, setCropSearchQuery] = useState("");
  const [varietySearchQuery, setVarietySearchQuery] = useState("");

  // Form states
  const [showCropForm, setShowCropForm] = useState(false);
  const [showVarietyForm, setShowVarietyForm] = useState(false);
  const [editingCrop, setEditingCrop] = useState<Crop | null>(null);
  const [editingVariety, setEditingVariety] = useState<Variety | null>(null);
  const [formLoading, setFormLoading] = useState(false);

  // Form fields
  const [cropName, setCropName] = useState("");
  const [cropDescription, setCropDescription] = useState("");
  const [varietyName, setVarietyName] = useState("");
  const [varietyDescription, setVarietyDescription] = useState("");
  const [varietyCropId, setVarietyCropId] = useState<number | null>(null);

  // Delete confirmation state
  const [deleteConfirm, setDeleteConfirm] = useState<{
    type: "crop" | "variety";
    id: number;
    name: string;
  } | null>(null);
  const [deleteLoading, setDeleteLoading] = useState(false);

  // Toast notification state
  const [toast, setToast] = useState<{
    type: "success" | "error";
    message: string;
  } | null>(null);

  const fetchCrops = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await adminCropApi.list();
      if (response?.result) {
        setCrops(Array.isArray(response.result) ? response.result : []);
      }
    } catch (err) {
      setError(t("admin.crops.loadCropsError"));
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const fetchVarieties = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await adminVarietyApi.list(selectedCropId || undefined);
      if (response?.result) {
        setVarieties(Array.isArray(response.result) ? response.result : []);
      }
    } catch (err) {
      setError(t("admin.crops.loadVarietiesError"));
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchCrops();
  }, []);

  useEffect(() => {
    if (activeTab === "varieties") {
      fetchVarieties();
    }
  }, [activeTab, selectedCropId]);

  const openCropForm = (crop?: Crop) => {
    if (crop) {
      setEditingCrop(crop);
      setCropName(crop.cropName);
      setCropDescription(crop.description || "");
    } else {
      setEditingCrop(null);
      setCropName("");
      setCropDescription("");
    }
    setShowCropForm(true);
  };

  const openVarietyForm = (variety?: Variety) => {
    if (variety) {
      setEditingVariety(variety);
      setVarietyName(variety.name);
      setVarietyDescription(variety.description || "");
      setVarietyCropId(variety.cropId);
    } else {
      setEditingVariety(null);
      setVarietyName("");
      setVarietyDescription("");
      setVarietyCropId(selectedCropId || crops[0]?.id || null);
    }
    setShowVarietyForm(true);
  };

  const handleSaveCrop = async () => {
    if (!cropName.trim()) return;

    setFormLoading(true);
    try {
      if (editingCrop) {
        await adminCropApi.update(editingCrop.id, {
          cropName,
          description: cropDescription,
        });
      } else {
        await adminCropApi.create({ cropName, description: cropDescription });
      }
      setShowCropForm(false);
      fetchCrops();
    } catch (err) {
      console.error("Failed to save crop:", err);
    } finally {
      setFormLoading(false);
    }
  };

  const handleSaveVariety = async () => {
    if (!varietyName.trim() || !varietyCropId) return;

    setFormLoading(true);
    try {
      if (editingVariety) {
        await adminVarietyApi.update(editingVariety.id, {
          name: varietyName,
          cropId: varietyCropId,
          description: varietyDescription,
        });
      } else {
        await adminVarietyApi.create({
          name: varietyName,
          cropId: varietyCropId,
          description: varietyDescription,
        });
      }
      setShowVarietyForm(false);
      fetchVarieties();
    } catch (err) {
      console.error("Failed to save variety:", err);
    } finally {
      setFormLoading(false);
    }
  };

  const handleDelete = async () => {
    if (!deleteConfirm) return;

    setDeleteLoading(true);
    try {
      if (deleteConfirm.type === "crop") {
        await adminCropApi.delete(deleteConfirm.id);
        setToast({
          type: "success",
          message: t("admin.crops.toast.cropDeleted", { name: deleteConfirm.name }),
        });
        fetchCrops();
      } else {
        await adminVarietyApi.delete(deleteConfirm.id);
        setToast({
          type: "success",
          message: t("admin.crops.toast.varietyDeleted", { name: deleteConfirm.name }),
        });
        fetchVarieties();
      }
      setDeleteConfirm(null);
    } catch (err: any) {
      // Handle 409 Conflict errors with specific backend messages
      const errorMessage =
        err?.response?.data?.message ||
        (deleteConfirm.type === "crop"
          ? t("admin.crops.toast.cropDeleteBlocked")
          : t("admin.crops.toast.varietyDeleteBlocked"));
      setToast({ type: "error", message: errorMessage });
      setDeleteConfirm(null);
    } finally {
      setDeleteLoading(false);
    }
  };

  // Auto-hide toast after 5 seconds
  useEffect(() => {
    if (toast) {
      const timer = setTimeout(() => setToast(null), 5000);
      return () => clearTimeout(timer);
    }
  }, [toast]);

  // Filtered data based on search
  const filteredCrops = crops.filter(
    (crop) =>
      crop.cropName.toLowerCase().includes(cropSearchQuery.toLowerCase()) ||
      (crop.description || "")
        .toLowerCase()
        .includes(cropSearchQuery.toLowerCase()),
  );

  const filteredVarieties = varieties.filter(
    (variety) =>
      variety.name.toLowerCase().includes(varietySearchQuery.toLowerCase()) ||
      (variety.description || "")
        .toLowerCase()
        .includes(varietySearchQuery.toLowerCase()) ||
      variety.cropName.toLowerCase().includes(varietySearchQuery.toLowerCase()),
  );

  const renderCrops = () => (
    <div className="space-y-4">
      <AdminFilterCard>
        <CardContent className="space-y-3 p-4">
          <div className="flex flex-col sm:flex-row sm:items-center gap-3">
            <div className="relative w-full sm:flex-1 sm:max-w-md">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
              <input
                type="text"
                value={cropSearchQuery}
                onChange={(e) => setCropSearchQuery(e.target.value)}
                placeholder={t("admin.crops.searchCrops")}
                className="w-full pl-9 pr-3 py-2 border border-border rounded-[14px] bg-card text-sm focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary"
              />
            </div>
            <button
              onClick={() => setCropSearchQuery(cropSearchQuery)}
              className="w-full sm:w-auto inline-flex items-center justify-center gap-2 px-4 py-2 border border-border rounded-[14px] text-sm hover:bg-muted/50 transition-colors"
            >
              <Search className="h-4 w-4" />
              {t("common.search")}
            </button>
          </div>

          <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
            <div className="flex items-center gap-2">
              <span className="text-sm text-muted-foreground">
                {filteredCrops.length}{" "}
                {filteredCrops.length === 1 ? t("admin.crops.count.crop") : t("admin.crops.count.crops")}
                {cropSearchQuery && ` (${t("admin.crops.filteredFrom")} ${crops.length})`}
              </span>
            </div>
            <div className="flex flex-col sm:flex-row sm:items-center gap-2">
              <button
                onClick={() => openCropForm()}
                className="w-full sm:w-auto inline-flex items-center justify-center gap-2 px-3 py-2 bg-primary text-primary-foreground rounded-[14px] text-sm hover:bg-primary/90"
              >
                <Plus className="h-4 w-4" />
                {t("admin.crops.addCrop")}
              </button>
              <button
                onClick={fetchCrops}
                className="w-full sm:w-auto inline-flex items-center justify-center gap-2 px-3 py-2 border border-border rounded-[14px] text-sm hover:bg-muted/50"
              >
                <RefreshCw className={`h-4 w-4 ${loading ? "animate-spin" : ""}`} />
              </button>
            </div>
          </div>
        </CardContent>
      </AdminFilterCard>

      <AdminContentCard>
        <div className="overflow-x-auto">
        <table className="w-full min-w-[620px]">
          <thead className="bg-muted/50 border-b border-border">
            <tr>
              <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">
                {t("admin.crops.table.name")}
              </th>
              <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">
                {t("admin.crops.table.description")}
              </th>
              <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">
                {t("admin.crops.table.actions")}
              </th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td
                  colSpan={3}
                  className="px-4 py-8 text-center text-muted-foreground"
                >
                  <RefreshCw className="h-6 w-6 animate-spin mx-auto mb-2" />
                  {t("common.loading")}
                </td>
              </tr>
            ) : error ? (
              <tr>
                <td colSpan={3} className="px-4 py-8 text-center">
                  <div className="flex flex-col items-center gap-2 text-destructive">
                    <AlertCircle className="h-6 w-6" />
                    {error}
                    <button
                      onClick={fetchCrops}
                      className="text-sm text-primary hover:underline"
                    >
                      {t("common.tryAgain")}
                    </button>
                  </div>
                </td>
              </tr>
            ) : filteredCrops.length === 0 ? (
              <tr>
                <td
                  colSpan={3}
                  className="px-4 py-8 text-center text-muted-foreground"
                >
                  <Leaf className="h-8 w-8 mx-auto mb-2 opacity-50" />
                  {cropSearchQuery
                    ? t("admin.crops.empty.noCropsMatching", { query: cropSearchQuery })
                    : t("admin.crops.empty.noCrops")}
                </td>
              </tr>
            ) : (
              filteredCrops.map((crop) => (
                <tr
                  key={crop.id}
                  className="border-b border-border hover:bg-muted/30"
                >
                  <td className="px-4 py-3 text-sm font-medium">
                    {crop.cropName}
                  </td>
                  <td className="px-4 py-3 text-sm text-muted-foreground">
                    {crop.description || "-"}
                  </td>
                  <td className="px-4 py-3">
                    <DropdownMenu>
                      <DropdownMenuTrigger asChild>
                        <button
                          className="p-2 rounded-lg hover:bg-muted transition-colors focus:outline-none focus:ring-2 focus:ring-primary/20"
                          aria-label={t("admin.crops.actionsMenu")}
                        >
                          <MoreVertical className="h-4 w-4 text-muted-foreground" />
                        </button>
                      </DropdownMenuTrigger>
                      <DropdownMenuContent align="end" className="w-40">
                        <DropdownMenuItem
                          onClick={() => openCropForm(crop)}
                          className="cursor-pointer"
                        >
                          <Edit className="mr-2 h-4 w-4" />
                          {t("common.edit")}
                        </DropdownMenuItem>
                        <DropdownMenuSeparator />
                        <DropdownMenuItem
                          onClick={() =>
                            setDeleteConfirm({
                              type: "crop",
                              id: crop.id,
                              name: crop.cropName,
                            })
                          }
                          className="cursor-pointer text-destructive focus:text-destructive"
                        >
                          <Trash2 className="mr-2 h-4 w-4" />
                          {t("common.delete")}
                        </DropdownMenuItem>
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
    </div>
  );

  const renderVarieties = () => (
    <div className="space-y-4">
      <AdminFilterCard>
        <CardContent className="space-y-3 p-4">
          <div className="flex flex-col sm:flex-row sm:items-center gap-3">
            <div className="relative w-full sm:flex-1 sm:max-w-md">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
              <input
                type="text"
                value={varietySearchQuery}
                onChange={(e) => setVarietySearchQuery(e.target.value)}
                placeholder={t("admin.crops.searchVarieties")}
                className="w-full pl-9 pr-3 py-2 border border-border rounded-[14px] bg-card text-sm focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary"
              />
            </div>
            <button
              onClick={() => setVarietySearchQuery(varietySearchQuery)}
              className="w-full sm:w-auto inline-flex items-center justify-center gap-2 px-4 py-2 border border-border rounded-[14px] text-sm hover:bg-muted/50 transition-colors"
            >
              <Search className="h-4 w-4" />
              {t("common.search")}
            </button>
          </div>

          <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
            <div className="flex flex-col sm:flex-row sm:items-center gap-2">
              <select
                value={selectedCropId || ""}
                onChange={(e) =>
                  setSelectedCropId(e.target.value ? Number(e.target.value) : null)
                }
                className="w-full sm:w-auto px-3 py-2 border border-border rounded-[14px] bg-card text-sm"
              >
                <option value="">{t("seasonFilters.allCrops")}</option>
                {crops.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.cropName}
                  </option>
                ))}
              </select>
              <span className="text-sm text-muted-foreground">
                {filteredVarieties.length}{" "}
                {filteredVarieties.length === 1 ? t("admin.crops.count.variety") : t("admin.crops.count.varieties")}
                {varietySearchQuery && ` (${t("admin.crops.filteredFrom")} ${varieties.length})`}
              </span>
            </div>
            <div className="flex flex-col sm:flex-row sm:items-center gap-2">
              <button
                onClick={() => openVarietyForm()}
                className="w-full sm:w-auto inline-flex items-center justify-center gap-2 px-3 py-2 bg-primary text-primary-foreground rounded-[14px] text-sm hover:bg-primary/90"
              >
                <Plus className="h-4 w-4" />
                {t("admin.crops.addVariety")}
              </button>
              <button
                onClick={fetchVarieties}
                className="w-full sm:w-auto inline-flex items-center justify-center gap-2 px-3 py-2 border border-border rounded-[14px] text-sm hover:bg-muted/50"
              >
                <RefreshCw className={`h-4 w-4 ${loading ? "animate-spin" : ""}`} />
              </button>
            </div>
          </div>
        </CardContent>
      </AdminFilterCard>

      <AdminContentCard>
        <div className="overflow-x-auto">
        <table className="w-full min-w-[760px]">
          <thead className="bg-muted/50 border-b border-border">
            <tr>
              <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">
                {t("admin.crops.table.name")}
              </th>
              <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">
                {t("admin.crops.table.crop")}
              </th>
              <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">
                {t("admin.crops.table.description")}
              </th>
              <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">
                {t("admin.crops.table.actions")}
              </th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td
                  colSpan={4}
                  className="px-4 py-8 text-center text-muted-foreground"
                >
                  <RefreshCw className="h-6 w-6 animate-spin mx-auto mb-2" />
                  {t("common.loading")}
                </td>
              </tr>
            ) : error ? (
              <tr>
                <td colSpan={4} className="px-4 py-8 text-center">
                  <div className="flex flex-col items-center gap-2 text-destructive">
                    <AlertCircle className="h-6 w-6" />
                    {error}
                    <button
                      onClick={fetchVarieties}
                      className="text-sm text-primary hover:underline"
                    >
                      {t("common.tryAgain")}
                    </button>
                  </div>
                </td>
              </tr>
            ) : filteredVarieties.length === 0 ? (
              <tr>
                <td
                  colSpan={4}
                  className="px-4 py-8 text-center text-muted-foreground"
                >
                  <Sprout className="h-8 w-8 mx-auto mb-2 opacity-50" />
                  {varietySearchQuery
                    ? t("admin.crops.empty.noVarietiesMatching", { query: varietySearchQuery })
                    : t("admin.crops.empty.noVarieties")}
                </td>
              </tr>
            ) : (
              filteredVarieties.map((variety) => (
                <tr
                  key={variety.id}
                  className="border-b border-border hover:bg-muted/30"
                >
                  <td className="px-4 py-3 text-sm font-medium">
                    {variety.name}
                  </td>
                  <td className="px-4 py-3 text-sm">
                    <span className="inline-flex items-center px-2 py-1 rounded-full text-xs bg-primary/10 text-primary">
                      {variety.cropName}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-sm text-muted-foreground">
                    {variety.description || "-"}
                  </td>
                  <td className="px-4 py-3">
                    <DropdownMenu>
                      <DropdownMenuTrigger asChild>
                        <button
                          className="p-2 rounded-lg hover:bg-muted transition-colors focus:outline-none focus:ring-2 focus:ring-primary/20"
                          aria-label={t("admin.crops.actionsMenu")}
                        >
                          <MoreVertical className="h-4 w-4 text-muted-foreground" />
                        </button>
                      </DropdownMenuTrigger>
                      <DropdownMenuContent align="end" className="w-40">
                        <DropdownMenuItem
                          onClick={() => openVarietyForm(variety)}
                          className="cursor-pointer"
                        >
                          <Edit className="mr-2 h-4 w-4" />
                          {t("common.edit")}
                        </DropdownMenuItem>
                        <DropdownMenuSeparator />
                        <DropdownMenuItem
                          onClick={() =>
                            setDeleteConfirm({
                              type: "variety",
                              id: variety.id,
                              name: variety.name,
                            })
                          }
                          className="cursor-pointer text-destructive focus:text-destructive"
                        >
                          <Trash2 className="mr-2 h-4 w-4" />
                          {t("common.delete")}
                        </DropdownMenuItem>
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
    </div>
  );

  return (
    <AdminPageContainer>
      <AdminHeaderCard
        title={t("admin.crops.title")}
        description={t("admin.crops.subtitle")}
      />

      {/* Tab Navigation */}
      <div className="overflow-x-auto pb-1">
        <div className={adminTabsListClass}>
        <button
          onClick={() => setActiveTab("crops")}
          className={cn(
            "inline-flex h-8 items-center justify-center rounded-[18px] px-4 text-sm font-medium whitespace-nowrap transition-all",
            activeTab === "crops"
              ? "bg-card text-foreground shadow-sm"
              : "text-muted-foreground hover:text-foreground",
          )}
        >
          <Leaf className="inline-block h-4 w-4 mr-2" />
          {t("admin.crops.tabs.crops")}
        </button>
        <button
          onClick={() => setActiveTab("varieties")}
          className={cn(
            "inline-flex h-8 items-center justify-center rounded-[18px] px-4 text-sm font-medium whitespace-nowrap transition-all",
            activeTab === "varieties"
              ? "bg-card text-foreground shadow-sm"
              : "text-muted-foreground hover:text-foreground",
          )}
        >
          <Sprout className="inline-block h-4 w-4 mr-2" />
          {t("admin.crops.tabs.varieties")}
        </button>
        </div>
      </div>

      {/* Tab Content */}
      {activeTab === "crops" && renderCrops()}
      {activeTab === "varieties" && renderVarieties()}

      {/* Crop Form Modal */}
      {showCropForm && (
        <div className="fixed inset-0 z-50 bg-background/80 backdrop-blur-sm flex items-center justify-center">
          <div className="bg-card border border-border rounded-lg shadow-lg w-full max-w-md mx-4">
            <div className="flex items-center justify-between p-4 border-b border-border">
              <h3 className="text-lg font-semibold">
                {editingCrop ? t("admin.crops.form.editCrop") : t("admin.crops.form.addCrop")}
              </h3>
              <button
                onClick={() => setShowCropForm(false)}
                className="p-1 hover:bg-muted rounded"
              >
                <X className="h-4 w-4" />
              </button>
            </div>
            <div className="p-4 space-y-4">
              <div>
                <label className="block text-sm font-medium mb-1">{t("admin.crops.form.nameRequired")}</label>
                <input
                  type="text"
                  value={cropName}
                  onChange={(e) => setCropName(e.target.value)}
                  className="w-full px-3 py-2 border border-border rounded-lg bg-background text-sm"
                  placeholder={t("admin.crops.form.cropNamePlaceholder")}
                />
              </div>
              <div>
                <label className="block text-sm font-medium mb-1">
                  {t("common.description")}
                </label>
                <textarea
                  value={cropDescription}
                  onChange={(e) => setCropDescription(e.target.value)}
                  className="w-full px-3 py-2 border border-border rounded-lg bg-background text-sm min-h-[80px]"
                  placeholder={t("admin.crops.form.descriptionPlaceholder")}
                />
              </div>
            </div>
            <div className="flex justify-end gap-2 p-4 border-t border-border">
              <button
                onClick={() => setShowCropForm(false)}
                className="px-4 py-2 border border-border rounded-lg text-sm hover:bg-muted/50"
              >
                {t("common.cancel")}
              </button>
              <button
                onClick={handleSaveCrop}
                disabled={formLoading || !cropName.trim()}
                className="px-4 py-2 bg-primary text-primary-foreground rounded-lg text-sm hover:bg-primary/90 disabled:opacity-50"
              >
                {formLoading ? t("common.saving") : t("common.save")}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Variety Form Modal */}
      {showVarietyForm && (
        <div className="fixed inset-0 z-50 bg-background/80 backdrop-blur-sm flex items-center justify-center">
          <div className="bg-card border border-border rounded-lg shadow-lg w-full max-w-md mx-4">
            <div className="flex items-center justify-between p-4 border-b border-border">
              <h3 className="text-lg font-semibold">
                {editingVariety ? t("admin.crops.form.editVariety") : t("admin.crops.form.addVariety")}
              </h3>
              <button
                onClick={() => setShowVarietyForm(false)}
                className="p-1 hover:bg-muted rounded"
              >
                <X className="h-4 w-4" />
              </button>
            </div>
            <div className="p-4 space-y-4">
              <div>
                <label className="block text-sm font-medium mb-1">{t("admin.crops.form.cropRequired")}</label>
                <select
                  value={varietyCropId || ""}
                  onChange={(e) => setVarietyCropId(Number(e.target.value))}
                  className="w-full px-3 py-2 border border-border rounded-lg bg-background text-sm"
                >
                  <option value="">{t("admin.crops.form.selectCrop")}</option>
                  {crops.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.cropName}
                    </option>
                  ))}
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium mb-1">{t("admin.crops.form.nameRequired")}</label>
                <input
                  type="text"
                  value={varietyName}
                  onChange={(e) => setVarietyName(e.target.value)}
                  className="w-full px-3 py-2 border border-border rounded-lg bg-background text-sm"
                  placeholder={t("admin.crops.form.varietyNamePlaceholder")}
                />
              </div>
              <div>
                <label className="block text-sm font-medium mb-1">
                  {t("common.description")}
                </label>
                <textarea
                  value={varietyDescription}
                  onChange={(e) => setVarietyDescription(e.target.value)}
                  className="w-full px-3 py-2 border border-border rounded-lg bg-background text-sm min-h-[80px]"
                  placeholder={t("admin.crops.form.descriptionPlaceholder")}
                />
              </div>
            </div>
            <div className="flex justify-end gap-2 p-4 border-t border-border">
              <button
                onClick={() => setShowVarietyForm(false)}
                className="px-4 py-2 border border-border rounded-lg text-sm hover:bg-muted/50"
              >
                {t("common.cancel")}
              </button>
              <button
                onClick={handleSaveVariety}
                disabled={formLoading || !varietyName.trim() || !varietyCropId}
                className="px-4 py-2 bg-primary text-primary-foreground rounded-lg text-sm hover:bg-primary/90 disabled:opacity-50"
              >
                {formLoading ? t("common.saving") : t("common.save")}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Delete Confirmation Dialog */}
      {deleteConfirm && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="bg-card border border-border rounded-lg shadow-xl w-full max-w-md">
            <div className="p-4 border-b border-border">
              <h2 className="text-lg font-semibold text-destructive">
                {deleteConfirm.type === "crop"
                  ? t("admin.crops.delete.titleCrop")
                  : t("admin.crops.delete.titleVariety")}
              </h2>
            </div>
            <div className="p-4">
              <p className="text-sm text-muted-foreground mb-4">
                {t("admin.crops.delete.confirm", {
                  type:
                    deleteConfirm.type === "crop"
                      ? t("admin.crops.count.crop")
                      : t("admin.crops.count.variety"),
                })}{" "}
                <strong className="text-foreground">
                  "{deleteConfirm.name}"
                </strong>
                ?
              </p>
              <p className="text-xs text-muted-foreground">
                {deleteConfirm.type === "crop"
                  ? t("admin.crops.delete.cropWarning")
                  : t("admin.crops.delete.varietyWarning")}
              </p>
            </div>
            <div className="flex justify-end gap-2 p-4 border-t border-border">
              <button
                onClick={() => setDeleteConfirm(null)}
                disabled={deleteLoading}
                className="px-4 py-2 border border-border rounded-lg text-sm hover:bg-muted/50 disabled:opacity-50"
              >
                {t("common.cancel")}
              </button>
              <button
                onClick={handleDelete}
                disabled={deleteLoading}
                className="inline-flex items-center gap-2 px-4 py-2 bg-destructive text-destructive-foreground rounded-lg text-sm hover:bg-destructive/90 disabled:opacity-50"
              >
                {deleteLoading ? (
                  <>
                    <Loader2 className="h-4 w-4 animate-spin" />
                    {t("common.deleting")}
                  </>
                ) : (
                  <>
                    <Trash2 className="h-4 w-4" />
                    {t("common.delete")}
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
            <Leaf className="h-4 w-4" />
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
