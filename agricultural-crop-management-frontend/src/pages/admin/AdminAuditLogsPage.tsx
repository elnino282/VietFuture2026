import {
  Alert,
  AlertDescription,
  AlertTitle,
  BackButton,
  Badge,
  Button,
  CardContent,
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
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
import { adminAuditLogApi } from "@/features/admin/shared/api";
import {
  AdminContentCard,
  AdminHeaderCard,
  AdminPageContainer,
} from "@/features/admin/shared/ui";
import { usePreferences } from "@/shared/contexts";
import { useI18n } from "@/shared/lib/hooks/useI18n";
import { useQuery } from "@tanstack/react-query";
import { AlertCircle } from "lucide-react";
import { useEffect, useState } from "react";

const PAGE_SIZE = 20;
const MODULE_OPTIONS = [
  "ALL",
  "IDENTITY",
  "INVENTORY",
  "SUSTAINABILITY",
  "INCIDENT",
  "FARM",
];

const toApiDateTime = (value: string) => {
  if (!value) {
    return undefined;
  }
  return value.length === 16 ? `${value}:00` : value;
};

const formatDateTime = (value: string, locale: string) => {
  if (!value) {
    return "-";
  }
  try {
    return new Date(value).toLocaleString(locale);
  } catch {
    return value;
  }
};

const compactText = (value?: string | null, maxLength = 120) => {
  if (!value) {
    return "-";
  }
  return value.length > maxLength ? `${value.slice(0, maxLength)}...` : value;
};

export function AdminAuditLogsPage() {
  const { t } = useI18n();
  const { preferences } = usePreferences();
  const [page, setPage] = useState(0);
  const [module, setModule] = useState("ALL");
  const [entityType, setEntityType] = useState("");
  const [action, setAction] = useState("");
  const [user, setUser] = useState("");
  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");
  const [selectedLog, setSelectedLog] = useState<Awaited<ReturnType<typeof adminAuditLogApi.list>>["items"][number] | null>(null);

  useEffect(() => {
    setPage(0);
  }, [module, entityType, action, user, from, to]);

  const auditLogsQuery = useQuery({
    queryKey: [
      "adminAuditLogs",
      page,
      module,
      entityType,
      action,
      user,
      from,
      to,
    ],
    queryFn: () =>
      adminAuditLogApi.list({
        page,
        size: PAGE_SIZE,
        module: module === "ALL" ? undefined : module,
        entityType: entityType || undefined,
        action: action || undefined,
        user: user || undefined,
        from: toApiDateTime(from),
        to: toApiDateTime(to),
      }),
  });

  const totalPages = auditLogsQuery.data?.totalPages ?? 0;

  return (
    <AdminPageContainer>
      <AdminHeaderCard
        title={t("admin.auditLogs.title")}
        description={t("admin.auditLogs.subtitle")}
      />

      <AdminContentCard>
        <CardContent className="space-y-4 p-4">
          <div className="flex flex-col gap-3 sm:flex-row sm:flex-wrap">
            <Select value={module} onValueChange={setModule}>
              <SelectTrigger className="h-9 w-full rounded-[14px] sm:w-[180px]">
                <SelectValue placeholder={t("admin.auditLogs.filters.module")} />
              </SelectTrigger>
              <SelectContent>
                {MODULE_OPTIONS.map((option) => (
                  <SelectItem key={option} value={option}>
                    {option}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>

            <Input
              className="h-9 w-full rounded-[14px] sm:w-[190px]"
              placeholder={t("admin.auditLogs.filters.entityType")}
              value={entityType}
              onChange={(event) => setEntityType(event.target.value)}
            />
            <Input
              className="h-9 w-full rounded-[14px] sm:w-[190px]"
              placeholder={t("admin.auditLogs.filters.action")}
              value={action}
              onChange={(event) => setAction(event.target.value)}
            />
            <Input
              className="h-9 w-full rounded-[14px] sm:w-[190px]"
              placeholder={t("admin.auditLogs.filters.user")}
              value={user}
              onChange={(event) => setUser(event.target.value)}
            />
            <Input
              className="h-9 w-full rounded-[14px] sm:w-[210px]"
              type="datetime-local"
              value={from}
              onChange={(event) => setFrom(event.target.value)}
            />
            <Input
              className="h-9 w-full rounded-[14px] sm:w-[210px]"
              type="datetime-local"
              value={to}
              onChange={(event) => setTo(event.target.value)}
            />
          </div>

          {auditLogsQuery.isLoading && (
            <div className="space-y-3">
              {[...Array(6)].map((_, index) => (
                <Skeleton key={index} className="h-10 w-full" />
              ))}
            </div>
          )}

          {auditLogsQuery.isError && (
            <Alert variant="destructive">
              <AlertCircle className="h-4 w-4" />
              <AlertTitle>{t("admin.auditLogs.error.load")}</AlertTitle>
              <AlertDescription>
                {auditLogsQuery.error?.message || t("common.error.description")}
              </AlertDescription>
            </Alert>
          )}

          {!auditLogsQuery.isLoading && !auditLogsQuery.isError && (
            <>
              {auditLogsQuery.data?.items?.length ? (
                <>
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead>{t("admin.auditLogs.table.time")}</TableHead>
                        <TableHead>{t("admin.auditLogs.table.user")}</TableHead>
                        <TableHead>{t("admin.auditLogs.table.module")}</TableHead>
                        <TableHead>{t("admin.auditLogs.table.action")}</TableHead>
                        <TableHead>{t("admin.auditLogs.table.entity")}</TableHead>
                        <TableHead>{t("admin.auditLogs.table.reason")}</TableHead>
                        <TableHead>{t("admin.auditLogs.table.snapshot")}</TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {auditLogsQuery.data.items.map((log) => (
                        <TableRow
                          key={log.id}
                          className="cursor-pointer hover:bg-muted/40"
                          onClick={() => setSelectedLog(log)}
                        >
                          <TableCell>
                            {formatDateTime(log.performedAt, preferences.locale)}
                          </TableCell>
                          <TableCell>{log.performedBy}</TableCell>
                          <TableCell>
                            <Badge variant="secondary">{log.module}</Badge>
                          </TableCell>
                          <TableCell>{log.operation}</TableCell>
                          <TableCell>
                            {log.entityType} #{log.entityId}
                          </TableCell>
                          <TableCell>{compactText(log.reason, 70)}</TableCell>
                          <TableCell className="max-w-[260px]">
                            <span className="text-xs text-muted-foreground">
                              {compactText(log.snapshotData)}
                            </span>
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                  <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
                    <span className="text-sm text-muted-foreground">
                      {t("pagination.page")} {page + 1} / {Math.max(totalPages, 1)}
                    </span>
                    <div className="flex items-center gap-2">
                      <Button
                        variant="outline"
                        size="sm"
                        disabled={page === 0}
                        onClick={() => setPage((prev) => Math.max(prev - 1, 0))}
                      >
                        {t("pagination.previousPage")}
                      </Button>
                      <Button
                        variant="outline"
                        size="sm"
                        disabled={page >= totalPages - 1 || totalPages === 0}
                        onClick={() => setPage((prev) => prev + 1)}
                      >
                        {t("pagination.nextPage")}
                      </Button>
                    </div>
                  </div>
                </>
              ) : (
                <div className="py-10 text-center text-sm text-muted-foreground">
                  {t("admin.auditLogs.empty")}
                </div>
              )}
            </>
          )}
        </CardContent>
      </AdminContentCard>

      <Dialog open={!!selectedLog} onOpenChange={(open) => !open && setSelectedLog(null)}>
        <DialogContent className="sm:max-w-2xl">
          <DialogHeader>
            <BackButton onClick={() => setSelectedLog(null)} className="w-fit" />
            <DialogTitle>{selectedLog?.operation ?? t("admin.auditLogs.detail.title", "Audit log detail")}</DialogTitle>
            <DialogDescription>
              {selectedLog ? `${selectedLog.module} / ${selectedLog.entityType} #${selectedLog.entityId}` : ""}
            </DialogDescription>
          </DialogHeader>

          {selectedLog && (
            <div className="space-y-4 text-sm">
              <div className="grid gap-3 sm:grid-cols-2">
                <div>
                  <p className="text-xs text-muted-foreground">{t("admin.auditLogs.table.time")}</p>
                  <p className="font-medium">{formatDateTime(selectedLog.performedAt, preferences.locale)}</p>
                </div>
                <div>
                  <p className="text-xs text-muted-foreground">{t("admin.auditLogs.table.user")}</p>
                  <p className="font-medium">{selectedLog.performedBy}</p>
                </div>
                <div>
                  <p className="text-xs text-muted-foreground">{t("admin.auditLogs.table.module")}</p>
                  <p className="font-medium">{selectedLog.module}</p>
                </div>
                <div>
                  <p className="text-xs text-muted-foreground">{t("admin.auditLogs.table.action")}</p>
                  <p className="font-medium">{selectedLog.operation}</p>
                </div>
                <div>
                  <p className="text-xs text-muted-foreground">{t("admin.auditLogs.table.entity")}</p>
                  <p className="font-medium">{selectedLog.entityType} #{selectedLog.entityId}</p>
                </div>
                <div>
                  <p className="text-xs text-muted-foreground">IP</p>
                  <p className="font-medium">{selectedLog.ipAddress || "-"}</p>
                </div>
              </div>
              <div>
                <p className="text-xs text-muted-foreground">{t("admin.auditLogs.table.reason")}</p>
                <p className="mt-1 whitespace-pre-wrap">{selectedLog.reason || "-"}</p>
              </div>
              <div>
                <p className="text-xs text-muted-foreground">{t("admin.auditLogs.table.snapshot")}</p>
                <pre className="mt-1 max-h-[260px] overflow-auto rounded-lg border border-border bg-muted/30 p-3 text-xs whitespace-pre-wrap">
                  {selectedLog.snapshotData || "-"}
                </pre>
              </div>
            </div>
          )}
        </DialogContent>
      </Dialog>
    </AdminPageContainer>
  );
}
