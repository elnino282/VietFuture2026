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
  useCreateIrrigationWaterAnalysis,
  useSeasonIrrigationWaterAnalyses,
  type IrrigationSourceType,
} from "@/entities/irrigation-water-analysis";
import { metricStatusClassName } from "@/features/farmer/dashboard/lib/metrics";
import { useI18n } from "@/shared/lib/hooks/useI18n";
import { AlertCircle, Droplets, Loader2 } from "lucide-react";
import { toast } from "sonner";

type Translator = (key: string, optionsOrDefault?: Record<string, unknown> | string) => string;

const createIrrigationWaterAnalysisFormSchema = (t: Translator) =>
  z
    .object({
      sampleDate: z.string().min(1, t("seasonIrrigationWorkspace.validation.sampleDateRequired")),
      nitrateMgPerL: z.number().min(0, t("seasonIrrigationWorkspace.validation.nonNegative")).optional(),
      ammoniumMgPerL: z.number().min(0, t("seasonIrrigationWorkspace.validation.nonNegative")).optional(),
      totalNmgPerL: z.number().min(0, t("seasonIrrigationWorkspace.validation.nonNegative")).optional(),
      irrigationVolumeM3: z
        .number({ required_error: t("seasonIrrigationWorkspace.validation.volumeRequired") })
        .min(0, t("seasonIrrigationWorkspace.validation.nonNegative")),
      sourceType: z.enum(["user_entered", "lab_measured", "external_reference"]),
      sourceDocument: z.string().trim().max(255).optional(),
      labReference: z.string().trim().max(255).optional(),
      note: z.string().trim().max(4000).optional(),
    })
    .superRefine((value, context) => {
      if (
        value.totalNmgPerL === undefined
        && value.nitrateMgPerL === undefined
        && value.ammoniumMgPerL === undefined
      ) {
        context.addIssue({
          code: z.ZodIssueCode.custom,
          message: t("seasonIrrigationWorkspace.validation.nitrogenRequired"),
          path: ["totalNmgPerL"],
        });
      }
    });

type IrrigationWaterAnalysisFormSchemaType = ReturnType<typeof createIrrigationWaterAnalysisFormSchema>;
type IrrigationWaterAnalysisFormValues = z.infer<IrrigationWaterAnalysisFormSchemaType>;

const SOURCE_OPTIONS: { value: IrrigationSourceType; labelKey: string; fallbackLabel: string }[] = [
  {
    value: "user_entered",
    labelKey: "seasonIrrigationWorkspace.form.sourceTypeOptions.userEntered",
    fallbackLabel: "User entered",
  },
  {
    value: "lab_measured",
    labelKey: "seasonIrrigationWorkspace.form.sourceTypeOptions.labMeasured",
    fallbackLabel: "Lab measured",
  },
  {
    value: "external_reference",
    labelKey: "seasonIrrigationWorkspace.form.sourceTypeOptions.externalReference",
    fallbackLabel: "External reference",
  },
];

const FORM_INPUT_CLASS_NAME =
  "h-11 rounded-xl border border-slate-300 bg-white px-4 text-sm shadow-sm placeholder:text-slate-400 hover:border-slate-400 focus:border-[#3BA55D] focus:ring-2 focus:ring-[#3BA55D]/20 focus-visible:border-[#3BA55D] focus-visible:ring-2 focus-visible:ring-[#3BA55D]/20";

const FORM_SELECT_TRIGGER_CLASS_NAME =
  "h-11 rounded-xl border border-slate-300 bg-white px-4 text-sm shadow-sm data-[placeholder]:text-slate-400 hover:border-slate-400 focus:border-[#3BA55D] focus:ring-2 focus:ring-[#3BA55D]/20 focus-visible:border-[#3BA55D] focus-visible:ring-2 focus-visible:ring-[#3BA55D]/20";

const FORM_TEXTAREA_CLASS_NAME =
  "rounded-xl border border-slate-300 bg-white px-4 py-3 text-sm shadow-sm placeholder:text-slate-400 hover:border-slate-400 focus:border-[#3BA55D] focus:ring-2 focus:ring-[#3BA55D]/20 focus-visible:border-[#3BA55D] focus-visible:ring-2 focus-visible:ring-[#3BA55D]/20";

const defaultValues = (): IrrigationWaterAnalysisFormValues => ({
  sampleDate: new Date().toISOString().split("T")[0],
  nitrateMgPerL: undefined,
  ammoniumMgPerL: undefined,
  totalNmgPerL: undefined,
  irrigationVolumeM3: 0,
  sourceType: "user_entered",
  sourceDocument: "",
  labReference: "",
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

const formatNumber = (value?: number | null, fallback = "-") => {
  if (value === null || value === undefined) {
    return fallback;
  }
  return value.toFixed(4);
};

function toNumberOrUndefined(raw: string): number | undefined {
  const parsed = Number.parseFloat(raw);
  return Number.isNaN(parsed) ? undefined : parsed;
}

export function SeasonIrrigationWaterAnalysesWorkspace() {
  const { seasonId } = useParams();
  const navigate = useNavigate();
  const { t, locale } = useI18n();
  const seasonIdNumber = Number(seasonId);
  const hasValidSeasonId = Number.isFinite(seasonIdNumber) && seasonIdNumber > 0;
  const [submitMode, setSubmitMode] = useState<"stay" | "dashboard">("stay");

  const formSchema = useMemo(() => createIrrigationWaterAnalysisFormSchema(t), [t]);
  const form = useForm<IrrigationWaterAnalysisFormValues>({
    resolver: zodResolver(formSchema),
    defaultValues: defaultValues(),
  });

  const { data: seasonDetail, isLoading: isSeasonLoading } = useSeasonById(seasonIdNumber, {
    enabled: hasValidSeasonId,
  });

  const { data: analyses, isLoading: isListLoading } = useSeasonIrrigationWaterAnalyses(
    seasonIdNumber,
    undefined,
    { enabled: hasValidSeasonId }
  );

  const createMutation = useCreateIrrigationWaterAnalysis(seasonIdNumber, {
    onSuccess: () => {
      toast.success(t("seasonIrrigationWorkspace.toast.createSuccess"));
      form.reset({
        ...defaultValues(),
        sourceType: form.getValues("sourceType"),
      });
      if (submitMode === "dashboard") {
        navigate("/farmer/dashboard");
      }
    },
    onError: (error) => {
      toast.error(error.message || t("seasonIrrigationWorkspace.toast.createError"));
    },
  });

  const plotId = seasonDetail?.plotId ?? null;
  const isSeasonWriteLocked =
    seasonDetail?.status === "COMPLETED"
    || seasonDetail?.status === "CANCELLED"
    || seasonDetail?.status === "ARCHIVED";
  const seasonWriteLockReason = isSeasonWriteLocked
    ? t("seasonIrrigationWorkspace.alerts.writeLockedDescription")
    : undefined;
  const records = useMemo(() => analyses ?? [], [analyses]);

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
      toast.error(t("seasonIrrigationWorkspace.validation.plotRequired"));
      return;
    }

    createMutation.mutate({
      plotId,
      sampleDate: values.sampleDate,
      nitrateMgPerL: values.nitrateMgPerL,
      ammoniumMgPerL: values.ammoniumMgPerL,
      totalNmgPerL: values.totalNmgPerL,
      irrigationVolumeM3: values.irrigationVolumeM3,
      sourceType: values.sourceType,
      sourceDocument: values.sourceDocument?.trim() || undefined,
      labReference: values.labReference?.trim() || undefined,
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
            <Droplets className="w-5 h-5 text-primary" />
            {t("seasonIrrigationWorkspace.title")}
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          {isSeasonLoading ? (
            <div className="text-sm text-muted-foreground flex items-center gap-2">
              <Loader2 className="w-4 h-4 animate-spin" />
              {t("seasonIrrigationWorkspace.loadingSeason")}
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
              <div className="rounded-xl border border-border p-3 bg-card">
                <p className="text-xs text-muted-foreground mb-1">{t("seasonIrrigationWorkspace.meta.season")}</p>
                <p className="text-sm text-foreground">{seasonDetail?.seasonName ?? "-"}</p>
              </div>
              <div className="rounded-xl border border-border p-3 bg-card">
                <p className="text-xs text-muted-foreground mb-1">{t("seasonIrrigationWorkspace.meta.plot")}</p>
                <p className="text-sm text-foreground">{seasonDetail?.plotName ?? "-"}</p>
              </div>
              <div className="rounded-xl border border-border p-3 bg-card">
                <p className="text-xs text-muted-foreground mb-1">{t("seasonIrrigationWorkspace.meta.crop")}</p>
                <p className="text-sm text-foreground">{seasonDetail?.cropName ?? "-"}</p>
              </div>
            </div>
          )}

          {!plotId && !isSeasonLoading && (
            <Alert variant="destructive">
              <AlertCircle className="h-4 w-4" />
              <AlertTitle>{t("seasonIrrigationWorkspace.alerts.missingPlotTitle")}</AlertTitle>
              <AlertDescription>{t("seasonIrrigationWorkspace.alerts.missingPlotDescription")}</AlertDescription>
            </Alert>
          )}

          {isSeasonWriteLocked && (
            <Alert>
              <AlertCircle className="h-4 w-4" />
              <AlertTitle>{t("seasonIrrigationWorkspace.alerts.writeLockedTitle")}</AlertTitle>
              <AlertDescription>{seasonWriteLockReason}</AlertDescription>
            </Alert>
          )}

          <Form {...form}>
            <form onSubmit={onSubmit} className="space-y-4" data-testid="irrigation-analysis-form">
              <fieldset disabled={isSeasonWriteLocked}>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <FormField
                    control={form.control}
                    name="sampleDate"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>{t("seasonIrrigationWorkspace.form.sampleDate")} *</FormLabel>
                        <FormControl>
                          <Input
                            type="date"
                            data-testid="irrigation-sample-date-input"
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
                    name="sourceType"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>{t("seasonIrrigationWorkspace.form.sourceType")} *</FormLabel>
                        <Select value={field.value} onValueChange={field.onChange}>
                          <FormControl>
                            <SelectTrigger className={FORM_SELECT_TRIGGER_CLASS_NAME}>
                              <SelectValue placeholder={t("seasonIrrigationWorkspace.form.sourceTypePlaceholder")} />
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
                    name="nitrateMgPerL"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>{t("seasonIrrigationWorkspace.form.nitrate")}</FormLabel>
                        <FormControl>
                          <Input
                            type="number"
                            step="0.0001"
                            min="0"
                            data-testid="irrigation-nitrate-input"
                            value={field.value ?? ""}
                            onChange={(event) => field.onChange(toNumberOrUndefined(event.target.value))}
                            placeholder={t("seasonIrrigationWorkspace.form.nitratePlaceholder")}
                            className={FORM_INPUT_CLASS_NAME}
                          />
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />

                  <FormField
                    control={form.control}
                    name="ammoniumMgPerL"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>{t("seasonIrrigationWorkspace.form.ammonium")}</FormLabel>
                        <FormControl>
                          <Input
                            type="number"
                            step="0.0001"
                            min="0"
                            data-testid="irrigation-ammonium-input"
                            value={field.value ?? ""}
                            onChange={(event) => field.onChange(toNumberOrUndefined(event.target.value))}
                            placeholder={t("seasonIrrigationWorkspace.form.ammoniumPlaceholder")}
                            className={FORM_INPUT_CLASS_NAME}
                          />
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />

                  <FormField
                    control={form.control}
                    name="totalNmgPerL"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>{t("seasonIrrigationWorkspace.form.totalN")}</FormLabel>
                        <FormControl>
                          <Input
                            type="number"
                            step="0.0001"
                            min="0"
                            data-testid="irrigation-total-n-input"
                            value={field.value ?? ""}
                            onChange={(event) => field.onChange(toNumberOrUndefined(event.target.value))}
                            placeholder={t("seasonIrrigationWorkspace.form.totalNPlaceholder")}
                            className={FORM_INPUT_CLASS_NAME}
                          />
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />

                  <FormField
                    control={form.control}
                    name="irrigationVolumeM3"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>{t("seasonIrrigationWorkspace.form.volume")} *</FormLabel>
                        <FormControl>
                          <Input
                            type="number"
                            step="0.0001"
                            min="0"
                            data-testid="irrigation-volume-input"
                            value={Number.isFinite(field.value) ? field.value : ""}
                            onChange={(event) => {
                              const nextValue = Number.parseFloat(event.target.value);
                              field.onChange(Number.isNaN(nextValue) ? undefined : nextValue);
                            }}
                            placeholder={t("seasonIrrigationWorkspace.form.volumePlaceholder")}
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
                        <FormLabel>{t("seasonIrrigationWorkspace.form.sourceDocument")}</FormLabel>
                        <FormControl>
                          <Input
                            value={field.value ?? ""}
                            onChange={field.onChange}
                            placeholder={t("seasonIrrigationWorkspace.form.sourceDocumentPlaceholder")}
                            className={FORM_INPUT_CLASS_NAME}
                          />
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />

                  <FormField
                    control={form.control}
                    name="labReference"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>{t("seasonIrrigationWorkspace.form.labReference")}</FormLabel>
                        <FormControl>
                          <Input
                            value={field.value ?? ""}
                            onChange={field.onChange}
                            placeholder={t("seasonIrrigationWorkspace.form.labReferencePlaceholder")}
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
                      <FormLabel>{t("seasonIrrigationWorkspace.form.note")}</FormLabel>
                      <FormControl>
                        <Textarea
                          rows={3}
                          value={field.value ?? ""}
                          onChange={field.onChange}
                          placeholder={t("seasonIrrigationWorkspace.form.notePlaceholder")}
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
                    data-testid="submit-irrigation-analysis"
                    disabled={createMutation.isPending || !plotId || isSeasonWriteLocked}
                    onClick={() => setSubmitMode("stay")}
                  >
                    {createMutation.isPending && submitMode === "stay" ? (
                      <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                    ) : null}
                    {t("seasonIrrigationWorkspace.actions.save")}
                  </Button>
                  <Button
                    type="submit"
                    data-testid="submit-irrigation-analysis-dashboard"
                    variant="outline"
                    disabled={createMutation.isPending || !plotId || isSeasonWriteLocked}
                    onClick={() => setSubmitMode("dashboard")}
                  >
                    {createMutation.isPending && submitMode === "dashboard" ? (
                      <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                    ) : null}
                    {t("seasonIrrigationWorkspace.actions.saveAndOpenDashboard")}
                  </Button>
                </div>
              </fieldset>
            </form>
          </Form>
        </CardContent>
      </Card>

      <Card className="border border-border rounded-2xl shadow-sm">
        <CardHeader>
          <CardTitle className="text-base">{t("seasonIrrigationWorkspace.list.title")}</CardTitle>
        </CardHeader>
        <CardContent>
          {isListLoading ? (
            <div className="text-sm text-muted-foreground flex items-center gap-2">
              <Loader2 className="w-4 h-4 animate-spin" />
              {t("seasonIrrigationWorkspace.list.loading")}
            </div>
          ) : records.length === 0 ? (
            <p className="text-sm text-muted-foreground">{t("seasonIrrigationWorkspace.list.empty")}</p>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-border text-muted-foreground">
                    <th className="text-left font-medium py-2 pr-3">{t("seasonIrrigationWorkspace.list.headers.date")}</th>
                    <th className="text-left font-medium py-2 pr-3">{t("seasonIrrigationWorkspace.list.headers.effectiveN")}</th>
                    <th className="text-left font-medium py-2 pr-3">{t("seasonIrrigationWorkspace.list.headers.volume")}</th>
                    <th className="text-left font-medium py-2 pr-3">{t("seasonIrrigationWorkspace.list.headers.nContribution")}</th>
                    <th className="text-left font-medium py-2 pr-3">{t("seasonIrrigationWorkspace.list.headers.source")}</th>
                    <th className="text-left font-medium py-2 pr-3">{t("seasonIrrigationWorkspace.list.headers.status")}</th>
                  </tr>
                </thead>
                <tbody>
                  {records.map((item) => (
                    <tr key={item.id} className="border-b border-border/60">
                      <td className="py-2 pr-3">{formatDate(item.sampleDate, locale)}</td>
                      <td className="py-2 pr-3">{formatNumber(item.effectiveNmgPerL)}</td>
                      <td className="py-2 pr-3">{formatNumber(item.irrigationVolumeM3)}</td>
                      <td className="py-2 pr-3">{formatNumber(item.estimatedNContributionKg)}</td>
                      <td className="py-2 pr-3">{getSourceTypeLabel(item.sourceType)}</td>
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
