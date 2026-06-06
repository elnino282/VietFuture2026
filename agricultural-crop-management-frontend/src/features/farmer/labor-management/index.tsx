import {
  useAddSeasonEmployee,
  useAssignTaskToEmployee,
  useBulkAssignSeasonEmployees,
  useEmployeeDirectory,
  useRecalculateSeasonPayroll,
  useRemoveSeasonEmployee,
  useSeasonEmployees,
  useSeasonPayrollRecords,
  useSeasonProgressLogs,
} from "@/entities/labor";
import { useTasksBySeason } from "@/entities/task";
import { useMySeasons } from "@/entities/season/api/hooks";
import { useSeasonById } from "@/entities/season";
import {
  Badge,
  Button,
  Card,
  CardContent,
  CardHeader,
  CardTitle,
  Checkbox,
  Input,
  Label,
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
  Separator,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
  Tabs,
  TabsContent,
  TabsList,
  TabsTrigger,
  PageContainer,
} from "@/shared/ui";
import { useI18n } from "@/shared/lib/hooks/useI18n";
import { Calendar, ExternalLink, RefreshCw, Trash2, UserPlus } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import { useParams, useSearchParams } from "react-router-dom";
import { toast } from "sonner";

const LABOR_WORKSPACE_TABS = ["employees", "assignment", "progress", "payroll"] as const;
type LaborWorkspaceTab = (typeof LABOR_WORKSPACE_TABS)[number];

const formatDate = (value?: string | null, locale = "en-US") => {
  if (!value) return "-";
  try {
    return new Date(value).toLocaleString(locale);
  } catch {
    return value;
  }
};

const formatMoney = (value?: number | null, locale = "en-US") => {
  if (value === undefined || value === null) return "-";
  return value.toLocaleString(locale);
};

const getTaskStatusLabelKey = (status?: string) => {
  switch (status) {
    case "PENDING":
      return "tasks.status.pending";
    case "IN_PROGRESS":
      return "tasks.status.inProgress";
    case "DONE":
      return "tasks.status.done";
    case "OVERDUE":
      return "tasks.status.overdue";
    case "CANCELLED":
      return "seasonWorkspace.status.cancelled";
    default:
      return null;
  }
};

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

export function LaborManagementPage() {
  const { t, locale } = useI18n();
  const { seasonId: workspaceSeasonIdParam } = useParams();
  const [searchParams, setSearchParams] = useSearchParams();
  const workspaceSeasonId = Number(workspaceSeasonIdParam);
  const isWorkspaceScoped = Number.isFinite(workspaceSeasonId) && workspaceSeasonId > 0;

  // Season selection - local state with useMySeasons
  const { data: mySeasons, isLoading: isSeasonsLoading } = useMySeasons();
  const { data: workspaceSeasonDetail } = useSeasonById(workspaceSeasonId, {
    enabled: isWorkspaceScoped,
  });
  const [selectedSeasonId, setSelectedSeasonId] = useState<number | null>(
    isWorkspaceScoped ? workspaceSeasonId : null
  );
  const seasonId = selectedSeasonId ?? 0;

  // Keep season selection aligned with workspace route when scoped by season
  useEffect(() => {
    if (!isWorkspaceScoped) return;
    if (selectedSeasonId !== workspaceSeasonId) {
      setSelectedSeasonId(workspaceSeasonId);
    }
  }, [isWorkspaceScoped, selectedSeasonId, workspaceSeasonId]);

  // Auto-select first season when data loads
  useEffect(() => {
    if (isWorkspaceScoped) return;
    if (!isSeasonsLoading && mySeasons && mySeasons.length > 0 && selectedSeasonId === null) {
      setSelectedSeasonId(mySeasons[0].seasonId);
    }
  }, [isWorkspaceScoped, isSeasonsLoading, mySeasons, selectedSeasonId]);

  const selectedSeasonName = useMemo(() => {
    if (selectedSeasonId && mySeasons) {
      const found = mySeasons.find((s) => s.seasonId === selectedSeasonId);
      if (found) {
        return `${found.seasonName}${found.status ? ` (${found.status})` : ""}`;
      }
    }

    if (isWorkspaceScoped && workspaceSeasonDetail) {
      return `${workspaceSeasonDetail.seasonName}${workspaceSeasonDetail.status ? ` (${workspaceSeasonDetail.status})` : ""}`;
    }

    return selectedSeasonId ? t("seasonWorkspace.fallbackSeasonName", { id: selectedSeasonId }) : null;
  }, [selectedSeasonId, mySeasons, isWorkspaceScoped, workspaceSeasonDetail, t]);

  const selectedSeasonStatus = useMemo(() => {
    if (selectedSeasonId && mySeasons) {
      const seasonStatus = mySeasons.find((s) => s.seasonId === selectedSeasonId)?.status;
      if (seasonStatus) return seasonStatus;
    }
    return isWorkspaceScoped ? workspaceSeasonDetail?.status ?? null : null;
  }, [selectedSeasonId, mySeasons, isWorkspaceScoped, workspaceSeasonDetail]);

  const isSeasonLocked =
    selectedSeasonStatus === "COMPLETED"
    || selectedSeasonStatus === "CANCELLED"
    || selectedSeasonStatus === "ARCHIVED";

  const [selectedDirectoryEmployeeId, setSelectedDirectoryEmployeeId] = useState<string>("");
  const [selectedBulkEmployeeIds, setSelectedBulkEmployeeIds] = useState<number[]>([]);
  const [wagePerTask, setWagePerTask] = useState<string>("");
  const [taskAssigneeDraft, setTaskAssigneeDraft] = useState<Record<number, string>>({});

  const { data: employeeDirectoryData, isLoading: isEmployeeDirectoryLoading } = useEmployeeDirectory(
    { page: 0, size: 200 },
    { enabled: seasonId > 0 }
  );

  const { data: seasonEmployeesData, isLoading: isSeasonEmployeesLoading } = useSeasonEmployees(
    seasonId,
    { page: 0, size: 200 },
    { enabled: seasonId > 0 }
  );

  const { data: tasksData, isLoading: isTasksLoading } = useTasksBySeason(
    seasonId,
    { page: 0, size: 200, sortBy: "dueDate", sortDirection: "asc" },
    { enabled: seasonId > 0 }
  );

  const { data: progressData, isLoading: isProgressLoading } = useSeasonProgressLogs(
    seasonId,
    { page: 0, size: 100 },
    { enabled: seasonId > 0 }
  );

  const { data: payrollData, isLoading: isPayrollLoading } = useSeasonPayrollRecords(
    seasonId,
    { page: 0, size: 100 },
    { enabled: seasonId > 0 }
  );

  const addSeasonEmployeeMutation = useAddSeasonEmployee(seasonId, {
    onSuccess: () => {
      toast.success(t("laborWorkspace.toast.addEmployeeSuccess"));
      setSelectedDirectoryEmployeeId("");
      setWagePerTask("");
    },
    onError: (error) => toast.error(error.message || t("laborWorkspace.toast.addEmployeeError")),
  });

  const bulkAssignSeasonEmployeesMutation = useBulkAssignSeasonEmployees(seasonId, {
    onSuccess: (rows) => {
      toast.success(t("laborWorkspace.toast.bulkAddEmployeeSuccess", { count: rows.length }));
      setSelectedBulkEmployeeIds([]);
      setWagePerTask("");
    },
    onError: (error) => toast.error(error.message || t("laborWorkspace.toast.bulkAddEmployeeError")),
  });

  const removeSeasonEmployeeMutation = useRemoveSeasonEmployee(seasonId, {
    onSuccess: () => toast.success(t("laborWorkspace.toast.removeEmployeeSuccess")),
    onError: (error) => toast.error(error.message || t("laborWorkspace.toast.removeEmployeeError")),
  });

  const assignTaskMutation = useAssignTaskToEmployee(seasonId, {
    onSuccess: () => toast.success(t("laborWorkspace.toast.assignTaskSuccess")),
    onError: (error) => toast.error(error.message || t("laborWorkspace.toast.assignTaskError")),
  });

  const recalculatePayrollMutation = useRecalculateSeasonPayroll(seasonId, {
    onSuccess: () => toast.success(t("laborWorkspace.toast.recalculatePayrollSuccess")),
    onError: (error) => toast.error(error.message || t("laborWorkspace.toast.recalculatePayrollError")),
  });

  const seasonEmployees = seasonEmployeesData?.items ?? [];
  const tasks = tasksData?.items ?? [];
  const progressLogs = progressData?.items ?? [];
  const payrollRecords = payrollData?.items ?? [];

  const seasonEmployeeIds = useMemo(
    () => new Set(seasonEmployees.map((employee) => employee.employeeUserId)),
    [seasonEmployees]
  );

  const availableDirectoryEmployees = useMemo(
    () =>
      (employeeDirectoryData?.items ?? []).filter(
        (employee) => !seasonEmployeeIds.has(employee.userId)
      ),
    [employeeDirectoryData, seasonEmployeeIds]
  );

  const activeSeasonEmployees = useMemo(
    () =>
      seasonEmployees
        .filter((employee) => employee.active !== false)
        .map((employee) => ({
          userId: employee.employeeUserId,
          label:
            employee.employeeName ||
            employee.employeeUsername ||
            employee.employeeEmail ||
            t("seasonWorkspace.employeeFallback", { id: employee.employeeUserId }),
        })),
    [seasonEmployees, t]
  );

  const canMutateSeason = !!selectedSeasonId && !isSeasonLocked;
  const tabParam = searchParams.get("tab");
  const activeTab: LaborWorkspaceTab = LABOR_WORKSPACE_TABS.includes(tabParam as LaborWorkspaceTab)
    ? (tabParam as LaborWorkspaceTab)
    : "employees";

  const handleTabChange = (value: string) => {
    setSearchParams((prev) => {
      const next = new URLSearchParams(prev);
      next.set("tab", value);
      return next;
    });
  };

  const handleAddSeasonEmployee = () => {
    if (!canMutateSeason) {
      toast.error(t("laborWorkspace.validation.seasonLockedUpdate"));
      return;
    }
    if (!selectedDirectoryEmployeeId) {
      toast.error(t("laborWorkspace.validation.selectEmployee"));
      return;
    }

    const parsedWage = wagePerTask.trim().length ? Number(wagePerTask) : undefined;
    if (parsedWage !== undefined && (Number.isNaN(parsedWage) || parsedWage < 0)) {
      toast.error(t("laborWorkspace.validation.invalidWagePerTask"));
      return;
    }

    addSeasonEmployeeMutation.mutate({
      employeeUserId: Number(selectedDirectoryEmployeeId),
      wagePerTask: parsedWage,
    });
  };

  const handleBulkAssignSeasonEmployees = () => {
    if (!canMutateSeason) {
      toast.error(t("laborWorkspace.validation.seasonLockedUpdate"));
      return;
    }
    if (selectedBulkEmployeeIds.length === 0) {
      toast.error(t("laborWorkspace.validation.selectAtLeastOneEmployee"));
      return;
    }

    const parsedWage = wagePerTask.trim().length ? Number(wagePerTask) : undefined;
    if (parsedWage !== undefined && (Number.isNaN(parsedWage) || parsedWage < 0)) {
      toast.error(t("laborWorkspace.validation.invalidWagePerTask"));
      return;
    }

    bulkAssignSeasonEmployeesMutation.mutate({
      employeeUserIds: selectedBulkEmployeeIds,
      wagePerTask: parsedWage,
    });
  };

  const handleAssignTask = (taskId: number) => {
    if (!canMutateSeason) {
      toast.error(t("laborWorkspace.validation.seasonLockedAssign"));
      return;
    }
    const selectedAssigneeId = taskAssigneeDraft[taskId];
    if (!selectedAssigneeId) {
      toast.error(t("laborWorkspace.validation.selectEmployeeForAssignment"));
      return;
    }

    assignTaskMutation.mutate({
      taskId,
      employeeUserId: Number(selectedAssigneeId),
    });
  };

  const handleRemoveSeasonEmployee = (employeeUserId: number) => {
    if (!canMutateSeason) {
      toast.error(t("laborWorkspace.validation.seasonLockedDelete"));
      return;
    }
    removeSeasonEmployeeMutation.mutate(employeeUserId);
  };

  const isLoadingBase = isEmployeeDirectoryLoading || isSeasonEmployeesLoading;

  if (!isWorkspaceScoped && isSeasonsLoading) {
    return (
      <PageContainer variant="wide">
        <Card className="rounded-2xl border border-border">
          <CardContent className="p-6 space-y-3">
            <div className="flex items-center gap-2 text-muted-foreground">
              <RefreshCw className="w-4 h-4 animate-spin" />
              {t("laborWorkspace.loadingSeasons")}
            </div>
          </CardContent>
        </Card>
      </PageContainer>
    );
  }

  if (!isWorkspaceScoped && (!mySeasons || mySeasons.length === 0)) {
    return (
      <PageContainer variant="wide">
        <Card className="rounded-2xl border border-border">
          <CardContent className="p-6 space-y-3">
            <h2 className="text-lg text-foreground">{t("laborWorkspace.emptySeasonsTitle")}</h2>
            <p className="text-sm text-muted-foreground">
              {t("laborWorkspace.emptySeasonsDescription")}
            </p>
          </CardContent>
        </Card>
      </PageContainer>
    );
  }

  return (
    <PageContainer variant="wide" className="space-y-6">
      <Card className="rounded-2xl border border-border">
        <CardHeader>
          <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
            <div>
              <CardTitle>{t("laborWorkspace.title")}</CardTitle>
              <p className="text-sm text-muted-foreground mt-1">
                {selectedSeasonName
                  ? t("laborWorkspace.currentSeasonLabel", { season: selectedSeasonName })
                  : t("laborWorkspace.selectSeasonToStart")}
              </p>
            </div>
            {isWorkspaceScoped ? (
              <div className="flex items-center gap-2">
                <Calendar className="w-4 h-4 text-muted-foreground" />
                <Badge className="bg-muted text-foreground border-border">
                  {selectedSeasonName ?? t("seasonWorkspace.fallbackSeasonName", { id: workspaceSeasonId })}
                </Badge>
              </div>
            ) : (
              <div className="flex items-center gap-2">
                <Calendar className="w-4 h-4 text-muted-foreground" />
                <Select
                  value={selectedSeasonId?.toString() ?? ""}
                  onValueChange={(val) => setSelectedSeasonId(Number(val))}
                >
                  <SelectTrigger className="w-[260px]">
                    <SelectValue placeholder={t("laborWorkspace.selectSeasonPlaceholder")} />
                  </SelectTrigger>
                  <SelectContent>
                    {(mySeasons ?? []).map((season) => (
                      <SelectItem
                        key={season.seasonId}
                        value={season.seasonId.toString()}
                      >
                        {season.seasonName}{" "}
                        {season.status && `(${season.status})`}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            )}
          </div>
        </CardHeader>
      </Card>

      {isSeasonLocked && (
        <Card className="rounded-2xl border border-amber-300 bg-amber-50">
          <CardContent className="p-4 text-sm text-amber-900">
            {t("laborWorkspace.lockedSeasonBanner")}
          </CardContent>
        </Card>
      )}

      <Tabs value={activeTab} onValueChange={handleTabChange} className="space-y-4">
        <TabsList>
          <TabsTrigger value="employees">{t("laborWorkspace.tabs.employees")}</TabsTrigger>
          <TabsTrigger value="assignment">{t("laborWorkspace.tabs.assignment")}</TabsTrigger>
          <TabsTrigger value="progress">{t("laborWorkspace.tabs.progress")}</TabsTrigger>
          <TabsTrigger value="payroll">{t("laborWorkspace.tabs.payroll")}</TabsTrigger>
        </TabsList>

        <TabsContent value="employees" className="space-y-4">
          <Card className="rounded-2xl border border-border">
            <CardHeader>
              <CardTitle className="text-base">{t("laborWorkspace.sections.addEmployees.title")}</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
                <div className="space-y-2">
                  <Label>{t("laborWorkspace.fields.availableEmployees")}</Label>
                  <Select
                    value={selectedDirectoryEmployeeId}
                    onValueChange={setSelectedDirectoryEmployeeId}
                    disabled={!canMutateSeason}
                  >
                    <SelectTrigger className="h-11 rounded-xl border border-slate-300 bg-white px-4 text-sm shadow-sm data-[placeholder]:text-slate-400 hover:border-slate-400 focus:border-[#3BA55D] focus:ring-2 focus:ring-[#3BA55D]/20 focus-visible:border-[#3BA55D] focus-visible:ring-2 focus-visible:ring-[#3BA55D]/20">
                      <SelectValue placeholder={t("laborWorkspace.fields.selectEmployeePlaceholder")} />
                    </SelectTrigger>
                    <SelectContent>
                      {availableDirectoryEmployees.map((employee) => (
                        <SelectItem key={employee.userId} value={String(employee.userId)}>
                          {employee.fullName || employee.username || employee.email || t("seasonWorkspace.employeeFallback", { id: employee.userId })}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>

                <div className="space-y-2">
                  <Label>{t("laborWorkspace.fields.wagePerTaskVnd")}</Label>
                  <Input
                    type="number"
                    min={0}
                    value={wagePerTask}
                    onChange={(event) => setWagePerTask(event.target.value)}
                    placeholder={t("laborWorkspace.fields.wagePerTaskPlaceholder")}
                    disabled={!canMutateSeason}
                    className="h-11 rounded-xl border border-slate-300 bg-white px-4 text-sm shadow-sm placeholder:text-slate-400 hover:border-slate-400 focus:border-[#3BA55D] focus:ring-2 focus:ring-[#3BA55D]/20 focus-visible:border-[#3BA55D] focus-visible:ring-2 focus-visible:ring-[#3BA55D]/20"
                  />
                </div>

                <div className="flex items-end">
                  <Button
                    className="w-full"
                    onClick={handleAddSeasonEmployee}
                    disabled={!canMutateSeason || addSeasonEmployeeMutation.isPending || !selectedDirectoryEmployeeId}
                  >
                    <UserPlus className="w-4 h-4 mr-2" />
                    {t("laborWorkspace.actions.addEmployee")}
                  </Button>
                </div>
              </div>

              <Separator />

              <div className="space-y-3">
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <p className="text-sm text-muted-foreground">
                    {t("laborWorkspace.bulk.description")}
                  </p>
                  <div className="flex items-center gap-2">
                    <Button
                      type="button"
                      size="sm"
                      variant="outline"
                      onClick={() =>
                        setSelectedBulkEmployeeIds(
                          availableDirectoryEmployees.map((employee) => employee.userId)
                        )
                      }
                      disabled={!canMutateSeason || availableDirectoryEmployees.length === 0}
                    >
                      {t("laborWorkspace.bulk.selectAll")}
                    </Button>
                    <Button
                      type="button"
                      size="sm"
                      variant="outline"
                      className="rounded-xl border-slate-300 bg-white shadow-sm hover:border-slate-400 hover:bg-white focus-visible:border-[#3BA55D] focus-visible:ring-2 focus-visible:ring-[#3BA55D]/20"
                      onClick={() => setSelectedBulkEmployeeIds([])}
                      disabled={selectedBulkEmployeeIds.length === 0}
                    >
                      {t("laborWorkspace.bulk.clearSelection")}
                    </Button>
                  </div>
                </div>

                {availableDirectoryEmployees.length === 0 ? (
                  <p className="text-sm text-muted-foreground">
                    {t("laborWorkspace.bulk.emptyAvailableEmployees")}
                  </p>
                ) : (
                  <div className="max-h-56 overflow-y-auto rounded-xl border border-border p-3 space-y-2">
                    {availableDirectoryEmployees.map((employee) => {
                      const checked = selectedBulkEmployeeIds.includes(employee.userId);
                      const label = employee.fullName || employee.username || employee.email || t("seasonWorkspace.employeeFallback", { id: employee.userId });
                      return (
                        <label
                          key={employee.userId}
                          className="flex items-center gap-3 rounded-lg border border-border px-3 py-2 hover:bg-muted/40"
                        >
                          <Checkbox
                            checked={checked}
                            onCheckedChange={(nextChecked) =>
                              setSelectedBulkEmployeeIds((prev) => {
                                if (nextChecked) {
                                  return prev.includes(employee.userId)
                                    ? prev
                                    : [...prev, employee.userId];
                                }
                                return prev.filter((id) => id !== employee.userId);
                              })
                            }
                            disabled={!canMutateSeason}
                          />
                          <span className="text-sm text-foreground">{label}</span>
                        </label>
                      );
                    })}
                  </div>
                )}

                <div className="flex justify-end">
                  <Button
                    onClick={handleBulkAssignSeasonEmployees}
                    disabled={
                      !canMutateSeason
                      || bulkAssignSeasonEmployeesMutation.isPending
                      || selectedBulkEmployeeIds.length === 0
                    }
                  >
                    <UserPlus className="w-4 h-4 mr-2" />
                    {t("laborWorkspace.actions.bulkAddEmployees")}
                  </Button>
                </div>
              </div>
            </CardContent>
          </Card>

          <Card className="rounded-2xl border border-border">
            <CardHeader>
              <CardTitle className="text-base">{t("laborWorkspace.sections.seasonEmployees.title")}</CardTitle>
            </CardHeader>
            <CardContent>
              {isLoadingBase ? (
                <p className="text-sm text-muted-foreground">{t("laborWorkspace.sections.seasonEmployees.loading")}</p>
              ) : seasonEmployees.length === 0 ? (
                <p className="text-sm text-muted-foreground">{t("laborWorkspace.sections.seasonEmployees.empty")}</p>
              ) : (
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>{t("laborWorkspace.table.employee")}</TableHead>
                      <TableHead>{t("laborWorkspace.table.email")}</TableHead>
                      <TableHead>{t("laborWorkspace.table.wagePerTask")}</TableHead>
                      <TableHead>{t("laborWorkspace.table.status")}</TableHead>
                      <TableHead className="text-right">{t("laborWorkspace.table.actions")}</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {seasonEmployees.map((employee) => (
                      <TableRow key={employee.id}>
                        <TableCell>
                          {employee.employeeName || employee.employeeUsername || t("seasonWorkspace.employeeFallback", { id: employee.employeeUserId })}
                        </TableCell>
                        <TableCell>{employee.employeeEmail ?? "-"}</TableCell>
                        <TableCell>{formatMoney(employee.wagePerTask, locale)}</TableCell>
                        <TableCell>
                          <Badge
                            className={
                              employee.active === false
                                ? "bg-slate-100 text-slate-600 border-slate-200"
                                : "bg-emerald-100 text-emerald-700 border-emerald-200"
                            }
                          >
                            {employee.active === false ? t("laborWorkspace.status.inactive") : t("laborWorkspace.status.active")}
                          </Badge>
                        </TableCell>
                        <TableCell className="text-right">
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => handleRemoveSeasonEmployee(employee.employeeUserId)}
                            disabled={!canMutateSeason || removeSeasonEmployeeMutation.isPending}
                          >
                            <Trash2 className="w-4 h-4 mr-2" />
                            {t("common.delete")}
                          </Button>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              )}
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="assignment" className="space-y-4">
          <Card className="rounded-2xl border border-border">
            <CardHeader>
              <CardTitle className="text-base">{t("laborWorkspace.sections.assignment.title")}</CardTitle>
            </CardHeader>
            <CardContent>
              {isTasksLoading ? (
                <p className="text-sm text-muted-foreground">{t("laborWorkspace.sections.assignment.loading")}</p>
              ) : tasks.length === 0 ? (
                <p className="text-sm text-muted-foreground">{t("laborWorkspace.sections.assignment.empty")}</p>
              ) : (
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>{t("laborWorkspace.assignmentTable.task")}</TableHead>
                      <TableHead>{t("laborWorkspace.assignmentTable.status")}</TableHead>
                      <TableHead>{t("laborWorkspace.assignmentTable.currentAssignee")}</TableHead>
                      <TableHead>{t("laborWorkspace.assignmentTable.newAssignee")}</TableHead>
                      <TableHead className="text-right">{t("laborWorkspace.assignmentTable.actions")}</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {tasks.map((task) => {
                      const fallbackCurrentAssignee = task.userName || (task.userId ? t("laborWorkspace.userFallback", { id: task.userId }) : "-");
                      const currentSelection =
                        taskAssigneeDraft[task.taskId] ??
                        (activeSeasonEmployees.some((employee) => employee.userId === task.userId)
                          ? String(task.userId)
                          : "");
                      return (
                        <TableRow key={task.taskId}>
                          <TableCell>{task.title}</TableCell>
                          <TableCell>
                            <Badge className={getTaskStatusClassName(task.status)}>
                              {(() => {
                                const taskStatusLabelKey = getTaskStatusLabelKey(task.status);
                                return taskStatusLabelKey ? t(taskStatusLabelKey) : task.status ?? "-";
                              })()}
                            </Badge>
                          </TableCell>
                          <TableCell>{fallbackCurrentAssignee}</TableCell>
                          <TableCell>
                            <Select
                              value={currentSelection}
                              onValueChange={(value) =>
                                setTaskAssigneeDraft((prev) => ({
                                  ...prev,
                                  [task.taskId]: value,
                                }))
                              }
                              disabled={!canMutateSeason}
                            >
                              <SelectTrigger className="w-[260px]">
                                <SelectValue placeholder={t("laborWorkspace.fields.selectEmployeePlaceholder")} />
                              </SelectTrigger>
                              <SelectContent>
                                {activeSeasonEmployees.map((employee) => (
                                  <SelectItem key={employee.userId} value={String(employee.userId)}>
                                    {employee.label}
                                  </SelectItem>
                                ))}
                              </SelectContent>
                            </Select>
                          </TableCell>
                          <TableCell className="text-right">
                            <Button
                              size="sm"
                              onClick={() => handleAssignTask(task.taskId)}
                              disabled={!canMutateSeason || assignTaskMutation.isPending || !currentSelection}
                            >
                              {t("laborWorkspace.actions.assignTask")}
                            </Button>
                          </TableCell>
                        </TableRow>
                      );
                    })}
                  </TableBody>
                </Table>
              )}
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="progress" className="space-y-4">
          <Card className="rounded-2xl border border-border">
            <CardHeader>
              <CardTitle className="text-base">{t("laborWorkspace.sections.progress.title")}</CardTitle>
            </CardHeader>
            <CardContent>
              {isProgressLoading ? (
                <p className="text-sm text-muted-foreground">{t("laborWorkspace.sections.progress.loading")}</p>
              ) : progressLogs.length === 0 ? (
                <p className="text-sm text-muted-foreground">{t("laborWorkspace.sections.progress.empty")}</p>
              ) : (
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>{t("laborWorkspace.progressTable.employee")}</TableHead>
                      <TableHead>{t("laborWorkspace.progressTable.task")}</TableHead>
                      <TableHead>{t("laborWorkspace.progressTable.progress")}</TableHead>
                      <TableHead>{t("laborWorkspace.progressTable.note")}</TableHead>
                      <TableHead>{t("laborWorkspace.progressTable.evidence")}</TableHead>
                      <TableHead>{t("laborWorkspace.progressTable.time")}</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {progressLogs.map((log) => (
                      <TableRow key={log.id}>
                        <TableCell>{log.employeeName || t("seasonWorkspace.employeeFallback", { id: log.employeeUserId })}</TableCell>
                        <TableCell>{log.taskTitle || t("laborWorkspace.taskFallback", { id: log.taskId })}</TableCell>
                        <TableCell>{log.progressPercent}%</TableCell>
                        <TableCell className="max-w-[320px] truncate">{log.note || "-"}</TableCell>
                        <TableCell>
                          {log.evidenceUrl ? (
                            <a
                              href={log.evidenceUrl}
                              target="_blank"
                              rel="noreferrer"
                              className="inline-flex items-center gap-1 text-sm text-primary hover:underline"
                            >
                              {t("laborWorkspace.progressTable.openEvidence")}
                              <ExternalLink className="h-3.5 w-3.5" />
                            </a>
                          ) : (
                            "-"
                          )}
                        </TableCell>
                        <TableCell>{formatDate(log.loggedAt, locale)}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              )}
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="payroll" className="space-y-4">
          <Card className="rounded-2xl border border-border">
            <CardHeader className="flex flex-row items-center justify-between">
              <CardTitle className="text-base">{t("laborWorkspace.sections.payroll.title")}</CardTitle>
              <Button
                onClick={() => recalculatePayrollMutation.mutate(undefined)}
                disabled={!canMutateSeason || recalculatePayrollMutation.isPending}
              >
                <RefreshCw className="w-4 h-4 mr-2" />
                {t("laborWorkspace.actions.recalculatePayroll")}
              </Button>
            </CardHeader>
            <CardContent>
              {isPayrollLoading ? (
                <p className="text-sm text-muted-foreground">{t("laborWorkspace.sections.payroll.loading")}</p>
              ) : payrollRecords.length === 0 ? (
                <p className="text-sm text-muted-foreground">{t("laborWorkspace.sections.payroll.empty")}</p>
              ) : (
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>{t("laborWorkspace.payrollTable.employee")}</TableHead>
                      <TableHead>{t("laborWorkspace.payrollTable.period")}</TableHead>
                      <TableHead>{t("laborWorkspace.payrollTable.completedTasks")}</TableHead>
                      <TableHead>{t("laborWorkspace.payrollTable.wagePerTask")}</TableHead>
                      <TableHead>{t("laborWorkspace.payrollTable.totalAmount")}</TableHead>
                      <TableHead>{t("laborWorkspace.payrollTable.updatedAt")}</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {payrollRecords.map((record) => (
                      <TableRow key={record.id}>
                        <TableCell>{record.employeeName || t("seasonWorkspace.employeeFallback", { id: record.employeeUserId })}</TableCell>
                        <TableCell>
                          {record.periodStart ?? "-"} - {record.periodEnd ?? "-"}
                        </TableCell>
                        <TableCell>
                          {record.totalCompletedTasks} / {record.totalAssignedTasks}
                        </TableCell>
                        <TableCell>{formatMoney(record.wagePerTask, locale)}</TableCell>
                        <TableCell>{formatMoney(record.totalAmount, locale)}</TableCell>
                        <TableCell>{formatDate(record.generatedAt, locale)}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              )}
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
    </PageContainer>
  );
}



