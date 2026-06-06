import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { z } from "zod";
import { Alert, AlertDescription, AlertTitle } from "@/shared/ui/alert";
import { Badge } from "@/shared/ui/badge";
import { BackButton } from "@/shared/ui/back-button";
import { Button } from "@/shared/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/shared/ui/card";
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
import { Textarea } from "@/shared/ui/textarea";
import { useSeasonById } from "@/entities/season";
import {
  useCreateNutrientInput,
  useSeasonNutrientInputs,
  type NutrientInputSourceType,
} from "@/entities/nutrient-input";
import { metricStatusClassName } from "@/features/farmer/dashboard/lib/metrics";
import { useI18n } from "@/shared/lib/hooks/useI18n";
import { AlertCircle, Beaker, Loader2 } from "lucide-react";
import { toast } from "sonner";

type Translator = (key: string, optionsOrDefault?: Record<string, unknown> | string) => string;

const createNutrientInputFormSchema = (t: Translator) =>
  z.object({
    inputSource: z.enum(["MINERAL_FERTILIZER", "ORGANIC_FERTILIZER"]),
    value: z
      .number({ required_error: t("seasonNutrientWorkspace.validation.valueRequired") })
      .min(0, t("seasonNutrientWorkspace.validation.nonNegative")),
    unit: z.enum(["kg_n", "kg_n_per_ha"]),
    recordedAt: z.string().min(1, t("seasonNutrientWorkspace.validation.recordedAtRequired")),
    sourceType: z.enum(["user_entered", "lab_measured", "external_reference"]),
    sourceDocument: z.string().trim().max(255).optional(),
    note: z.string().trim().max(4000).optional(),
  });

type NutrientInputFormSchemaType = ReturnType<typeof createNutrientInputFormSchema>;
type NutrientInputFormValues = z.infer<NutrientInputFormSchemaType>;

const SOURCE_OPTIONS: { value: NutrientInputSourceType; labelKey: string; fallbackLabel: string }[] = [
  {
    value: "user_entered",
    labelKey: "seasonNutrientWorkspace.form.sourceTypeOptions.userEntered",
    fallbackLabel: "User entered",
  },
  {
    value: "lab_measured",
    labelKey: "seasonNutrientWorkspace.form.sourceTypeOptions.labMeasured",
    fallbackLabel: "Lab measured",
  },
  {
    value: "external_reference",
    labelKey: "seasonNutrientWorkspace.form.sourceTypeOptions.externalReference",
    fallbackLabel: "External reference",
  },
];

const INPUT_SOURCE_OPTIONS = [
  {
    value: "MINERAL_FERTILIZER",
    labelKey: "seasonNutrientWorkspace.form.inputSourceOptions.mineral",
    fallbackLabel: "Mineral fertilizer",
  },
  {
    value: "ORGANIC_FERTILIZER",
    labelKey: "seasonNutrientWorkspace.form.inputSourceOptions.organic",
    fallbackLabel: "Organic fertilizer / manure",
  },
] as const;

const UNIT_OPTIONS = [
  {
    value: "kg_n",
    labelKey: "seasonNutrientWorkspace.form.unitOptions.kgN",
    fallbackLabel: "kg N / season",
  },
  {
    value: "kg_n_per_ha",
    labelKey: "seasonNutrientWorkspace.form.unitOptions.kgNPerHa",
    fallbackLabel: "kg N / ha",
  },
] as const;

const FORM_INPUT_CLASS_NAME =
  "h-11 rounded-xl border border-slate-300 bg-white px-4 text-sm shadow-sm placeholder:text-slate-400 hover:border-slate-400 focus:border-[#3BA55D] focus:ring-2 focus:ring-[#3BA55D]/20 focus-visible:border-[#3BA55D] focus-visible:ring-2 focus-visible:ring-[#3BA55D]/20";

const FORM_SELECT_TRIGGER_CLASS_NAME =
  "h-11 rounded-xl border border-slate-300 bg-white px-4 text-sm shadow-sm data-[placeholder]:text-slate-400 hover:border-slate-400 focus:border-[#3BA55D] focus:ring-2 focus:ring-[#3BA55D]/20 focus-visible:border-[#3BA55D] focus-visible:ring-2 focus-visible:ring-[#3BA55D]/20";

const FORM_TEXTAREA_CLASS_NAME =
  "rounded-xl border border-slate-300 bg-white px-4 py-3 text-sm shadow-sm placeholder:text-slate-400 hover:border-slate-400 focus:border-[#3BA55D] focus:ring-2 focus:ring-[#3BA55D]/20 focus-visible:border-[#3BA55D] focus-visible:ring-2 focus-visible:ring-[#3BA55D]/20";

const defaultFormValues = (): NutrientInputFormValues => ({
  inputSource: "MINERAL_FERTILIZER",
  value: 0,
  unit: "kg_n",
  recordedAt: new Date().toISOString().split("T")[0],
  sourceType: "user_entered",
  sourceDocument: "",
  note: "",
});

const formatDate = (value: string | null | undefined, locale: string) => {
  if (!value) {
    return "-";
  }
  const parsed = new Date(`${value}T00:00:00`);
  if (Number.isNaN(parsed.getTime())) {
    return value;
  }
  return parsed.toLocaleDateString(locale);
};

export function SeasonNutrientInputsWorkspace() {
  const { seasonId } = useParams();
  const navigate = useNavigate();
  const { t, locale } = useI18n();
  const seasonIdNumber = Number(seasonId);
  const hasValidSeasonId = Number.isFinite(seasonIdNumber) && seasonIdNumber > 0;
  const [submitMode, setSubmitMode] = useState<"stay" | "dashboard">("stay");

  const formSchema = useMemo(() => createNutrientInputFormSchema(t), [t]);
  const form = useForm<NutrientInputFormValues>({
    resolver: zodResolver(formSchema),
    defaultValues: defaultFormValues(),
  });

  const { data: seasonDetail, isLoading: isSeasonLoading } = useSeasonById(seasonIdNumber, {
    enabled: hasValidSeasonId,
  });

  const { data: nutrientInputs, isLoading: isInputsLoading } = useSeasonNutrientInputs(
    seasonIdNumber,
    undefined,
    {
      enabled: hasValidSeasonId,
    }
  );

  const createMutation = useCreateNutrientInput(seasonIdNumber, {
    onSuccess: () => {
      toast.success(t("seasonNutrientWorkspace.toast.createSuccess"));
      form.reset({
        ...defaultFormValues(),
        inputSource: form.getValues("inputSource"),
        unit: form.getValues("unit"),
        sourceType: form.getValues("sourceType"),
      });

      if (submitMode === "dashboard") {
        navigate("/farmer/dashboard");
      }
    },
    onError: (error) => {
      toast.error(error.message || t("seasonNutrientWorkspace.toast.createError"));
    },
  });

  const plotId = seasonDetail?.plotId ?? null;
  const isSeasonWriteLocked =
    seasonDetail?.status === "COMPLETED"
    || seasonDetail?.status === "CANCELLED"
    || seasonDetail?.status === "ARCHIVED";
  const seasonWriteLockReason = isSeasonWriteLocked
    ? t("seasonNutrientWorkspace.alerts.writeLockedDescription")
    : undefined;

  const createdRecords = useMemo(() => nutrientInputs ?? [], [nutrientInputs]);
  const getInputSourceLabel = (value?: string | null) => {
    const option = INPUT_SOURCE_OPTIONS.find((item) => item.value === value);
    if (!option) return value ?? "N/A";
    return t(option.labelKey, option.fallbackLabel);
  };
  const getSourceTypeLabel = (value?: string | null) => {
    const option = SOURCE_OPTIONS.find((item) => item.value === value);
    if (!option) return value ?? "N/A";
    return t(option.labelKey, option.fallbackLabel);
  };

  const onSubmit = form.handleSubmit((values) => {
    if (isSeasonWriteLocked) {
      toast.error(seasonWriteLockReason);
      return;
    }
    if (!plotId) {
      toast.error(t("seasonNutrientWorkspace.validation.plotRequired"));
      return;
    }

    createMutation.mutate({
      plotId,
      inputSource: values.inputSource,
      value: values.value,
      unit: values.unit,
      recordedAt: values.recordedAt,
      sourceType: values.sourceType,
      sourceDocument: values.sourceDocument?.trim() || undefined,
      note: values.note?.trim() || undefined,
    });
  });

  if (!hasValidSeasonId) {
    return (
      <div className="space-y-4">
        <BackButton to="/farmer/seasons" variant="outline" />
        <Card className="border border-destructive/20 bg-destructive/5">
          <CardContent className="p-6 space-y-3">
            <div className="flex items-start gap-3">
              <AlertCircle className="h-5 w-5 text-destructive mt-0.5" />
              <div className="space-y-2">
                <p className="text-sm text-foreground">{t("seasonWorkspace.invalidSeason")}</p>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <Card className="border border-border rounded-2xl shadow-sm">
        <CardHeader>
          <CardTitle className="text-base flex items-center gap-2">
            <Beaker className="w-5 h-5 text-primary" />
            {t("seasonNutrientWorkspace.title")}
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          {isSeasonLoading ? (
            <div className="text-sm text-muted-foreground flex items-center gap-2">
              <Loader2 className="w-4 h-4 animate-spin" />
              {t("seasonNutrientWorkspace.loadingSeason")}
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
              <div className="rounded-xl border border-border p-3 bg-card">
                <p className="text-xs text-muted-foreground mb-1">{t("seasonNutrientWorkspace.meta.season")}</p>
                <p className="text-sm text-foreground">{seasonDetail?.seasonName ?? "-"}</p>
              </div>
              <div className="rounded-xl border border-border p-3 bg-card">
                <p className="text-xs text-muted-foreground mb-1">{t("seasonNutrientWorkspace.meta.plot")}</p>
                <p className="text-sm text-foreground">{seasonDetail?.plotName ?? "-"}</p>
              </div>
              <div className="rounded-xl border border-border p-3 bg-card">
                <p className="text-xs text-muted-foreground mb-1">{t("seasonNutrientWorkspace.meta.crop")}</p>
                <p className="text-sm text-foreground">{seasonDetail?.cropName ?? "-"}</p>
              </div>
            </div>
          )}

          {!plotId && !isSeasonLoading && (
            <Alert variant="destructive">
              <AlertCircle className="h-4 w-4" />
              <AlertTitle>{t("seasonNutrientWorkspace.alerts.missingPlotTitle")}</AlertTitle>
              <AlertDescription>{t("seasonNutrientWorkspace.alerts.missingPlotDescription")}</AlertDescription>
            </Alert>
          )}

          {isSeasonWriteLocked && (
            <Alert>
              <AlertCircle className="h-4 w-4" />
              <AlertTitle>{t("seasonNutrientWorkspace.alerts.writeLockedTitle")}</AlertTitle>
              <AlertDescription>{seasonWriteLockReason}</AlertDescription>
            </Alert>
          )}

          <Form {...form}>
            <form onSubmit={onSubmit} className="space-y-4" data-testid="nutrient-input-form">
              <fieldset disabled={isSeasonWriteLocked}>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <FormField
                    control={form.control}
                    name="inputSource"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>{t("seasonNutrientWorkspace.form.inputSource")} *</FormLabel>
                        <Select value={field.value} onValueChange={field.onChange}>
                          <FormControl>
                            <SelectTrigger className={FORM_SELECT_TRIGGER_CLASS_NAME}>
                              <SelectValue placeholder={t("seasonNutrientWorkspace.form.inputSourcePlaceholder")} />
                            </SelectTrigger>
                          </FormControl>
                          <SelectContent>
                            {INPUT_SOURCE_OPTIONS.map((item) => (
                              <SelectItem key={item.value} value={item.value}>
                                {t(item.labelKey, item.fallbackLabel)}
                              </SelectItem>
                            ))}
                          </SelectContent>
                        </Select>
                        <FormMessage />
                      </FormItem>
                    )}
                  />

                  <FormField
                    control={form.control}
                    name="sourceType"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>{t("seasonNutrientWorkspace.form.sourceType")} *</FormLabel>
                        <Select value={field.value} onValueChange={field.onChange}>
                          <FormControl>
                            <SelectTrigger className={FORM_SELECT_TRIGGER_CLASS_NAME}>
                              <SelectValue placeholder={t("seasonNutrientWorkspace.form.sourceTypePlaceholder")} />
                            </SelectTrigger>
                          </FormControl>
                          <SelectContent>
                            {SOURCE_OPTIONS.map((item) => (
                              <SelectItem key={item.value} value={item.value}>
                                {t(item.labelKey, item.fallbackLabel)}
                              </SelectItem>
                            ))}
                          </SelectContent>
                        </Select>
                        <FormMessage />
                      </FormItem>
                    )}
                  />

                  <FormField
                    control={form.control}
                    name="value"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>{t("seasonNutrientWorkspace.form.value")} *</FormLabel>
                        <FormControl>
                          <Input
                            type="number"
                            step="0.0001"
                            min="0"
                            data-testid="nutrient-value-input"
                            value={Number.isFinite(field.value) ? field.value : ""}
                            onChange={(event) => {
                              const nextValue = Number.parseFloat(event.target.value);
                              field.onChange(Number.isNaN(nextValue) ? undefined : nextValue);
                            }}
                            placeholder={t("seasonNutrientWorkspace.form.valuePlaceholder")}
                            className={FORM_INPUT_CLASS_NAME}
                          />
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />

                  <FormField
                    control={form.control}
                    name="unit"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>{t("seasonNutrientWorkspace.form.unit")} *</FormLabel>
                        <Select value={field.value} onValueChange={field.onChange}>
                          <FormControl>
                            <SelectTrigger className={FORM_SELECT_TRIGGER_CLASS_NAME}>
                              <SelectValue placeholder={t("seasonNutrientWorkspace.form.unitPlaceholder")} />
                            </SelectTrigger>
                          </FormControl>
                          <SelectContent>
                            {UNIT_OPTIONS.map((item) => (
                              <SelectItem key={item.value} value={item.value}>
                                {t(item.labelKey, item.fallbackLabel)}
                              </SelectItem>
                            ))}
                          </SelectContent>
                        </Select>
                        <FormMessage />
                      </FormItem>
                    )}
                  />

                  <FormField
                    control={form.control}
                    name="recordedAt"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>{t("seasonNutrientWorkspace.form.recordedAt")} *</FormLabel>
                        <FormControl>
                          <Input
                            type="date"
                            data-testid="nutrient-recorded-at-input"
                            value={field.value}
                            onChange={field.onChange}
                            className={FORM_INPUT_CLASS_NAME}
                          />
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />

                  <FormField
                    control={form.control}
                    name="sourceDocument"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>{t("seasonNutrientWorkspace.form.sourceDocument")}</FormLabel>
                        <FormControl>
                          <Input
                            value={field.value ?? ""}
                            onChange={field.onChange}
                            placeholder={t("seasonNutrientWorkspace.form.sourceDocumentPlaceholder")}
                            className={FORM_INPUT_CLASS_NAME}
                          />
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                </div>

                <FormField
                  control={form.control}
                  name="note"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>{t("seasonNutrientWorkspace.form.note")}</FormLabel>
                      <FormControl>
                        <Textarea
                          rows={3}
                          value={field.value ?? ""}
                          onChange={field.onChange}
                          placeholder={t("seasonNutrientWorkspace.form.notePlaceholder")}
                          className={FORM_TEXTAREA_CLASS_NAME}
                        />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <div className="flex flex-wrap gap-2">
                  <Button
                    type="submit"
                    data-testid="submit-nutrient-input"
                    disabled={createMutation.isPending || !plotId || isSeasonWriteLocked}
                    onClick={() => setSubmitMode("stay")}
                  >
                    {createMutation.isPending && submitMode === "stay" ? (
                      <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                    ) : null}
                    {t("seasonNutrientWorkspace.actions.save")}
                  </Button>
                  <Button
                    type="submit"
                    data-testid="submit-nutrient-input-dashboard"
                    variant="outline"
                    disabled={createMutation.isPending || !plotId || isSeasonWriteLocked}
                    onClick={() => setSubmitMode("dashboard")}
                  >
                    {createMutation.isPending && submitMode === "dashboard" ? (
                      <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                    ) : null}
                    {t("seasonNutrientWorkspace.actions.saveAndOpenDashboard")}
                  </Button>
                </div>
              </fieldset>
            </form>
          </Form>
        </CardContent>
      </Card>

      <Card className="border border-border rounded-2xl shadow-sm">
        <CardHeader>
          <CardTitle className="text-base">{t("seasonNutrientWorkspace.list.title")}</CardTitle>
        </CardHeader>
        <CardContent>
          {isInputsLoading ? (
            <div className="text-sm text-muted-foreground flex items-center gap-2">
              <Loader2 className="w-4 h-4 animate-spin" />
              {t("seasonNutrientWorkspace.list.loading")}
            </div>
          ) : createdRecords.length === 0 ? (
            <p className="text-sm text-muted-foreground">{t("seasonNutrientWorkspace.list.empty")}</p>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-border text-muted-foreground">
                    <th className="text-left font-medium py-2 pr-3">{t("seasonNutrientWorkspace.list.headers.date")}</th>
                    <th className="text-left font-medium py-2 pr-3">{t("seasonNutrientWorkspace.list.headers.inputType")}</th>
                    <th className="text-left font-medium py-2 pr-3">{t("seasonNutrientWorkspace.list.headers.value")}</th>
                    <th className="text-left font-medium py-2 pr-3">{t("seasonNutrientWorkspace.list.headers.source")}</th>
                    <th className="text-left font-medium py-2 pr-3">{t("seasonNutrientWorkspace.list.headers.status")}</th>
                  </tr>
                </thead>
                <tbody>
                  {createdRecords.map((item) => (
                    <tr key={item.id} className="border-b border-border/60">
                      <td className="py-2 pr-3">{formatDate(item.recordedAt, locale)}</td>
                      <td className="py-2 pr-3">
                        {getInputSourceLabel(item.inputSource)}
                      </td>
                      <td className="py-2 pr-3">
                        {item.value} {item.unit}
                      </td>
                      <td className="py-2 pr-3">
                        {getSourceTypeLabel(item.sourceType)}
                      </td>
                      <td className="py-2 pr-3">
                        <Badge className={metricStatusClassName(item.status)}>{item.status ?? "unavailable"}</Badge>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
