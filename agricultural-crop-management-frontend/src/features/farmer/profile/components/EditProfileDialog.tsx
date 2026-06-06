import { Button } from "@/shared/ui/button";
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/shared/ui/form";
import { Input } from "@/shared/ui/input";
import { useProfileUpdate } from "@/entities/user";
import { useTranslation } from "react-i18next";
import {
  AddressSelector,
  AlertDialog,
  AlertDialogAction,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  BackButton,
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  type AddressValue,
} from "@/shared/ui";
import { zodResolver } from "@hookform/resolvers/zod";
import axios from "axios";
import { CheckCircle2, Loader2 } from "lucide-react";
import { useEffect, useMemo, useRef, useState } from "react";
import { Controller, useForm, useWatch } from "react-hook-form";
import {
  EditProfileFormSchema,
  type EditProfileFormData,
  type FarmerProfileData,
} from "../types";

interface EditProfileDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  profileData: FarmerProfileData;
}

/**
 * EditProfileDialog Component
 *
 * Modal dialog for editing farmer profile information:
 * - Display name
 * - Email
 * - Phone
 * - Address (Province/Ward selection)
 * - Bio (optional)
 *
 * Uses react-hook-form with zod validation
 */
export function EditProfileDialog({
  open,
  onOpenChange,
  profileData,
}: EditProfileDialogProps) {
  const { t } = useTranslation();
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [showSuccessDialog, setShowSuccessDialog] = useState(false);
  const wasOpenRef = useRef(false);

  const formValues = useMemo<EditProfileFormData>(
    () => ({
      fullName: profileData.displayName || "",
      phone:
        profileData.phone === "Not available" ? "" : profileData.phone || "",
      provinceId: profileData.provinceId,
      wardId: profileData.wardId,
    }),
    [profileData],
  );

  const form = useForm<EditProfileFormData>({
    resolver: zodResolver(EditProfileFormSchema),
    defaultValues: formValues,
  });

  const updateProfile = useProfileUpdate();
  const isSaving = form.formState.isSubmitting || updateProfile.isPending;
  const wardId = useWatch({ control: form.control, name: "wardId" });

  useEffect(() => {
    if (open && !wasOpenRef.current) {
      form.reset(formValues);
      setSubmitError(null);
    }

    if (!open && wasOpenRef.current) {
      setSubmitError(null);
    }

    wasOpenRef.current = open;
  }, [form, formValues, open]);

  const onSubmit = async (data: EditProfileFormData) => {
    setSubmitError(null);
    try {
      await updateProfile.mutateAsync({
        fullName: data.fullName,
        phone: data.phone || "",
        provinceId: data.provinceId,
        wardId: data.wardId,
      });

      // Close the edit dialog and show success dialog
      onOpenChange(false);
      setShowSuccessDialog(true);
      form.reset(data);
    } catch (error) {
      const message = axios.isAxiosError(error)
        ? (error.response?.data as { message?: string } | undefined)?.message
        : error instanceof Error
          ? error.message
          : undefined;
      setSubmitError(message || t("profile.editDialog.updateError"));
      console.error("Error updating profile:", error);
    }
  };
  const handleClose = () => {
    if (isSaving) return;
    if (
      form.formState.isDirty &&
      !window.confirm(t("common.unsavedChangesConfirm", "You have unsaved changes. Leave this page?"))
    ) {
      return;
    }

    onOpenChange(false);
  };

  return (
    <>
      <Dialog
        open={open}
        onOpenChange={(nextOpen) => {
          if (isSaving && !nextOpen) return;
          if (nextOpen) {
            onOpenChange(true);
          } else {
            handleClose();
          }
        }}
      >
        <DialogContent className="sm:max-w-[560px]" closeDisabled={isSaving}>
          <DialogHeader>
            <BackButton onClick={handleClose} className="w-fit" />
            <DialogTitle>{t("profile.editDialog.title")}</DialogTitle>
            <DialogDescription>
              {t("profile.editDialog.description")}
            </DialogDescription>
          </DialogHeader>

          {submitError && (
            <div className="rounded-md border border-destructive/30 bg-destructive/10 px-3 py-2 text-sm text-destructive">
              {submitError}
            </div>
          )}

          <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
              <div className="grid gap-4 sm:grid-cols-2">
                <FormField
                  control={form.control}
                  name="fullName"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel required>{t("profile.editDialog.fullName")}</FormLabel>
                      <FormControl>
                        <Input placeholder={t("profile.editDialog.fullNamePlaceholder")} {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <FormField
                  control={form.control}
                  name="phone"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>{t("profile.editDialog.phone")}</FormLabel>
                      <FormControl>
                        <Input placeholder={t("profile.editDialog.phonePlaceholder")} {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
              </div>

              <Controller
                name="provinceId"
                control={form.control}
                render={({
                  field: provinceField,
                  fieldState: provinceFieldState,
                }) => {
                  const wardError = form.formState.errors.wardId?.message;
                  const combinedError =
                    provinceFieldState.error?.message || wardError;

                  return (
                    <AddressSelector
                      value={{
                        provinceId: provinceField.value ?? null,
                        wardId: wardId ?? null,
                      }}
                      onChange={(address: AddressValue) => {
                        const nextProvinceId = address.provinceId ?? undefined;
                        const nextWardId = address.wardId ?? undefined;
                        provinceField.onChange(nextProvinceId);
                        form.setValue("wardId", nextWardId, { shouldDirty: true });
                      }}
                      error={combinedError}
                      disabled={isSaving}
                      label={t("profile.editDialog.location")}
                      description={t("profile.editDialog.locationDescription")}
                    />
                  );
                }}
              />

              <DialogFooter>
                <Button
                  type="button"
                  variant="outline"
                  onClick={handleClose}
                  disabled={isSaving}
                >
                  {t("common.cancel")}
                </Button>
                <Button
                  type="submit"
                  disabled={isSaving}
                >
                  {isSaving && (
                    <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  )}
                  {t("common.saveChanges")}
                </Button>
              </DialogFooter>
            </form>
          </Form>
        </DialogContent>
      </Dialog>

      {/* Success Dialog - shown after profile update */}
      <AlertDialog open={showSuccessDialog} onOpenChange={setShowSuccessDialog}>
        <AlertDialogContent className="max-w-sm">
          <AlertDialogHeader>
            <div className="flex flex-col items-center gap-3 text-center">
              <div className="flex h-12 w-12 items-center justify-center rounded-full bg-green-100">
                <CheckCircle2 className="h-6 w-6 text-green-600" />
              </div>
              <AlertDialogTitle>{t("profile.editDialog.successTitle")}</AlertDialogTitle>
              <AlertDialogDescription>
                {t("profile.editDialog.successDescription")}
              </AlertDialogDescription>
            </div>
          </AlertDialogHeader>
          <AlertDialogFooter className="justify-center sm:justify-center">
            <AlertDialogAction
              onClick={() => {
                setShowSuccessDialog(false);
                window.location.reload();
              }}
            >
              {t("common.ok")}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </>
  );
}
