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
  "h-11 rounded-xl border border-input bg-input-background px-4 text-sm shadow-sm placeholder:text-muted-foreground hover:border-primary/50 focus:border-primary focus:ring-2 focus:ring-primary/20 focus-visible:border-primary focus-visible:ring-2 focus-visible:ring-primary/20";

const FORM_SELECT_TRIGGER_CLASS_NAME =
  "h-11 rounded-xl border border-input bg-input-background px-4 text-sm shadow-sm data-[placeholder]:text-muted-foreground hover:border-primary/50 focus:border-primary focus:ring-2 focus:ring-primary/20 focus-visible:border-primary focus-visible:ring-2 focus-visible:ring-primary/20";

const FORM_TEXTAREA_CLASS_NAME =
  "rounded-xl border border-input bg-input-background px-4 py-3 text-sm shadow-sm placeholder:text-muted-foreground hover:border-primary/50 focus:border-primary focus:ring-2 focus:ring-primary/20 focus-visible:border-primary focus-visible:ring-2 focus-visible:ring-primary/20";

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
    <div className="space-y-8 pb-10 max-w-5xl mx-auto">
      {/* Header & Meta Section */}
      <div className="space-y-4 border-b border-border/40 pb-6">
        <div className="flex items-center gap-3">
          <div className="p-2.5 rounded-2xl bg-primary/10 text-primary">
            <Droplets className="w-6 h-6" />
          </div>
          <h2 className="text-xl font-semibold tracking-tight">{t("seasonIrrigationWorkspace.title")}</h2>
        </div>
        
        {isSeasonLoading ? (
          <div className="text-sm text-muted-foreground flex items-center gap-2 py-2">
            <Loader2 className="w-4 h-4 animate-spin" />
            {t("seasonIrrigationWorkspace.loadingSeason")}
          </div>
        ) : (
          <div className="flex flex-wrap items-center gap-4 bg-muted/30 p-3 rounded-2xl border border-border/40">
            <div className="flex items-center gap-2 px-2">
              <span className="text-xs text-muted-foreground uppercase tracking-wider font-medium">{t("seasonIrrigationWorkspace.meta.season")}</span>
              <span className="text-sm font-medium">{seasonDetail?.seasonName ?? "-"}</span>
            </div>
            <div className="w-px h-4 bg-border/60 hidden sm:block"></div>
            <div className="flex items-center gap-2 px-2">
              <span className="text-xs text-muted-foreground uppercase tracking-wider font-medium">{t("seasonIrrigationWorkspace.meta.plot")}</span>
              <span className="text-sm font-medium">{seasonDetail?.plotName ?? "-"}</span>
            </div>
            <div className="w-px h-4 bg-border/60 hidden sm:block"></div>
            <div className="flex items-center gap-2 px-2">
              <span className="text-xs text-muted-foreground uppercase tracking-wider font-medium">{t("seasonIrrigationWorkspace.meta.crop")}</span>
              <Badge variant="outline" className="text-xs font-normal shadow-none border-border/60 bg-card">
                {seasonDetail?.cropName ?? "-"}
              </Badge>
            </div>
          </div>
        )}
      </div>

      {/* Main Form Section */}
      <section className="space-y-6">
        <div className="space-y-4">
          {!plotId && !isSeasonLoading && (
            <Alert variant="destructive" className="border-destructive/20 bg-destructive/5 rounded-2xl">
              <AlertCircle className="h-4 w-4" />
              <AlertTitle>{t("seasonIrrigationWorkspace.alerts.missingPlotTitle")}</AlertTitle>
              <AlertDescription>{t("seasonIrrigationWorkspace.alerts.missingPlotDescription")}</AlertDescription>
            </Alert>
          )}

          {isSeasonWriteLocked && (
            <Alert className="rounded-2xl border-amber-300 bg-amber-50">
              <AlertCircle className="h-4 w-4 text-amber-600" />
              <AlertTitle className="text-amber-800">{t("seasonIrrigationWorkspace.alerts.writeLockedTitle")}</AlertTitle>
              <AlertDescription className="text-amber-700">{seasonWriteLockReason}</AlertDescription>
            </Alert>
          )}

          <Form {...form}>
            <form onSubmit={onSubmit} className="space-y-8" data-testid="irrigation-analysis-form">
              <fieldset disabled={isSeasonWriteLocked} className="space-y-8">
                
                {/* Sample Info Block */}
                <div className="p-6 rounded-3xl bg-card border border-border/50 shadow-sm space-y-6">
                  <h3 className="text-sm font-medium uppercase tracking-wider text-muted-foreground border-b border-border/40 pb-2">
                    {t("seasonIrrigationWorkspace.sections.sampleInfo", "Thông tin lấy mẫu & Lượng tưới")}
                  </h3>
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
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
                  </div>
                </div>

                {/* Nitrogen Metrics Block */}
                <div className="p-6 rounded-3xl bg-card border border-border/50 shadow-sm space-y-6">
                  <div className="space-y-1 border-b border-border/40 pb-2">
                    <h3 className="text-sm font-medium uppercase tracking-wider text-muted-foreground">
                      {t("seasonIrrigationWorkspace.sections.nitrogenMetrics", "Chỉ số Nitrogen (mg/L)")}
                    </h3>
                    <p className="text-xs text-muted-foreground">Nhập ít nhất một trong các chỉ số sau để hệ thống tính toán lượng đạm đóng góp.</p>
                  </div>
                  <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
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
                  </div>
                </div>

                {/* References Block */}
                <div className="p-6 rounded-3xl bg-card border border-border/50 shadow-sm space-y-6">
                  <h3 className="text-sm font-medium uppercase tracking-wider text-muted-foreground border-b border-border/40 pb-2">
                    {t("seasonIrrigationWorkspace.sections.references", "Nguồn dữ liệu & Ghi chú")}
                  </h3>
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                    <FormField
                      control={form.control}
                      name="sourceType"
                      render={({ field }) => (
                        <FormItem className="md:col-span-2">
                          <FormLabel>{t("seasonIrrigationWorkspace.form.sourceType")} *</FormLabel>
                          <Select value={field.value} onValueChange={field.onChange}>
                            <FormControl>
                              <SelectTrigger className={FORM_SELECT_TRIGGER_CLASS_NAME}>
                                <SelectValue placeholder={t("seasonIrrigationWorkspace.form.sourceTypePlaceholder")} />
                              </SelectTrigger>
                            </FormControl>
                            <SelectContent>
                              {SOURCE_OPTIONS.map((item) => (
                                <SelectItem key={item.value} value={item.value} className="rounded-lg cursor-pointer">
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

                    <FormField
                      control={form.control}
                      name="note"
                      render={({ field }) => (
                        <FormItem className="md:col-span-2">
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
                  </div>
                </div>

                <div className="flex flex-wrap items-center justify-end gap-3 pt-2">
                  <Button
                    type="submit"
                    variant="outline"
                    data-testid="submit-irrigation-analysis-dashboard"
                    disabled={createMutation.isPending || !plotId || isSeasonWriteLocked}
                    onClick={() => setSubmitMode("dashboard")}
                    className="h-12 rounded-xl px-6"
                  >
                    {createMutation.isPending && submitMode === "dashboard" ? (
                      <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                    ) : null}
                    {t("seasonIrrigationWorkspace.actions.saveAndOpenDashboard")}
                  </Button>
                  <Button
                    type="submit"
                    data-testid="submit-irrigation-analysis"
                    disabled={createMutation.isPending || !plotId || isSeasonWriteLocked}
                    onClick={() => setSubmitMode("stay")}
                    className="h-12 rounded-xl px-8"
                  >
                    {createMutation.isPending && submitMode === "stay" ? (
                      <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                    ) : null}
                    {t("seasonIrrigationWorkspace.actions.save")}
                  </Button>
                </div>
              </fieldset>
            </form>
          </Form>
        </div>
      </section>

      {/* List Section */}
      <section className="space-y-4 pt-6">
        <div>
          <h3 className="text-lg font-semibold tracking-tight">{t("seasonIrrigationWorkspace.list.title")}</h3>
          <p className="text-sm text-muted-foreground mt-1">Lịch sử phân tích nước & lượng tưới cho mùa vụ này.</p>
        </div>
        
        <div className="rounded-2xl border border-border/50 bg-card shadow-sm overflow-hidden">
          {isListLoading ? (
            <div className="p-12 text-center flex flex-col items-center justify-center space-y-3">
              <div className="w-12 h-12 rounded-full bg-muted/50 flex items-center justify-center">
                <Loader2 className="w-6 h-6 animate-spin text-muted-foreground/50" />
              </div>
              <p className="text-sm text-muted-foreground">{t("seasonIrrigationWorkspace.list.loading")}</p>
            </div>
          ) : records.length === 0 ? (
            <div className="p-12 text-center flex flex-col items-center justify-center space-y-3">
              <div className="w-12 h-12 rounded-full bg-muted/50 flex items-center justify-center">
                <Droplets className="w-6 h-6 text-muted-foreground/50" />
              </div>
              <p className="text-sm text-muted-foreground">{t("seasonIrrigationWorkspace.list.empty")}</p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead className="bg-muted/30">
                  <tr className="border-b border-border/50 text-muted-foreground">
                    <th className="text-left font-medium py-3 px-4">{t("seasonIrrigationWorkspace.list.headers.date")}</th>
                    <th className="text-left font-medium py-3 px-4">{t("seasonIrrigationWorkspace.list.headers.effectiveN")} <span className="text-xs font-normal text-muted-foreground">(mg/L)</span></th>
                    <th className="text-left font-medium py-3 px-4">{t("seasonIrrigationWorkspace.list.headers.volume")} <span className="text-xs font-normal text-muted-foreground">(m³)</span></th>
                    <th className="text-left font-medium py-3 px-4">{t("seasonIrrigationWorkspace.list.headers.nContribution")} <span className="text-xs font-normal text-muted-foreground">(kg)</span></th>
                    <th className="text-left font-medium py-3 px-4">{t("seasonIrrigationWorkspace.list.headers.source")}</th>
                    <th className="text-left font-medium py-3 px-4">{t("seasonIrrigationWorkspace.list.headers.status")}</th>
                  </tr>
                </thead>
                <tbody>
                  {records.map((item) => (
                    <tr key={item.id} className="border-b border-border/40 hover:bg-muted/20 transition-colors">
                      <td className="py-3 px-4 text-muted-foreground">{formatDate(item.sampleDate, locale)}</td>
                      <td className="py-3 px-4 font-semibold text-primary">{formatNumber(item.effectiveNmgPerL)}</td>
                      <td className="py-3 px-4 font-medium">{formatNumber(item.irrigationVolumeM3)}</td>
                      <td className="py-3 px-4 font-semibold text-primary/80">{formatNumber(item.estimatedNContributionKg)}</td>
                      <td className="py-3 px-4 text-muted-foreground">{getSourceTypeLabel(item.sourceType)}</td>
                      <td className="py-3 px-4">
                        <Badge className={`${metricStatusClassName(item.status)} font-medium shadow-none`}>{item.status ?? "unavailable"}</Badge>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </section>
    </div>
  );
}
