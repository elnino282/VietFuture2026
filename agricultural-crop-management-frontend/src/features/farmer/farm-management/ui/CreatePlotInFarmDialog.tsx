/**
 * CreatePlotInFarmDialog Component
 *
 * Modal dialog for creating a new plot within a specific farm.
 * Includes form validation and status enum enforcement.
 */

import { Button } from "@/shared/ui/button";
import { BackButton } from "@/shared/ui/back-button";
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/shared/ui/form";
import { Input } from "@/shared/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/shared/ui/select";
import { useCreatePlotInFarm } from "@/entities/plot";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/shared/ui";
import { zodResolver } from "@hookform/resolvers/zod";
import type { TFunction } from "i18next";
import { Loader2 } from "lucide-react";
import { useMemo } from "react";
import { useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { toast } from "sonner";
import { z } from "zod";
import { PLOT_STATUS_OPTIONS, SOIL_TYPE_OPTIONS } from "@/features/farmer/shared/plotOptions";

// Form validation schema
const createPlotFormSchema = (t: TFunction) =>
  z.object({
    plotName: z.string().min(1, t("farms.validation.plotNameRequired")),
    area: z.number().positive(t("farms.validation.areaPositive")),
    soilType: z.string().optional(),
    status: z.enum(["IN_USE", "IDLE", "AVAILABLE", "FALLOW", "MAINTENANCE"], {
      required_error: t("farms.validation.statusRequired"),
    }),
  });

type CreatePlotFormData = z.infer<ReturnType<typeof createPlotFormSchema>>;

interface CreatePlotInFarmDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  farmId: number;
  farmName: string;
  onCreated?: () => void;
}

export function CreatePlotInFarmDialog({
  open,
  onOpenChange,
  farmId,
  farmName,
  onCreated,
}: CreatePlotInFarmDialogProps) {
  const { t } = useTranslation();
  const formSchema = useMemo(() => createPlotFormSchema(t), [t]);
  const form = useForm<CreatePlotFormData>({
    resolver: zodResolver(formSchema),
    defaultValues: {
      plotName: "",
      area: undefined,
      soilType: "",
      status: "IN_USE",
    },
  });

  const createPlotMutation = useCreatePlotInFarm({
    onSuccess: () => {
      toast.success(t("farms.toast.createPlotSuccess"));
      form.reset();
      onOpenChange(false);
      onCreated?.();
    },
    onError: (error: any) => {
      const errorCode = error?.response?.data?.code;
      let message = t("farms.toast.createPlotError");

      if (errorCode === "ERR_FARM_INACTIVE") {
        message = t("farms.toast.farmInactivePlots");
      } else if (errorCode === "ERR_PLOT_NAME_EXISTS") {
        message = t("farms.errors.plotNameExists");
      } else if (error?.message) {
        message = error.message;
      }

      toast.error(message);
    },
  });

  const handleSubmit = form.handleSubmit((data) => {
    createPlotMutation.mutate({
      farmId,
      data: {
        plotName: data.plotName,
        area: data.area,
        soilType: data.soilType || undefined,
        status: data.status,
      },
    });
  });
  const handleClose = () => {
    if (createPlotMutation.isPending) return;
    if (
      form.formState.isDirty &&
      !window.confirm(t("common.unsavedChangesConfirm", "You have unsaved changes. Leave this page?"))
    ) {
      return;
    }
    form.reset();
    onOpenChange(false);
  };

  return (
    <Dialog
      open={open}
      onOpenChange={(nextOpen) => {
        if (nextOpen) {
          onOpenChange(true);
          return;
        }
        handleClose();
      }}
    >
      <DialogContent className="sm:max-w-[500px]">
        <DialogHeader>
          <BackButton onClick={handleClose} className="w-fit" />
          <DialogTitle>{t("farms.dialog.createPlotTitle")}</DialogTitle>
          <DialogDescription>
            {t("farms.dialog.createPlotInFarmDescription", { farmName })}
          </DialogDescription>
        </DialogHeader>

        <Form {...form}>
          <form onSubmit={handleSubmit} className="space-y-4">
            {/* Plot Name */}
            <FormField
              control={form.control}
              name="plotName"
              render={({ field }) => (
                <FormItem>
                  <FormLabel required>{t("farms.form.plotName")}</FormLabel>
                  <FormControl>
                    <Input placeholder={t("farms.form.plotNameExample")} {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            {/* Area */}
            <FormField
              control={form.control}
              name="area"
              render={({ field }) => (
                <FormItem>
                  <FormLabel required>{t("farms.form.areaHectares")}</FormLabel>
                  <FormControl>
                    <Input
                      type="number"
                      step="0.01"
                      min="0.01"
                      placeholder={t("farms.form.areaExample")}
                      {...field}
                      onChange={(e) => {
                        const value = parseFloat(e.target.value);
                        field.onChange(isNaN(value) ? undefined : value);
                      }}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            {/* Soil Type */}
            <FormField
              control={form.control}
              name="soilType"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>{t("farms.form.soilType")}</FormLabel>
                  <Select onValueChange={field.onChange} value={field.value}>
                    <FormControl>
                      <SelectTrigger>
                        <SelectValue placeholder={t("farms.form.selectSoilType")} />
                      </SelectTrigger>
                    </FormControl>
                    <SelectContent>
                      {SOIL_TYPE_OPTIONS.map((type) => (
                          <SelectItem key={type.value} value={type.value}>
                            {t(type.labelKey)}
                          </SelectItem>
                        ))}
                    </SelectContent>
                  </Select>
                  <FormMessage />
                </FormItem>
              )}
            />

            {/* Status */}
            <FormField
              control={form.control}
              name="status"
              render={({ field }) => (
                <FormItem>
                  <FormLabel required>{t("farms.form.status")}</FormLabel>
                  <Select onValueChange={field.onChange} value={field.value}>
                    <FormControl>
                      <SelectTrigger>
                        <SelectValue placeholder={t("farms.form.selectStatus")} />
                      </SelectTrigger>
                    </FormControl>
                    <SelectContent>
                      {PLOT_STATUS_OPTIONS.map((status) => (
                          <SelectItem key={status.value} value={status.value}>
                            {t(status.labelKey)}
                          </SelectItem>
                        ))}
                    </SelectContent>
                  </Select>
                  <FormMessage />
                </FormItem>
              )}
            />

            <DialogFooter className="pt-4">
              <Button
                type="button"
                variant="outline"
                onClick={handleClose}
                disabled={createPlotMutation.isPending}
              >
                {t("common.cancel")}
              </Button>
              <Button type="submit" disabled={createPlotMutation.isPending}>
                {createPlotMutation.isPending && (
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                )}
                {t("farms.dialog.createPlotTitle")}
              </Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
}
