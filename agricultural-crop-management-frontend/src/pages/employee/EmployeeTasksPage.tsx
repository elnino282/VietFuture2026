import {
  useEmployeeAcceptTask,
  useEmployeeReportTaskProgress,
  useEmployeeSeasonPlan,
  useEmployeeTasks,
} from "@/entities/labor";
import {
  BackButton,
  Badge,
  Button,
  Card,
  CardContent,
  CardHeader,
  CardTitle,
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  Input,
  Label,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
  Textarea,
} from "@/shared/ui";
import { useI18n } from "@/hooks/useI18n";
import { useMemo, useState } from "react";
import { toast } from "sonner";

const getTaskStatusClassName = (status?: string) => {
  switch (status) {
    case "DONE":
      return "bg-emerald-100 text-emerald-700 border-emerald-200";
    case "IN_PROGRESS":
      return "bg-blue-100 text-blue-700 border-blue-200";
    case "OVERDUE":
      return "bg-red-100 text-red-700 border-red-200";
    case "CANCELLED":
      return "bg-slate-100 text-slate-600 border-slate-200";
    default:
      return "bg-amber-100 text-amber-700 border-amber-200";
  }
};

export function EmployeeTasksPage() {
  const { t } = useI18n();
  const { data: taskPageData, isLoading } = useEmployeeTasks({ page: 0, size: 200 });
  const [progressTaskId, setProgressTaskId] = useState<number | null>(null);
  const [planSeasonId, setPlanSeasonId] = useState<number | null>(null);
  const [progressPercent, setProgressPercent] = useState<string>("0");
  const [progressNote, setProgressNote] = useState<string>("");
  const [evidenceUrl, setEvidenceUrl] = useState<string>("");

  const tasks = taskPageData?.items ?? [];
  const tasksBySeason = useMemo(() => {
    const grouped = new Map<
      string,
      { seasonId: number | null; seasonName: string; tasks: typeof tasks }
    >();
    tasks.forEach((task) => {
      const seasonId = task.seasonId ?? null;
      const key = seasonId ? String(seasonId) : "no-season";
      const seasonName = task.seasonName || t("employee.common.unassignedSeason");
      const existing = grouped.get(key);
      if (existing) {
        existing.tasks.push(task);
      } else {
        grouped.set(key, { seasonId, seasonName, tasks: [task] });
      }
    });
    return Array.from(grouped.values()).sort((a, b) =>
      a.seasonName.localeCompare(b.seasonName)
    );
  }, [tasks, t]);
  const selectedTask = useMemo(
    () => tasks.find((task) => task.taskId === progressTaskId) ?? null,
    [progressTaskId, tasks]
  );
  const selectedPlanSeasonName = useMemo(
    () =>
      tasksBySeason.find((group) => group.seasonId === planSeasonId)?.seasonName
      ?? (planSeasonId ? t("employee.common.seasonFallback", { id: planSeasonId }) : ""),
    [planSeasonId, tasksBySeason, t]
  );
  const { data: seasonPlanTasks, isLoading: isSeasonPlanLoading } = useEmployeeSeasonPlan(planSeasonId);

  const acceptTaskMutation = useEmployeeAcceptTask({
    onSuccess: () => toast.success(t("employee.tasks.toast.acceptSuccess")),
    onError: (error) => toast.error(error.message || t("employee.tasks.toast.acceptError")),
  });

  const reportProgressMutation = useEmployeeReportTaskProgress({
    onSuccess: () => {
      toast.success(t("employee.tasks.toast.progressSuccess"));
      setProgressTaskId(null);
      setProgressPercent("0");
      setProgressNote("");
      setEvidenceUrl("");
    },
    onError: (error) => toast.error(error.message || t("employee.tasks.toast.progressError")),
  });

  const getTaskStatusLabel = (status?: string) =>
    status ? t(`employee.tasks.status.${status}`, status) : t("common.notAvailable");

  const handleSubmitProgress = () => {
    if (!progressTaskId) return;
    const parsedPercent = Number(progressPercent);
    if (Number.isNaN(parsedPercent) || parsedPercent < 0 || parsedPercent > 100) {
      toast.error(t("employee.tasks.validation.progressRange"));
      return;
    }
    reportProgressMutation.mutate({
      taskId: progressTaskId,
      data: {
        progressPercent: parsedPercent,
        note: progressNote.trim() || undefined,
        evidenceUrl: evidenceUrl.trim() || undefined,
      },
    });
  };
  const closeProgressDialog = () => {
    const isDirty =
      progressPercent !== "0" ||
      progressNote.trim().length > 0 ||
      evidenceUrl.trim().length > 0;
    if (
      isDirty &&
      !window.confirm(t("common.unsavedChangesConfirm", "You have unsaved changes. Leave this page?"))
    ) {
      return;
    }
    setProgressTaskId(null);
    setProgressPercent("0");
    setProgressNote("");
    setEvidenceUrl("");
  };

  return (
    <div className="p-4 sm:p-6 max-w-[1500px] mx-auto space-y-4">
      <Card className="rounded-2xl border border-border">
        <CardHeader>
          <CardTitle>{t("employee.tasks.title")}</CardTitle>
        </CardHeader>
        <CardContent>
          {isLoading ? (
            <p className="text-sm text-muted-foreground">{t("employee.tasks.loading")}</p>
          ) : tasks.length === 0 ? (
            <p className="text-sm text-muted-foreground">{t("employee.tasks.empty")}</p>
          ) : (
            <div className="space-y-6">
              {tasksBySeason.map((group) => (
                <div key={`${group.seasonId ?? "no-season"}-${group.seasonName}`} className="space-y-2">
                  <div className="flex flex-col items-start gap-2 sm:flex-row sm:items-center sm:justify-between">
                    <h3 className="text-sm font-semibold text-foreground">{group.seasonName}</h3>
                    {group.seasonId && (
                      <Button
                        type="button"
                        size="sm"
                        variant="outline"
                        onClick={() => setPlanSeasonId(group.seasonId)}
                      >
                        {t("employee.tasks.actions.viewSeasonPlan")}
                      </Button>
                    )}
                  </div>

                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead>{t("employee.tasks.table.task")}</TableHead>
                        <TableHead>{t("employee.tasks.table.dueDate")}</TableHead>
                        <TableHead>{t("employee.tasks.table.status")}</TableHead>
                        <TableHead className="text-right">{t("employee.tasks.table.actions")}</TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {group.tasks.map((task) => (
                        <TableRow key={task.taskId}>
                          <TableCell>
                            <div className="space-y-1">
                              <p className="font-medium">{task.title}</p>
                              <p className="text-xs text-muted-foreground">{task.description || "-"}</p>
                            </div>
                          </TableCell>
                          <TableCell>{task.dueDate || "-"}</TableCell>
                          <TableCell>
                            <Badge className={getTaskStatusClassName(task.status)}>
                              {getTaskStatusLabel(task.status)}
                            </Badge>
                          </TableCell>
                          <TableCell className="text-right">
                            <div className="inline-flex flex-wrap justify-end gap-2">
                              {(task.status === "PENDING" || task.status === "OVERDUE") && (
                                <Button
                                  variant="outline"
                                  size="sm"
                                  onClick={() => acceptTaskMutation.mutate(task.taskId)}
                                  disabled={acceptTaskMutation.isPending}
                                >
                                  {t("employee.tasks.actions.acceptTask")}
                                </Button>
                              )}
                              {task.status !== "CANCELLED" && (
                                <Button size="sm" onClick={() => setProgressTaskId(task.taskId)}>
                                  {t("employee.tasks.actions.reportProgress")}
                                </Button>
                              )}
                            </div>
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      <Dialog open={progressTaskId !== null} onOpenChange={(open) => !open && closeProgressDialog()}>
        <DialogContent className="w-[95vw] max-w-lg">
          <DialogHeader>
            <BackButton onClick={closeProgressDialog} className="w-fit" />
            <DialogTitle>{t("employee.tasks.progressDialog.title")}</DialogTitle>
          </DialogHeader>
          <div className="space-y-3">
            <p className="text-sm text-muted-foreground">
              {t("employee.tasks.progressDialog.taskLabel")}{" "}
              <span className="text-foreground">{selectedTask?.title || t("common.notAvailable")}</span>
            </p>
            <div className="space-y-2">
              <Label>{t("employee.tasks.progressDialog.progressPercent")}</Label>
              <Input
                type="number"
                min={0}
                max={100}
                value={progressPercent}
                onChange={(event) => setProgressPercent(event.target.value)}
              />
            </div>
            <div className="space-y-2">
              <Label>{t("employee.tasks.progressDialog.note")}</Label>
              <Textarea
                rows={4}
                value={progressNote}
                onChange={(event) => setProgressNote(event.target.value)}
                placeholder={t("employee.tasks.progressDialog.notePlaceholder")}
              />
            </div>
            <div className="space-y-2">
              <Label>{t("employee.tasks.progressDialog.evidenceUrl")}</Label>
              <Input
                value={evidenceUrl}
                onChange={(event) => setEvidenceUrl(event.target.value)}
                placeholder="https://..."
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={closeProgressDialog}>
              {t("common.cancel")}
            </Button>
            <Button onClick={handleSubmitProgress} disabled={reportProgressMutation.isPending}>
              {t("employee.tasks.progressDialog.submit")}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog open={planSeasonId !== null} onOpenChange={(open) => !open && setPlanSeasonId(null)}>
        <DialogContent className="w-[95vw] max-w-4xl max-h-[85vh] overflow-hidden">
          <DialogHeader>
            <BackButton onClick={() => setPlanSeasonId(null)} className="w-fit" />
            <DialogTitle>
              {t("employee.tasks.planDialog.title", { season: selectedPlanSeasonName })}
            </DialogTitle>
          </DialogHeader>
          {isSeasonPlanLoading ? (
            <p className="text-sm text-muted-foreground">{t("employee.tasks.planDialog.loading")}</p>
          ) : !seasonPlanTasks || seasonPlanTasks.length === 0 ? (
            <p className="text-sm text-muted-foreground">{t("employee.tasks.planDialog.empty")}</p>
          ) : (
            <div className="max-h-[55vh] overflow-auto">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>{t("employee.tasks.table.task")}</TableHead>
                    <TableHead>{t("employee.tasks.table.dueDate")}</TableHead>
                    <TableHead>{t("employee.tasks.table.status")}</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {seasonPlanTasks.map((task) => (
                    <TableRow key={task.taskId}>
                      <TableCell>
                        <div className="space-y-1">
                          <p className="font-medium">{task.title}</p>
                          <p className="text-xs text-muted-foreground">{task.description || "-"}</p>
                        </div>
                      </TableCell>
                      <TableCell>{task.dueDate || "-"}</TableCell>
                      <TableCell>
                        <Badge className={getTaskStatusClassName(task.status)}>
                          {getTaskStatusLabel(task.status)}
                        </Badge>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
          )}
          <DialogFooter>
            <Button variant="outline" onClick={() => setPlanSeasonId(null)}>
              {t("common.close")}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}



