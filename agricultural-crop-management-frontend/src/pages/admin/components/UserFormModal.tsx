import {
  BackButton,
  Button,
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  Input,
  Label,
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/shared/ui";
import { useI18n } from "@/shared/lib/hooks/useI18n";
import {
  adminRoleApi,
  adminUsersApi,
  type AdminUser,
  type Role,
} from "@/features/admin/shared/api";
import {
  Edit,
  Loader2,
  Mail,
  Phone,
  Plus,
  Save,
  Shield,
  User,
  X,
} from "lucide-react";
import { useEffect, useRef, useState } from "react";
import { toast } from "sonner";

export interface UserFormData {
  username: string;
  password: string;
  email: string;
  fullName: string;
  phone: string;
  roles: string[];
  status: string;
}

const initialFormData: UserFormData = {
  username: "",
  password: "",
  email: "",
  fullName: "",
  phone: "",
  roles: ["FARMER"],
  status: "ACTIVE",
};

interface UserFormModalProps {
  isOpen: boolean;
  onOpenChange: (open: boolean) => void;
  user: AdminUser | null; // null for create, object for edit
  onSuccess: () => void;
}

export function UserFormModal({
  isOpen,
  onOpenChange,
  user,
  onSuccess,
}: UserFormModalProps) {
  const { t } = useI18n();
  const [formData, setFormData] = useState<UserFormData>(initialFormData);
  const [loading, setLoading] = useState(false);
  const [roles, setRoles] = useState<Role[]>([]);
  const [loadingRoles, setLoadingRoles] = useState(false);
  const [errors, setErrors] = useState<
    Partial<Record<keyof UserFormData, string>>
  >({});
  const initialFormRef = useRef<string | null>(null);

  const isEditMode = !!user;

  // Fetch roles when modal opens
  useEffect(() => {
    if (isOpen) {
      fetchRoles();
      const nextFormData = user
        ? {
            username: user.username || "",
            password: "",
            email: user.email || "",
            fullName: user.fullName || "",
            phone: user.phone || "",
            roles: user.roles || ["FARMER"],
            status: user.status || "ACTIVE",
          }
        : initialFormData;
      if (user) {
        // Edit mode: populate form with user data
        setFormData(nextFormData);
      } else {
        // Create mode: reset form
        setFormData(nextFormData);
      }
      initialFormRef.current = JSON.stringify(nextFormData);
      setErrors({});
    }
  }, [isOpen, user]);

  const fetchRoles = async () => {
    setLoadingRoles(true);
    try {
      const rolesList = await adminRoleApi.list();
      setRoles(rolesList || []);
    } catch (err) {
      console.error("Failed to load roles:", err);
    } finally {
      setLoadingRoles(false);
    }
  };

  const validateForm = (): boolean => {
    const newErrors: Partial<Record<keyof UserFormData, string>> = {};

    if (!formData.username || formData.username.length < 3) {
      newErrors.username = t("admin.users.form.validation.usernameMin");
    }

    if (!isEditMode && (!formData.password || formData.password.length < 8)) {
      newErrors.password = t("validation.passwordMinLength");
    }

    if (formData.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email)) {
      newErrors.email = t("validation.invalidEmail");
    }

    if (formData.roles.length === 0) {
      newErrors.roles = t("admin.users.form.validation.roleRequired");
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async () => {
    if (!validateForm()) {
      return;
    }

    setLoading(true);
    try {
      if (isEditMode && user) {
        // Update existing user
        await adminUsersApi.update(Number(user.id), {
          username: formData.username,
          email: formData.email || undefined,
          fullName: formData.fullName || undefined,
          phone: formData.phone || undefined,
          roles: formData.roles,
          status: formData.status,
        });
        toast.success(t("admin.users.form.toast.updated"));
      } else {
        // Create new user
        await adminUsersApi.create({
          username: formData.username,
          password: formData.password,
          email: formData.email || undefined,
          fullName: formData.fullName || undefined,
          phone: formData.phone || undefined,
          roles: formData.roles,
        });
        toast.success(t("admin.users.form.toast.created"));
      }
      onSuccess();
      onOpenChange(false);
    } catch (err: any) {
      console.error("Failed to save user:", err);
      const errorCode = err?.response?.data?.code;
      if (errorCode === "ERR_USERNAME_ALREADY_EXISTS") {
        setErrors({ username: t("admin.users.form.validation.usernameTaken") });
      } else if (errorCode === "ERR_EMAIL_ALREADY_EXISTS") {
        setErrors({ email: t("admin.users.form.validation.emailTaken") });
      } else {
        toast.error(
          isEditMode ? t("admin.users.form.toast.updateFailed") : t("admin.users.form.toast.createFailed"),
        );
      }
    } finally {
      setLoading(false);
    }
  };

  const handleClose = () => {
    const isDirty = isOpen && !!initialFormRef.current && initialFormRef.current !== JSON.stringify(formData);
    if (
      isDirty &&
      !window.confirm(t("common.unsavedChangesConfirm", "You have unsaved changes. Leave this page?"))
    ) {
      return;
    }
    onOpenChange(false);
    setFormData(initialFormData);
    initialFormRef.current = null;
    setErrors({});
  };

  const handleRoleToggle = (roleCode: string) => {
    setFormData((prev) => {
      const hasRole = prev.roles.includes(roleCode);
      const newRoles = hasRole
        ? prev.roles.filter((r) => r !== roleCode)
        : [...prev.roles, roleCode];
      return { ...prev, roles: newRoles };
    });
  };

  return (
    <Dialog
      open={isOpen}
      onOpenChange={(nextOpen) => {
        if (!nextOpen) {
          handleClose();
          return;
        }
        onOpenChange(true);
      }}
    >
      <DialogContent className="max-h-[90vh] max-w-lg overflow-y-auto rounded-[18px] border-border bg-card shadow-sm">
        <DialogHeader>
          <BackButton onClick={handleClose} className="w-fit" />
          <DialogTitle className="flex items-center gap-2 text-xl">
            {isEditMode ? (
              <>
                <Edit className="w-5 h-5 text-primary" />
                {t("admin.users.form.editTitle")}
              </>
            ) : (
              <>
                <Plus className="w-5 h-5 text-green-600" />
                {t("admin.users.form.addTitle")}
              </>
            )}
          </DialogTitle>
          <DialogDescription className="text-sm text-muted-foreground">
            {isEditMode
              ? t("admin.users.form.editDescription")
              : t("admin.users.form.addDescription")}{" "}
            {t("admin.roles.form.requiredHint")}
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4 py-4">
          {/* Username */}
          <div className="space-y-2">
            <Label htmlFor="username">
              {t("admin.users.table.username")} <span className="text-destructive">*</span>
            </Label>
            <div className="relative">
              <User className="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-muted-foreground" />
              <Input
                id="username"
                placeholder={t("admin.users.form.usernamePlaceholder")}
                value={formData.username}
                onChange={(e) =>
                  setFormData({ ...formData, username: e.target.value })
                }
                className={`rounded-[14px] pl-10 ${errors.username ? "border-destructive" : ""}`}
              />
            </div>
            {errors.username && (
              <p className="text-xs text-destructive">{errors.username}</p>
            )}
          </div>

          {/* Password (only for create mode) */}
          {!isEditMode && (
            <div className="space-y-2">
              <Label htmlFor="password">
                {t("auth.signIn.password")} <span className="text-destructive">*</span>
              </Label>
              <Input
                id="password"
                type="password"
                placeholder={t("admin.users.form.passwordPlaceholder")}
                value={formData.password}
                onChange={(e) =>
                  setFormData({ ...formData, password: e.target.value })
                }
                className={`rounded-[14px] ${errors.password ? "border-destructive" : ""}`}
              />
              {errors.password && (
                <p className="text-xs text-destructive">{errors.password}</p>
              )}
            </div>
          )}

          {/* Email */}
          <div className="space-y-2">
            <Label htmlFor="email">{t("auth.signIn.email")}</Label>
            <div className="relative">
              <Mail className="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-muted-foreground" />
              <Input
                id="email"
                type="email"
                placeholder={t("auth.signUp.emailPlaceholder")}
                value={formData.email}
                onChange={(e) =>
                  setFormData({ ...formData, email: e.target.value })
                }
                className={`rounded-[14px] pl-10 ${errors.email ? "border-destructive" : ""}`}
              />
            </div>
            {errors.email && (
              <p className="text-xs text-destructive">{errors.email}</p>
            )}
          </div>

          {/* Full Name */}
          <div className="space-y-2">
            <Label htmlFor="fullName">{t("auth.signUp.fullName")}</Label>
            <Input
              id="fullName"
              placeholder={t("admin.users.form.fullNamePlaceholder")}
              value={formData.fullName}
              onChange={(e) =>
                setFormData({ ...formData, fullName: e.target.value })
              }
              className="rounded-[14px]"
            />
          </div>

          {/* Phone */}
          <div className="space-y-2">
            <Label htmlFor="phone">{t("admin.users.form.phone")}</Label>
            <div className="relative">
              <Phone className="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-muted-foreground" />
              <Input
                id="phone"
                placeholder={t("admin.users.form.phonePlaceholder")}
                value={formData.phone}
                onChange={(e) =>
                  setFormData({ ...formData, phone: e.target.value })
                }
                className="rounded-[14px] pl-10"
              />
            </div>
          </div>

          {/* Roles */}
          <div className="space-y-2">
            <Label className="flex items-center gap-2">
              <Shield className="w-4 h-4" />
              {t("admin.users.table.roles")} <span className="text-destructive">*</span>
            </Label>
            <div className="flex flex-wrap gap-2 rounded-[14px] border bg-muted/30 p-3">
              {loadingRoles ? (
                <div className="flex items-center gap-2 text-muted-foreground text-sm">
                  <Loader2 className="w-4 h-4 animate-spin" />
                  {t("admin.users.form.loadingRoles")}
                </div>
              ) : roles.length === 0 ? (
                <span className="text-muted-foreground text-sm">
                  {t("admin.users.form.noRolesAvailable")}
                </span>
              ) : (
                roles.map((role) => {
                  const isSelected = formData.roles.includes(role.code || "");
                  return (
                    <button
                      key={role.id || role.code}
                      type="button"
                      onClick={() => handleRoleToggle(role.code || "")}
                      className={`px-3 py-1.5 rounded-full text-sm font-medium transition-colors ${
                        isSelected
                          ? "bg-primary text-primary-foreground"
                          : "bg-background border border-border hover:bg-muted"
                      }`}
                    >
                      {role.name || role.code}
                    </button>
                  );
                })
              )}
            </div>
            {errors.roles && (
              <p className="text-xs text-destructive">{errors.roles}</p>
            )}
          </div>

          {/* Status (only for edit mode) */}
          {isEditMode && (
            <div className="space-y-2">
              <Label htmlFor="status">{t("common.status")}</Label>
              <Select
                value={formData.status}
                onValueChange={(value) =>
                  setFormData({ ...formData, status: value })
                }
              >
                <SelectTrigger className="rounded-[14px]">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="ACTIVE">
                    <div className="flex items-center gap-2">
                      <div className="w-2 h-2 rounded-full bg-green-500" />
                      {t("common.active")}
                    </div>
                  </SelectItem>
                  <SelectItem value="INACTIVE">
                    <div className="flex items-center gap-2">
                      <div className="w-2 h-2 rounded-full bg-gray-500" />
                      {t("common.inactive")}
                    </div>
                  </SelectItem>
                  <SelectItem value="LOCKED">
                    <div className="flex items-center gap-2">
                      <div className="w-2 h-2 rounded-full bg-red-500" />
                      {t("admin.users.status.locked")}
                    </div>
                  </SelectItem>
                </SelectContent>
              </Select>
            </div>
          )}
        </div>

        <DialogFooter>
          <Button
            variant="outline"
            onClick={handleClose}
            disabled={loading}
            className="rounded-[14px]"
          >
            <X className="w-4 h-4 mr-2" />
            {t("common.cancel")}
          </Button>
          <Button
            onClick={handleSubmit}
            disabled={loading}
            className="rounded-[14px] bg-primary hover:bg-primary/90"
          >
            {loading ? (
              <Loader2 className="w-4 h-4 mr-2 animate-spin" />
            ) : (
              <Save className="w-4 h-4 mr-2" />
            )}
            {isEditMode ? t("common.update") : t("common.create")} {t("admin.users.form.user")}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
