import {
  Alert,
  AlertDescription,
  AlertTitle,
  Badge,
  Button,
  CardContent,
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
  Input,
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
  Skeleton,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/shared/ui";
import {
  adminIncidentApi,
  adminInventoryApi,
  type AdminIncident,
} from "@/features/admin/shared/api";
import {
  AdminContentCard,
  AdminHeaderCard,
  AdminPageContainer,
} from "@/features/admin/shared/ui";
import { usePreferences } from "@/shared/contexts";
import { useDebounce } from "@/shared/lib";
import {
  keepPreviousData,
  useMutation,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query";
import { AlertCircle, CheckCircle2, Clock, MoreVertical } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import { useSearchParams } from "react-router-dom";

const STATUS_OPTIONS = [
  { value: "ALL", label: "All statuses" },
  { value: "OPEN", label: "Open" },
  { value: "IN_PROGRESS", label: "In Progress" },
  { value: "RESOLVED", label: "Resolved" },
];

const SEVERITY_OPTIONS = [
  { value: "ALL", label: "All severities" },
  { value: "LOW", label: "Low" },
  { value: "MEDIUM", label: "Medium" },
  { value: "HIGH", label: "High" },
];

const SORT_OPTIONS = [
  { value: "NEWEST", label: "Newest" },
  { value: "OLDEST", label: "Oldest" },
];

const parseNumber = (value: string | null) => {
  if (!value) return undefined;
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : undefined;
};

const formatDate = (value: string | null | undefined, locale: string) => {
  if (!value) return "-";
  try {
    return new Date(value).toLocaleDateString(locale);
  } catch {
    return value;
  }
};

export function AdminIncidentsPage() {
  const { preferences } = usePreferences();
  const [searchParams, setSearchParams] = useSearchParams();
  const [page, setPage] = useState(0);
  const [detailOpen, setDetailOpen] = useState(false);
  const [selectedIncident, setSelectedIncident] =
    useState<AdminIncident | null>(null);
  const [searchInput, setSearchInput] = useState(searchParams.get("q") ?? "");

  const farmId = parseNumber(searchParams.get("farmId"));
  const status = (searchParams.get("status") || "ALL").toUpperCase();
  const severity = (searchParams.get("severity") || "ALL").toUpperCase();
  const sort = (searchParams.get("sort") || "NEWEST").toUpperCase();
  const debouncedSearch = useDebounce(searchInput, 400);

  useEffect(() => {
    setPage(0);
  }, [farmId, status, severity, sort, debouncedSearch]);

  useEffect(() => {
    setSearchParams((prev) => {
      const next = new URLSearchParams(prev);
      if (debouncedSearch) {
        next.set("q", debouncedSearch);
      } else {
        next.delete("q");
      }
      return next;
    });
  }, [debouncedSearch, setSearchParams]);

  const updateParams = (
    updates: Record<string, string | number | undefined>,
  ) => {
    setSearchParams((prev) => {
      const next = new URLSearchParams(prev);
      Object.entries(updates).forEach(([key, value]) => {
        if (value === undefined || value === "") {
          next.delete(key);
        } else {
          next.set(key, String(value));
        }
      });
      return next;
    });
  };

  const optionsQuery = useQuery({
    queryKey: ["adminIncidents", "options"],
    queryFn: () => adminInventoryApi.getOptions(),
  });

  const incidentsQuery = useQuery({
    queryKey: [
      "adminIncidents",
      "list",
      farmId,
      status,
      severity,
      sort,
      debouncedSearch,
      page,
    ],
    queryFn: () =>
      adminIncidentApi.list({
        farmId: farmId ?? undefined,
        status: status === "ALL" ? undefined : status,
        severity: severity === "ALL" ? undefined : severity,
        sort,
        q: debouncedSearch || undefined,
        page,
        limit: 20,
      }),
    placeholderData: keepPreviousData,
  });

  const queryClient = useQueryClient();

  // Current query key for optimistic updates
  const currentQueryKey = [
    "adminIncidents",
    "list",
    farmId,
    status,
    severity,
    sort,
    debouncedSearch,
    page,
  ];

  const statusMutation = useMutation({
    mutationFn: ({ id, nextStatus }: { id: number; nextStatus: string }) =>
      adminIncidentApi.updateStatus(id, nextStatus),
    onMutate: async ({ id, nextStatus }) => {
      await queryClient.cancelQueries({ queryKey: ["adminIncidents", "list"] });
      const previousData = queryClient.getQueryData(currentQueryKey);

      // Optimistic update: change status immediately
      queryClient.setQueryData(currentQueryKey, (old: any) =>
        old
          ? {
              ...old,
              items: old.items?.map((incident: any) =>
                incident.incidentId === id
                  ? { ...incident, status: nextStatus }
                  : incident,
              ),
            }
          : old,
      );

      return { previousData };
    },
    onError: (_err, _variables, context: any) => {
      if (context?.previousData) {
        queryClient.setQueryData(currentQueryKey, context.previousData);
      }
    },
    onSuccess: (updatedIncident) => {
      setSelectedIncident(updatedIncident);
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: ["adminIncidents", "list"] });
    },
  });

  const formatNumber = useMemo(
    () => (value: number) =>
      new Intl.NumberFormat(preferences.locale).format(value),
    [preferences.locale],
  );

  const renderSeverityBadge = (value?: string | null) => {
    switch (value) {
      case "HIGH":
        return <Badge variant="destructive">High</Badge>;
      case "MEDIUM":
        return <Badge variant="warning">Medium</Badge>;
      case "LOW":
        return <Badge variant="secondary">Low</Badge>;
      default:
        return <Badge variant="outline">-</Badge>;
    }
  };

  const renderStatusBadge = (value?: string | null) => {
    switch (value) {
      case "OPEN":
        return (
          <Badge variant="destructive" className="gap-1">
            <AlertCircle className="h-3 w-3" />
            Open
          </Badge>
        );
      case "IN_PROGRESS":
        return (
          <Badge variant="info" className="gap-1">
            <Clock className="h-3 w-3" />
            In Progress
          </Badge>
        );
      case "RESOLVED":
        return (
          <Badge variant="success" className="gap-1">
            <CheckCircle2 className="h-3 w-3" />
            Resolved
          </Badge>
        );
      default:
        return <Badge variant="outline">-</Badge>;
    }
  };

  const handleView = (incident: AdminIncident) => {
    setSelectedIncident(incident);
    setDetailOpen(true);
  };

  return (
    <AdminPageContainer>
      <AdminHeaderCard
        title="Incidents"
        description="Review and update incident status system-wide."
      />

      <AdminContentCard>
        <CardContent className="space-y-4 p-4">
          <div className="flex flex-col items-stretch gap-3 sm:flex-row sm:flex-wrap sm:items-center">
            <Select
              value={farmId?.toString() ?? "all"}
              onValueChange={(value) =>
                updateParams({ farmId: value === "all" ? undefined : value })
              }
            >
              <SelectTrigger className="h-9 w-full rounded-[14px] sm:w-[220px]">
                <SelectValue placeholder="All farms" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">All farms</SelectItem>
                {(optionsQuery.data?.farms ?? []).map((farm) => (
                  <SelectItem key={farm.id} value={String(farm.id)}>
                    {farm.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>

            <Select
              value={status}
              onValueChange={(value) => updateParams({ status: value })}
            >
              <SelectTrigger className="h-9 w-full rounded-[14px] sm:w-[160px]">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {STATUS_OPTIONS.map((option) => (
                  <SelectItem key={option.value} value={option.value}>
                    {option.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>

            <Select
              value={severity}
              onValueChange={(value) => updateParams({ severity: value })}
            >
              <SelectTrigger className="h-9 w-full rounded-[14px] sm:w-[160px]">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {SEVERITY_OPTIONS.map((option) => (
                  <SelectItem key={option.value} value={option.value}>
                    {option.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>

            <Select
              value={sort}
              onValueChange={(value) => updateParams({ sort: value })}
            >
              <SelectTrigger className="h-9 w-full rounded-[14px] sm:w-[140px]">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {SORT_OPTIONS.map((option) => (
                  <SelectItem key={option.value} value={option.value}>
                    {option.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>

            <div className="relative w-full sm:w-[260px]">
              <Input
                placeholder="Search title or description"
                className="h-9 rounded-[14px]"
                value={searchInput}
                onChange={(event) => setSearchInput(event.target.value)}
              />
            </div>
          </div>

          {incidentsQuery.isLoading && (
            <div className="space-y-3">
              {[...Array(5)].map((_, i) => (
                <Skeleton key={i} className="h-10 w-full" />
              ))}
            </div>
          )}

          {incidentsQuery.isError && (
            <Alert variant="destructive">
              <AlertCircle className="h-4 w-4" />
              <AlertTitle>Failed to load incidents</AlertTitle>
              <AlertDescription className="mt-2 flex items-center justify-between gap-3">
                <span>
                  {incidentsQuery.error?.message || "Please try again."}
                </span>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => incidentsQuery.refetch()}
                >
                  Retry
                </Button>
              </AlertDescription>
            </Alert>
          )}

          {!incidentsQuery.isLoading && !incidentsQuery.isError && (
            <>
              {incidentsQuery.data?.items?.length ? (
                <>
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead>Title</TableHead>
                        <TableHead>Farm</TableHead>
                        <TableHead>Severity</TableHead>
                        <TableHead>Status</TableHead>
                        <TableHead>Created</TableHead>
                        <TableHead className="text-right">Actions</TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {incidentsQuery.data.items.map((incident) => (
                        <TableRow key={incident.incidentId}>
                          <TableCell className="font-medium">
                            {incident.incidentType || "Incident"}
                          </TableCell>
                          <TableCell>{incident.farmName || "-"}</TableCell>
                          <TableCell>
                            {renderSeverityBadge(incident.severity)}
                          </TableCell>
                          <TableCell>
                            {renderStatusBadge(incident.status)}
                          </TableCell>
                          <TableCell>
                            {formatDate(incident.createdAt, preferences.locale)}
                          </TableCell>
                          <TableCell className="text-right">
                            <DropdownMenu>
                              <DropdownMenuTrigger asChild>
                                <Button
                                  type="button"
                                  variant="ghost"
                                  size="icon"
                                  className="h-8 w-8 rounded-[14px]"
                                  aria-label={`Actions for ${incident.incidentType || "incident"}`}
                                >
                                  <MoreVertical className="h-4 w-4" />
                                </Button>
                              </DropdownMenuTrigger>
                              <DropdownMenuContent align="end" className="w-40">
                                <DropdownMenuItem
                                  onSelect={() => handleView(incident)}
                                >
                                  View incident
                                </DropdownMenuItem>
                              </DropdownMenuContent>
                            </DropdownMenu>
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>

                  <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between mt-4 text-sm text-muted-foreground">
                    <span>
                      {formatNumber(page + 1)} /{" "}
                      {formatNumber(incidentsQuery.data.totalPages)}
                    </span>
                    <div className="flex items-center gap-2">
                      <Button
                        variant="outline"
                        size="sm"
                        disabled={page === 0}
                        onClick={() => setPage((prev) => Math.max(prev - 1, 0))}
                      >
                        Previous
                      </Button>
                      <Button
                        variant="outline"
                        size="sm"
                        disabled={
                          page >= (incidentsQuery.data.totalPages ?? 1) - 1
                        }
                        onClick={() => setPage((prev) => prev + 1)}
                      >
                        Next
                      </Button>
                    </div>
                  </div>
                </>
              ) : (
                <div className="py-10 text-center text-sm text-muted-foreground">
                  No incidents match the current filters.
                </div>
              )}
            </>
          )}
        </CardContent>
      </AdminContentCard>

      {detailOpen && selectedIncident && (
        <div
          className="fixed inset-0 z-50 bg-background/80 backdrop-blur-sm cursor-pointer"
          onClick={() => setDetailOpen(false)}
        >
          <div
            className="fixed right-0 top-0 h-full w-full max-w-xl bg-card border-l border-border shadow-lg overflow-auto cursor-default"
            onClick={(event) => event.stopPropagation()}
          >
            <div className="p-6 space-y-6">
              <div className="flex items-center justify-between">
                <div>
                  <h2 className="text-lg font-semibold">Incident detail</h2>
                  <p className="text-sm text-muted-foreground">
                    {selectedIncident.incidentType}
                  </p>
                </div>
                <Button
                  variant="ghost"
                  size="icon"
                  onClick={() => setDetailOpen(false)}
                >
                  X
                </Button>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <p className="text-xs text-muted-foreground">Farm</p>
                  <p className="text-sm font-medium">
                    {selectedIncident.farmName || "-"}
                  </p>
                </div>
                <div>
                  <p className="text-xs text-muted-foreground">Status</p>
                  <div>{renderStatusBadge(selectedIncident.status)}</div>
                </div>
                <div>
                  <p className="text-xs text-muted-foreground">Severity</p>
                  <div>{renderSeverityBadge(selectedIncident.severity)}</div>
                </div>
                <div>
                  <p className="text-xs text-muted-foreground">Created</p>
                  <p className="text-sm font-medium">
                    {formatDate(selectedIncident.createdAt, preferences.locale)}
                  </p>
                </div>
              </div>

              <div>
                <p className="text-sm font-semibold mb-2">Description</p>
                <p className="text-sm text-muted-foreground whitespace-pre-wrap">
                  {selectedIncident.description || "No description provided."}
                </p>
              </div>

              <div>
                <p className="text-sm font-semibold mb-2">Update status</p>
                <Select
                  value={selectedIncident.status || ""}
                  onValueChange={(nextStatus) =>
                    statusMutation.mutate({
                      id: selectedIncident.incidentId,
                      nextStatus,
                    })
                  }
                >
                  <SelectTrigger className="h-9 w-full rounded-[14px] sm:w-[200px]">
                    <SelectValue placeholder="Select status" />
                  </SelectTrigger>
                  <SelectContent>
                    {STATUS_OPTIONS.filter(
                      (option) => option.value !== "ALL",
                    ).map((option) => (
                      <SelectItem key={option.value} value={option.value}>
                        {option.label}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            </div>
          </div>
        </div>
      )}
    </AdminPageContainer>
  );
}
