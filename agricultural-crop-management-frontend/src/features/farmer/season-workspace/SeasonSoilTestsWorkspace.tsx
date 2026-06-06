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
  useCreateSoilTest,
  useSeasonSoilTests,
  type SoilTestSourceType,
} from "@/entities/soil-test";
import { metricStatusClassName } from "@/features/farmer/dashboard/lib/metrics";
import { useI18n } from "@/shared/lib/hooks/useI18n";
import { AlertCircle, Loader2, TestTubeDiagonal } from "lucide-react";
import { toast } from "sonner";

type Translator = (key: string, optionsOrDefault?: Record<string, unknown> | string) => string;

const createSoilTestFormSchema = (t: Translator) =>
  z.object({
    sampleDate: z.string().min(1, t("seasonSoilWorkspace.validation.sampleDateRequired")),
    soilOrganicMatterPct: z.number().min(0).max(100).optional(),
    mineralNKgPerHa: z
      .number({ required_error: t("seasonSoilWorkspace.validation.mineralNRequired") })
      .min(0, t("seasonSoilWorkspace.validation.nonNegative")),
    nitrateMgPerKg: z.number().min(0).optional(),
    ammoniumMgPerKg: z.number().min(0).optional(),
    sourceType: z.enum(["user_entered", "lab_measured", "external_reference"]),
    sourceDocument: z.string().trim().max(255).optional(),
    labReference: z.string().trim().max(255).optional(),
    note: z.string().trim().max(4000).optional(),
  });

type SoilTestFormSchemaType = ReturnType<typeof createSoilTestFormSchema>;
type SoilTestFormValues = z.infer<SoilTestFormSchemaType>;

const SOURCE_OPTIONS: { value: SoilTestSourceType; labelKey: string; fallbackLabel: string }[] = [
  {
    value: "user_entered",
    labelKey: "seasonSoilWorkspace.form.sourceTypeOptions.userEntered",
    fallbackLabel: "User entered",
  },
  {
    value: "lab_measured",
    labelKey: "seasonSoilWorkspace.form.sourceTypeOptions.labMeasured",
    fallbackLabel: "Lab measured",
  },
  {
    value: "external_reference",
    labelKey: "seasonSoilWorkspace.form.sourceTypeOptions.externalReference",
    fallbackLabel: "External reference",
  },
];

const FORM_INPUT_CLASS_NAME =
  "h-11 rounded-xl border border-slate-300 bg-white px-4 text-sm shadow-sm placeholder:text-slate-400 hover:border-slate-400 focus:border-[#3BA55D] focus:ring-2 focus:ring-[#3BA55D]/20 focus-visible:border-[#3BA55D] focus-visible:ring-2 focus-visible:ring-[#3BA55D]/20";

const FORM_SELECT_TRIGGER_CLASS_NAME =
  "h-11 rounded-xl border border-slate-300 bg-white px-4 text-sm shadow-sm data-[placeholder]:text-slate-400 hover:border-slate-400 focus:border-[#3BA55D] focus:ring-2 focus:ring-[#3BA55D]/20 focus-visible:border-[#3BA55D] focus-visible:ring-2 focus-visible:ring-[#3BA55D]/20";

const FORM_TEXTAREA_CLASS_NAME =
  "rounded-xl border border-slate-300 bg-white px-4 py-3 text-sm shadow-sm placeholder:text-slate-400 hover:border-slate-400 focus:border-[#3BA55D] focus:ring-2 focus:ring-[#3BA55D]/20 focus-visible:border-[#3BA55D] focus-visible:ring-2 focus-visible:ring-[#3BA55D]/20";

const defaultValues = (): SoilTestFormValues => ({
  sampleDate: new Date().toISOString().split("T")[0],
  soilOrganicMatterPct: undefined,
  mineralNKgPerHa: 0,
  nitrateMgPerKg: undefined,
  ammoniumMgPerKg: undefined,
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

const toNumberOrUndefined = (raw: string): number | undefined => {
  const parsed = Number.parseFloat(raw);
  return Number.isNaN(parsed) ? undefined : parsed;
};

export function SeasonSoilTestsWorkspace() {
  const { seasonId } = useParams();
  const navigate = useNavigate();
  const { t, locale } = useI18n();
  const seasonIdNumber = Number(seasonId);
  const hasValidSeasonId = Number.isFinite(seasonIdNumber) && seasonIdNumber > 0;
  const [submitMode, setSubmitMode] = useState<"stay" | "dashboard">("stay");

  const formSchema = useMemo(() => createSoilTestFormSchema(t), [t]);
  const form = useForm<SoilTestFormValues>({
    resolver: zodResolver(formSchema),
    defaultValues: defaultValues(),
  });

  const { data: seasonDetail, isLoading: isSeasonLoading } = useSeasonById(seasonIdNumber, {
    enabled: hasValidSeasonId,
  });

  const { data: soilTests, isLoading: isListLoading } = useSeasonSoilTests(seasonIdNumber, undefined, {
    enabled: hasValidSeasonId,
  });

  const createMutation = useCreateSoilTest(seasonIdNumber, {
    onSuccess: () => {
      toast.success(t("seasonSoilWorkspace.toast.createSuccess"));
      form.reset({
        ...defaultValues(),
        sourceType: form.getValues("sourceType"),
      });
      if (submitMode === "dashboard") {
        navigate("/farmer/dashboard");
      }
    },
    onError: (error) => {
      toast.error(error.message || t("seasonSoilWorkspace.toast.createError"));
    },
  });

  const plotId = seasonDetail?.plotId ?? null;
  const isSeasonWriteLocked =
    seasonDetail?.status === "COMPLETED"
    || seasonDetail?.status === "CANCELLED"
    || seasonDetail?.status === "ARCHIVED";
  const seasonWriteLockReason = isSeasonWriteLocked
    ? t("seasonSoilWorkspace.alerts.writeLockedDescription")
    : undefined;
  const records = useMemo(() => soilTests ?? [], [soilTests]);

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
      toast.error(t("seasonSoilWorkspace.validation.plotRequired"));
      return;
    }

    createMutation.mutate({
      plotId,
      sampleDate: values.sampleDate,
      soilOrganicMatterPct: values.soilOrganicMatterPct,
      mineralNKgPerHa: values.mineralNKgPerHa,
      nitrateMgPerKg: values.nitrateMgPerKg,
      ammoniumMgPerKg: values.ammoniumMgPerKg,
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
            <TestTubeDiagonal className="w-5 h-5 text-primary" />
            {t("seasonSoilWorkspace.title")}
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          {isSeasonLoading ? (
            <div className="text-sm text-muted-foreground flex items-center gap-2">
              <Loader2 className="w-4 h-4 animate-spin" />
              {t("seasonSoilWorkspace.loadingSeason")}
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
              <div className="rounded-xl border border-border p-3 bg-card">
                <p className="text-xs text-muted-foreground mb-1">{t("seasonSoilWorkspace.meta.season")}</p>
                <p className="text-sm text-foreground">{seasonDetail?.seasonName ?? "-"}</p>
              </div>
              <div className="rounded-xl border border-border p-3 bg-card">
                <p className="text-xs text-muted-foreground mb-1">{t("seasonSoilWorkspace.meta.plot")}</p>
                <p className="text-sm text-foreground">{seasonDetail?.plotName ?? "-"}</p>
              </div>
              <div className="rounded-xl border border-border p-3 bg-card">
                <p className="text-xs text-muted-foreground mb-1">{t("seasonSoilWorkspace.meta.crop")}</p>
                <p className="text-sm text-foreground">{seasonDetail?.cropName ?? "-"}</p>
              </div>
            </div>
          )}

          {!plotId && !isSeasonLoading && (
            <Alert variant="destructive">
              <AlertCircle className="h-4 w-4" />
              <AlertTitle>{t("seasonSoilWorkspace.alerts.missingPlotTitle")}</AlertTitle>
              <AlertDescription>{t("seasonSoilWorkspace.alerts.missingPlotDescription")}</AlertDescription>
            </Alert>
          )}

          {isSeasonWriteLocked && (
            <Alert>
              <AlertCircle className="h-4 w-4" />
              <AlertTitle>{t("seasonSoilWorkspace.alerts.writeLockedTitle")}</AlertTitle>
              <AlertDescription>{seasonWriteLockReason}</AlertDescription>
            </Alert>
          )}

          <Form {...form}>
            <form onSubmit={onSubmit} className="space-y-4" data-testid="soil-test-form">
              <fieldset disabled={isSeasonWriteLocked}>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <FormField
                    control={form.control}
                    name="sampleDate"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>{t("seasonSoilWorkspace.form.sampleDate")} *</FormLabel>
                        <FormControl>
                          <Input
                            type="date"
                            data-testid="soil-sample-date-input"
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
                        <FormLabel>{t("seasonSoilWorkspace.form.sourceType")} *</FormLabel>
                        <Select value={field.value} onValueChange={field.onChange}>
                          <FormControl>
                            <SelectTrigger className={FORM_SELECT_TRIGGER_CLASS_NAME}>
                              <SelectValue placeholder={t("seasonSoilWorkspace.form.sourceTypePlaceholder")} />
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
                    name="mineralNKgPerHa"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>{t("seasonSoilWorkspace.form.mineralN")} *</FormLabel>
                        <FormControl>
                          <Input
                            type="number"
                            step="0.0001"
                            min="0"
                            data-testid="soil-mineral-n-input"
                            value={Number.isFinite(field.value) ? field.value : ""}
                            onChange={(event) => {
                              const nextValue = Number.parseFloat(event.target.value);
                              field.onChange(Number.isNaN(nextValue) ? undefined : nextValue);
                            }}
                            placeholder={t("seasonSoilWorkspace.form.mineralNPlaceholder")}
                            className={FORM_INPUT_CLASS_NAME}
                          />
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />

                  <FormField
                    control={form.control}
                    name="soilOrganicMatterPct"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>{t("seasonSoilWorkspace.form.soilOrganicMatter")}</FormLabel>
                        <FormControl>
                          <Input
                            type="number"
                            step="0.0001"
                            min="0"
                            max="100"
                            data-testid="soil-som-input"
                            value={field.value ?? ""}
                            onChange={(event) => field.onChange(toNumberOrUndefined(event.target.value))}
                            placeholder={t("seasonSoilWorkspace.form.soilOrganicMatterPlaceholder")}
                            className={FORM_INPUT_CLASS_NAME}
                          />
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />

                  <FormField
                    control={form.control}
                    name="nitrateMgPerKg"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>{t("seasonSoilWorkspace.form.nitrate")}</FormLabel>
                        <FormControl>
                          <Input
                            type="number"
                            step="0.0001"
                            min="0"
                            value={field.value ?? ""}
                            onChange={(event) => field.onChange(toNumberOrUndefined(event.target.value))}
                            placeholder={t("seasonSoilWorkspace.form.nitratePlaceholder")}
                            className={FORM_INPUT_CLASS_NAME}
                          />
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />

                  <FormField
                    control={form.control}
                    name="ammoniumMgPerKg"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>{t("seasonSoilWorkspace.form.ammonium")}</FormLabel>
                        <FormControl>
                          <Input
                            type="number"
                            step="0.0001"
                            min="0"
                            value={field.value ?? ""}
                            onChange={(event) => field.onChange(toNumberOrUndefined(event.target.value))}
                            placeholder={t("seasonSoilWorkspace.form.ammoniumPlaceholder")}
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
                        <FormLabel>{t("seasonSoilWorkspace.form.sourceDocument")}</FormLabel>
                        <FormControl>
                          <Input
                            value={field.value ?? ""}
                            onChange={field.onChange}
                            placeholder={t("seasonSoilWorkspace.form.sourceDocumentPlaceholder")}
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
                        <FormLabel>{t("seasonSoilWorkspace.form.labReference")}</FormLabel>
                        <FormControl>
                          <Input
                            value={field.value ?? ""}
                            onChange={field.onChange}
                            placeholder={t("seasonSoilWorkspace.form.labReferencePlaceholder")}
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
                      <FormLabel>{t("seasonSoilWorkspace.form.note")}</FormLabel>
                      <FormControl>
                        <Textarea
                          rows={3}
                          value={field.value ?? ""}
                          onChange={field.onChange}
                          placeholder={t("seasonSoilWorkspace.form.notePlaceholder")}
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
                    data-testid="submit-soil-test"
                    disabled={createMutation.isPending || !plotId || isSeasonWriteLocked}
                    onClick={() => setSubmitMode("stay")}
                  >
                    {createMutation.isPending && submitMode === "stay" ? (
                      <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                    ) : null}
                    {t("seasonSoilWorkspace.actions.save")}
                  </Button>
                  <Button
                    type="submit"
                    data-testid="submit-soil-test-dashboard"
                    variant="outline"
                    disabled={createMutation.isPending || !plotId || isSeasonWriteLocked}
                    onClick={() => setSubmitMode("dashboard")}
                  >
                    {createMutation.isPending && submitMode === "dashboard" ? (
                      <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                    ) : null}
                    {t("seasonSoilWorkspace.actions.saveAndOpenDashboard")}
                  </Button>
                </div>
              </fieldset>
            </form>
          </Form>
        </CardContent>
      </Card>

      <Card className="border border-border rounded-2xl shadow-sm">
        <CardHeader>
          <CardTitle className="text-base">{t("seasonSoilWorkspace.list.title")}</CardTitle>
        </CardHeader>
        <CardContent>
          {isListLoading ? (
            <div className="text-sm text-muted-foreground flex items-center gap-2">
              <Loader2 className="w-4 h-4 animate-spin" />
              {t("seasonSoilWorkspace.list.loading")}
            </div>
          ) : records.length === 0 ? (
            <p className="text-sm text-muted-foreground">{t("seasonSoilWorkspace.list.empty")}</p>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-border text-muted-foreground">
                    <th className="text-left font-medium py-2 pr-3">{t("seasonSoilWorkspace.list.headers.date")}</th>
                    <th className="text-left font-medium py-2 pr-3">{t("seasonSoilWorkspace.list.headers.mineralN")}</th>
                    <th className="text-left font-medium py-2 pr-3">{t("seasonSoilWorkspace.list.headers.soilOrganicMatter")}</th>
                    <th className="text-left font-medium py-2 pr-3">{t("seasonSoilWorkspace.list.headers.nContribution")}</th>
                    <th className="text-left font-medium py-2 pr-3">{t("seasonSoilWorkspace.list.headers.source")}</th>
                    <th className="text-left font-medium py-2 pr-3">{t("seasonSoilWorkspace.list.headers.status")}</th>
                  </tr>
                </thead>
                <tbody>
                  {records.map((item) => (
                    <tr key={item.id} className="border-b border-border/60">
                      <td className="py-2 pr-3">{formatDate(item.sampleDate, locale)}</td>
                      <td className="py-2 pr-3">{formatNumber(item.mineralNKgPerHa)}</td>
                      <td className="py-2 pr-3">{formatNumber(item.soilOrganicMatterPct)}</td>
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
