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
  "h-11 rounded-xl border border-input bg-input-background px-4 text-sm shadow-sm placeholder:text-muted-foreground hover:border-primary/50 focus:border-primary focus:ring-2 focus:ring-primary/20 focus-visible:border-primary focus-visible:ring-2 focus-visible:ring-primary/20";

const FORM_SELECT_TRIGGER_CLASS_NAME =
  "h-11 rounded-xl border border-input bg-input-background px-4 text-sm shadow-sm data-[placeholder]:text-muted-foreground hover:border-primary/50 focus:border-primary focus:ring-2 focus:ring-primary/20 focus-visible:border-primary focus-visible:ring-2 focus-visible:ring-primary/20";

const FORM_TEXTAREA_CLASS_NAME =
  "rounded-xl border border-input bg-input-background px-4 py-3 text-sm shadow-sm placeholder:text-muted-foreground hover:border-primary/50 focus:border-primary focus:ring-2 focus:ring-primary/20 focus-visible:border-primary focus-visible:ring-2 focus-visible:ring-primary/20";

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
    <div className="space-y-8 pb-10 max-w-5xl mx-auto">
      {/* Header & Meta Section */}
      <div className="space-y-4 border-b border-border/40 pb-6">
        <div className="flex items-center gap-3">
          <div className="p-2.5 rounded-2xl bg-primary/10 text-primary">
            <Beaker className="w-6 h-6" />
          </div>
          <h2 className="text-xl font-semibold tracking-tight">{t("seasonNutrientWorkspace.title")}</h2>
        </div>
        
        {isSeasonLoading ? (
          <div className="text-sm text-muted-foreground flex items-center gap-2 py-2">
            <Loader2 className="w-4 h-4 animate-spin" />
            {t("seasonNutrientWorkspace.loadingSeason")}
          </div>
        ) : (
          <div className="flex flex-wrap items-center gap-4 bg-muted/30 p-3 rounded-2xl border border-border/40">
            <div className="flex items-center gap-2 px-2">
              <span className="text-xs text-muted-foreground uppercase tracking-wider font-medium">{t("seasonNutrientWorkspace.meta.season")}</span>
              <span className="text-sm font-medium">{seasonDetail?.seasonName ?? "-"}</span>
            </div>
            <div className="w-px h-4 bg-border/60 hidden sm:block"></div>
            <div className="flex items-center gap-2 px-2">
              <span className="text-xs text-muted-foreground uppercase tracking-wider font-medium">{t("seasonNutrientWorkspace.meta.plot")}</span>
              <span className="text-sm font-medium">{seasonDetail?.plotName ?? "-"}</span>
            </div>
            <div className="w-px h-4 bg-border/60 hidden sm:block"></div>
            <div className="flex items-center gap-2 px-2">
              <span className="text-xs text-muted-foreground uppercase tracking-wider font-medium">{t("seasonNutrientWorkspace.meta.crop")}</span>
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
              <AlertTitle>{t("seasonNutrientWorkspace.alerts.missingPlotTitle")}</AlertTitle>
              <AlertDescription>{t("seasonNutrientWorkspace.alerts.missingPlotDescription")}</AlertDescription>
            </Alert>
          )}

          {isSeasonWriteLocked && (
            <Alert className="rounded-2xl border-amber-300 bg-amber-50">
              <AlertCircle className="h-4 w-4 text-amber-600" />
              <AlertTitle className="text-amber-800">{t("seasonNutrientWorkspace.alerts.writeLockedTitle")}</AlertTitle>
              <AlertDescription className="text-amber-700">{seasonWriteLockReason}</AlertDescription>
            </Alert>
          )}

          <Form {...form}>
            <form onSubmit={onSubmit} className="space-y-8" data-testid="nutrient-input-form">
              <fieldset disabled={isSeasonWriteLocked} className="space-y-8">
                
                {/* Input Details Block */}
                <div className="p-6 rounded-3xl bg-card border border-border/50 shadow-sm space-y-6">
                  <h3 className="text-sm font-medium uppercase tracking-wider text-muted-foreground border-b border-border/40 pb-2">
                    {t("seasonNutrientWorkspace.sections.inputDetails", "Chi tiết dinh dưỡng")}
                  </h3>
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                    <FormField
                      control={form.control}
                      name="inputSource"
                      render={({ field }) => (
                        <FormItem className="md:col-span-2">
                          <FormLabel>{t("seasonNutrientWorkspace.form.inputSource")} *</FormLabel>
                          <Select value={field.value} onValueChange={field.onChange}>
                            <FormControl>
                              <SelectTrigger className={FORM_SELECT_TRIGGER_CLASS_NAME}>
                                <SelectValue placeholder={t("seasonNutrientWorkspace.form.inputSourcePlaceholder")} />
                              </SelectTrigger>
                            </FormControl>
                            <SelectContent>
                              {INPUT_SOURCE_OPTIONS.map((item) => (
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
                  </div>
                </div>

                {/* Record Info Block */}
                <div className="p-6 rounded-3xl bg-card border border-border/50 shadow-sm space-y-6">
                  <h3 className="text-sm font-medium uppercase tracking-wider text-muted-foreground border-b border-border/40 pb-2">
                    {t("seasonNutrientWorkspace.sections.recordInfo", "Thông tin ghi nhận")}
                  </h3>
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
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
                        <FormItem className="md:col-span-2">
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

                    <FormField
                      control={form.control}
                      name="note"
                      render={({ field }) => (
                        <FormItem className="md:col-span-2">
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
                  </div>
                </div>

                <div className="flex flex-wrap items-center justify-end gap-3 pt-2">
                  <Button
                    type="submit"
                    variant="outline"
                    data-testid="submit-nutrient-input-dashboard"
                    disabled={createMutation.isPending || !plotId || isSeasonWriteLocked}
                    onClick={() => setSubmitMode("dashboard")}
                    className="h-12 rounded-xl px-6"
                  >
                    {createMutation.isPending && submitMode === "dashboard" ? (
                      <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                    ) : null}
                    {t("seasonNutrientWorkspace.actions.saveAndOpenDashboard")}
                  </Button>
                  <Button
                    type="submit"
                    data-testid="submit-nutrient-input"
                    disabled={createMutation.isPending || !plotId || isSeasonWriteLocked}
                    onClick={() => setSubmitMode("stay")}
                    className="h-12 rounded-xl px-8"
                  >
                    {createMutation.isPending && submitMode === "stay" ? (
                      <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                    ) : null}
                    {t("seasonNutrientWorkspace.actions.save")}
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
          <h3 className="text-lg font-semibold tracking-tight">{t("seasonNutrientWorkspace.list.title")}</h3>
          <p className="text-sm text-muted-foreground mt-1">Lịch sử ghi nhận dinh dưỡng cho mùa vụ này.</p>
        </div>
        
        <div className="rounded-2xl border border-border/50 bg-card shadow-sm overflow-hidden">
          {isInputsLoading ? (
            <div className="p-12 text-center flex flex-col items-center justify-center space-y-3">
              <div className="w-12 h-12 rounded-full bg-muted/50 flex items-center justify-center">
                <Loader2 className="w-6 h-6 animate-spin text-muted-foreground/50" />
              </div>
              <p className="text-sm text-muted-foreground">{t("seasonNutrientWorkspace.list.loading")}</p>
            </div>
          ) : createdRecords.length === 0 ? (
            <div className="p-12 text-center flex flex-col items-center justify-center space-y-3">
              <div className="w-12 h-12 rounded-full bg-muted/50 flex items-center justify-center">
                <Beaker className="w-6 h-6 text-muted-foreground/50" />
              </div>
              <p className="text-sm text-muted-foreground">{t("seasonNutrientWorkspace.list.empty")}</p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead className="bg-muted/30">
                  <tr className="border-b border-border/50 text-muted-foreground">
                    <th className="text-left font-medium py-3 px-4">{t("seasonNutrientWorkspace.list.headers.date")}</th>
                    <th className="text-left font-medium py-3 px-4">{t("seasonNutrientWorkspace.list.headers.inputType")}</th>
                    <th className="text-left font-medium py-3 px-4">{t("seasonNutrientWorkspace.list.headers.value")}</th>
                    <th className="text-left font-medium py-3 px-4">{t("seasonNutrientWorkspace.list.headers.source")}</th>
                    <th className="text-left font-medium py-3 px-4">{t("seasonNutrientWorkspace.list.headers.status")}</th>
                  </tr>
                </thead>
                <tbody>
                  {createdRecords.map((item) => (
                    <tr key={item.id} className="border-b border-border/40 hover:bg-muted/20 transition-colors">
                      <td className="py-3 px-4 text-muted-foreground">{formatDate(item.recordedAt, locale)}</td>
                      <td className="py-3 px-4 font-medium text-foreground">
                        {getInputSourceLabel(item.inputSource)}
                      </td>
                      <td className="py-3 px-4 font-semibold text-primary">
                        {item.value} <span className="text-muted-foreground font-normal text-xs ml-1">{item.unit}</span>
                      </td>
                      <td className="py-3 px-4 text-muted-foreground">
                        {getSourceTypeLabel(item.sourceType)}
                      </td>
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
