import { useEffect, useMemo, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { Search } from "lucide-react";
import {
  Badge,
  BackButton,
  Button,
  Card,
  CardContent,
  Input,
  Skeleton,
  Tabs,
  TabsList,
  TabsTrigger,
} from "@/shared/ui";
import { cn, useDebounce } from "@/shared/lib";
import { useGlobalSearch, type SearchEntityType } from "@/entities/search";

type PortalKind = "admin" | "farmer";

const TYPE_LABELS: Record<SearchEntityType, string> = {
  FARM: "Farms",
  PLOT: "Plots",
  SEASON: "Seasons",
  TASK: "Tasks",
  EXPENSE: "Expenses",
  DOCUMENT: "Documents",
  USER: "Users",
};

const TYPE_ORDER: Record<PortalKind, SearchEntityType[]> = {
  admin: ["FARM", "PLOT", "SEASON", "DOCUMENT", "USER"],
  farmer: ["PLOT", "SEASON", "TASK", "EXPENSE", "DOCUMENT"],
};

type SearchResultsPageProps = {
  portal: PortalKind;
  variant?: "default" | "admin";
};

export function SearchResultsPage({
  portal,
  variant = "default",
}: SearchResultsPageProps) {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const initialQuery = searchParams.get("q") ?? "";
  const [query, setQuery] = useState(initialQuery);
  const isAdminVariant = variant === "admin";

  const debouncedQuery = useDebounce(query, 300);
  const normalizedQuery = debouncedQuery.trim();
  const searchEnabled = normalizedQuery.length >= 2;

  const allowedTypes = TYPE_ORDER[portal];
  const rawType = (searchParams.get("type") ?? "").toUpperCase();
  const activeType = (allowedTypes.includes(rawType as SearchEntityType)
    ? rawType
    : "ALL") as "ALL" | SearchEntityType;

  useEffect(() => {
    setQuery(initialQuery);
  }, [initialQuery]);

  useEffect(() => {
    setSearchParams(
      (prev) => {
        const next = new URLSearchParams(prev);
        if (normalizedQuery) {
          next.set("q", normalizedQuery);
        } else {
          next.delete("q");
        }
        return next;
      },
      { replace: true },
    );
  }, [normalizedQuery, setSearchParams]);

  const { data, isFetching, isError } = useGlobalSearch(
    { q: normalizedQuery, limit: 20 },
    { enabled: searchEnabled, staleTime: 10_000 },
  );

  const groupedCounts = useMemo(() => {
    const counts: Record<string, number> = {};
    const grouped = data?.grouped ?? {};
    for (const type of allowedTypes) {
      counts[type] = grouped[type] ?? 0;
    }
    return counts;
  }, [allowedTypes, data?.grouped]);

  const totalCount = useMemo(
    () =>
      allowedTypes.reduce((sum, type) => sum + (groupedCounts[type] ?? 0), 0),
    [allowedTypes, groupedCounts],
  );

  const filteredResults = useMemo(() => {
    const results = data?.results ?? [];
    if (activeType === "ALL") {
      return results;
    }
    return results.filter((item) => item.type === activeType);
  }, [activeType, data?.results]);

  const updateType = (value: string) => {
    setSearchParams(
      (prev) => {
        const next = new URLSearchParams(prev);
        if (value === "ALL") {
          next.delete("type");
        } else {
          next.set("type", value);
        }
        return next;
      },
      { replace: true },
    );
  };

  return (
    <div
      className={cn(
        "space-y-6",
        isAdminVariant
          ? "min-h-full p-4 sm:p-6"
          : "mx-auto max-w-[1400px] p-6",
      )}
    >
      {isAdminVariant ? (
        <>
          <BackButton to="/admin/dashboard" className="w-fit" />
          <Card className="rounded-[18px] border-border bg-card shadow-sm">
            <CardContent className="p-4 sm:p-5">
              <h1 className="text-2xl font-bold">Search Results</h1>
              <p className="text-muted-foreground">
                {normalizedQuery
                  ? `Showing results for "${normalizedQuery}"`
                  : "Type a keyword to search."}
              </p>
            </CardContent>
          </Card>
        </>
      ) : (
        <div>
          <h1 className="text-2xl font-bold">Search Results</h1>
          <p className="text-muted-foreground">
            {normalizedQuery
              ? `Showing results for "${normalizedQuery}"`
              : "Type a keyword to search."}
          </p>
        </div>
      )}

      <Card
        className={cn(
          isAdminVariant
            ? "overflow-hidden rounded-[18px] border-border bg-card shadow-sm"
            : "border-0 shadow-sm",
        )}
      >
        <CardContent className={cn("space-y-4", isAdminVariant && "p-4")}>
          <div className="flex flex-wrap items-center gap-3">
            <div className="relative w-full max-w-md">
              <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
              <Input
                placeholder="Search across the platform"
                className={cn("h-9 pl-9", isAdminVariant && "rounded-[14px]")}
                value={query}
                onChange={(event) => setQuery(event.target.value)}
              />
            </div>

            <Tabs
              value={activeType}
              onValueChange={updateType}
              className={cn(isAdminVariant && "max-w-full overflow-x-auto")}
            >
              <TabsList
                className={cn(
                  "h-9",
                  isAdminVariant && "h-10 w-max rounded-[18px] bg-muted p-1",
                )}
              >
                <TabsTrigger
                  value="ALL"
                  className={cn(
                    "text-xs",
                    isAdminVariant && "h-8 rounded-[14px] px-3",
                  )}
                >
                  All
                  <Badge variant="secondary" className="ml-2 text-[10px]">
                    {totalCount}
                  </Badge>
                </TabsTrigger>
                {allowedTypes.map((type) => (
                  <TabsTrigger
                    key={type}
                    value={type}
                    className={cn(
                      "text-xs",
                      isAdminVariant && "h-8 rounded-[14px] px-3",
                    )}
                  >
                    {TYPE_LABELS[type]}
                    <Badge variant="secondary" className="ml-2 text-[10px]">
                      {groupedCounts[type] ?? 0}
                    </Badge>
                  </TabsTrigger>
                ))}
              </TabsList>
            </Tabs>
          </div>

          {!searchEnabled && normalizedQuery.length > 0 && (
            <div className="text-sm text-muted-foreground">
              Type at least 2 characters to search.
            </div>
          )}

          {searchEnabled && isFetching && (
            <div className="space-y-3">
              {[...Array(4)].map((_, index) => (
                <Skeleton key={index} className="h-10 w-full" />
              ))}
            </div>
          )}

          {searchEnabled && isError && (
            <div
              className={cn(
                "border border-destructive/20 bg-destructive/10 p-3 text-sm text-destructive",
                isAdminVariant ? "rounded-[14px]" : "rounded-md",
              )}
            >
              Search failed. Please try again.
            </div>
          )}

          {searchEnabled && !isFetching && !isError && (
            <>
              {filteredResults.length === 0 ? (
                <div className="py-10 text-center text-sm text-muted-foreground">
                  No results match your search.
                </div>
              ) : (
                <div className="space-y-2">
                  {filteredResults.map((item) => (
                    <div
                      key={`${item.type}-${item.id}-${item.title}`}
                      className={cn(
                        "flex items-center justify-between border border-border bg-card px-4 py-3",
                        isAdminVariant ? "rounded-[14px]" : "rounded-lg",
                      )}
                    >
                      <div>
                        <div className="text-sm font-medium">{item.title}</div>
                        {item.subtitle && (
                          <div className="text-xs text-muted-foreground">
                            {item.subtitle}
                          </div>
                        )}
                        <div className="mt-1 text-[10px] uppercase text-muted-foreground">
                          {TYPE_LABELS[item.type] ?? item.type}
                        </div>
                      </div>
                      <Button
                        variant="outline"
                        size="sm"
                        className={cn(isAdminVariant && "rounded-[14px]")}
                        disabled={!item.route}
                        disabledHint="No route available"
                        onClick={() => item.route && navigate(item.route)}
                      >
                        Open
                      </Button>
                    </div>
                  ))}
                </div>
              )}
            </>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
