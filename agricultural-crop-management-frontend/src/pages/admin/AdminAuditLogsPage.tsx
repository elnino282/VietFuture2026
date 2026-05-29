import {
  Alert,
  AlertDescription,
  AlertTitle,
  Badge,
  Button,
  CardContent,
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
  const { preferences } = usePreferences();
  const [page, setPage] = useState(0);
  const [module, setModule] = useState("ALL");
  const [entityType, setEntityType] = useState("");
  const [action, setAction] = useState("");
  const [user, setUser] = useState("");
  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");

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
        title="Audit Logs"
        description="View and filter sensitive data change trails."
      />

      <AdminContentCard>
        <CardContent className="space-y-4 p-4">
          <div className="flex flex-col gap-3 sm:flex-row sm:flex-wrap">
            <Select value={module} onValueChange={setModule}>
              <SelectTrigger className="h-9 w-full rounded-[14px] sm:w-[180px]">
                <SelectValue placeholder="Module" />
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
              placeholder="Entity type"
              value={entityType}
              onChange={(event) => setEntityType(event.target.value)}
            />
            <Input
              className="h-9 w-full rounded-[14px] sm:w-[190px]"
              placeholder="Action"
              value={action}
              onChange={(event) => setAction(event.target.value)}
            />
            <Input
              className="h-9 w-full rounded-[14px] sm:w-[190px]"
              placeholder="User"
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
              <AlertTitle>Failed to load audit logs</AlertTitle>
              <AlertDescription>
                {auditLogsQuery.error?.message || "Please retry."}
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
                        <TableHead>Time</TableHead>
                        <TableHead>User</TableHead>
                        <TableHead>Module</TableHead>
                        <TableHead>Action</TableHead>
                        <TableHead>Entity</TableHead>
                        <TableHead>Reason</TableHead>
                        <TableHead>Snapshot</TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {auditLogsQuery.data.items.map((log) => (
                        <TableRow key={log.id}>
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
                      Page {page + 1} / {Math.max(totalPages, 1)}
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
                        disabled={page >= totalPages - 1 || totalPages === 0}
                        onClick={() => setPage((prev) => prev + 1)}
                      >
                        Next
                      </Button>
                    </div>
                  </div>
                </>
              ) : (
                <div className="py-10 text-center text-sm text-muted-foreground">
                  No audit logs found for current filters.
                </div>
              )}
            </>
          )}
        </CardContent>
      </AdminContentCard>
    </AdminPageContainer>
  );
}
