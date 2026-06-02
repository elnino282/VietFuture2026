import { useEffect, useMemo, useRef, useState, type ChangeEvent, type DragEvent } from "react";
import { Camera, ImageIcon, Loader2, Search, UploadCloud } from "lucide-react";
import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { useAuth } from "@/features/auth";
import {
  useSearchMarketplaceByImageMutation,
} from "@/features/marketplace/hooks";
import { formatVnd } from "@/features/marketplace/lib/format";
import { cn } from "@/shared/lib";
import {
  Badge,
  Button,
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  Input,
} from "@/shared/ui";
import type {
  MarketplaceImageSearchFilters,
  MarketplaceImageSearchResult,
} from "@/shared/api";
import {
  resizeMarketplaceImage,
  validateMarketplaceImageFile,
  type ImageSearchValidationError,
} from "./imageProcessing";

export type ImageSearchModalProps = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  filters?: MarketplaceImageSearchFilters;
  onKeywordSearch: (keyword: string) => void;
};

type ViewState = "idle" | "validating" | "analyzing" | "success" | "lowConfidence" | "empty" | "error";

export function ImageSearchModal({
  open,
  onOpenChange,
  filters,
  onKeywordSearch,
}: ImageSearchModalProps) {
  const { t } = useTranslation();
  const { isAuthenticated, user } = useAuth();
  const fileInputRef = useRef<HTMLInputElement | null>(null);
  const searchMutation = useSearchMarketplaceByImageMutation();
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [keyword, setKeyword] = useState("");
  const [result, setResult] = useState<MarketplaceImageSearchResult | null>(null);
  const [validationError, setValidationError] = useState<ImageSearchValidationError | null>(null);
  const [processing, setProcessing] = useState(false);
  const [dragActive, setDragActive] = useState(false);

  const canUseImageSearch = isAuthenticated && user?.role === "buyer";
  const products = result?.products.items ?? [];
  const analysis = result?.analysis ?? null;
  const suggestedKeyword = useMemo(() => getSuggestedKeyword(result), [result]);

  useEffect(() => {
    if (!open) {
      resetState();
    }
  }, [open]);

  useEffect(() => {
    setKeyword(suggestedKeyword);
  }, [suggestedKeyword]);

  useEffect(() => () => {
    if (previewUrl) {
      URL.revokeObjectURL(previewUrl);
    }
  }, [previewUrl]);

  const viewState: ViewState = processing
    ? "validating"
    : searchMutation.isPending
      ? "analyzing"
      : validationError || searchMutation.isError
        ? "error"
        : result && (!analysis?.agricultural || (analysis.confidence ?? 0) < 0.35)
          ? "lowConfidence"
          : result && products.length === 0
            ? "empty"
            : result
              ? "success"
              : "idle";

  function resetState() {
    searchMutation.reset();
    setResult(null);
    setValidationError(null);
    setKeyword("");
    setProcessing(false);
    setDragActive(false);
    setPreviewUrl((current) => {
      if (current) {
        URL.revokeObjectURL(current);
      }
      return null;
    });
  }

  async function handleFile(file: File | null | undefined) {
    if (!canUseImageSearch) {
      return;
    }

    searchMutation.reset();
    setResult(null);
    setValidationError(null);
    const fileError = validateMarketplaceImageFile(file);
    if (fileError) {
      setValidationError(fileError);
      return;
    }

    const originalFile = file as File;
    setPreviewUrl((current) => {
      if (current) {
        URL.revokeObjectURL(current);
      }
      return URL.createObjectURL(originalFile);
    });

    try {
      setProcessing(true);
      const resized = await resizeMarketplaceImage(originalFile);
      setProcessing(false);
      const response = await searchMutation.mutateAsync({
        file: resized,
        filters: {
          ...filters,
          page: 0,
          size: filters?.size ?? 8,
        },
      });
      setResult(response);
    } catch {
      setProcessing(false);
    }
  }

  function handleInputChange(event: ChangeEvent<HTMLInputElement>) {
    void handleFile(event.target.files?.[0]);
    event.target.value = "";
  }

  function handleDrop(event: DragEvent<HTMLDivElement>) {
    event.preventDefault();
    setDragActive(false);
    void handleFile(event.dataTransfer.files?.[0]);
  }

  function handleManualSearch() {
    const trimmed = keyword.trim();
    if (!trimmed) {
      return;
    }
    onKeywordSearch(trimmed);
    onOpenChange(false);
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="w-[calc(100vw-2rem)] max-w-[calc(100vw-2rem)] p-0 sm:max-w-[940px] lg:max-w-[1080px] xl:max-w-[1160px]">
        <div className="border-b border-border px-5 py-4 sm:px-6">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <Camera className="h-5 w-5 text-emerald-700" aria-hidden="true" />
              {t("marketplaceBuyer.imageSearch.title")}
            </DialogTitle>
            <DialogDescription>
              {t("marketplaceBuyer.imageSearch.description")}
            </DialogDescription>
          </DialogHeader>
        </div>

        {!isAuthenticated ? (
          <AuthPrompt
            title={t("marketplaceBuyer.imageSearch.authTitle")}
            message={t("marketplaceBuyer.imageSearch.authMessage")}
            primaryLabel={t("marketplaceBuyer.imageSearch.signIn")}
            secondaryLabel={t("marketplaceBuyer.imageSearch.createAccount")}
          />
        ) : user?.role !== "buyer" ? (
          <AuthPrompt
            title={t("marketplaceBuyer.imageSearch.buyerOnlyTitle")}
            message={t("marketplaceBuyer.imageSearch.buyerOnlyMessage")}
            primaryLabel={t("marketplaceBuyer.imageSearch.signIn")}
            showSignInOnly
          />
        ) : (
          <div className="grid gap-6 px-5 pb-5 sm:px-6 sm:pb-6 lg:grid-cols-[minmax(0,1.05fr)_minmax(420px,0.95fr)] xl:grid-cols-[minmax(0,1.08fr)_minmax(460px,0.92fr)]">
            <div className="min-w-0 space-y-4">
              <div
                className={cn(
                  "flex h-[260px] min-w-0 cursor-pointer flex-col items-center justify-center rounded-lg border border-dashed p-4 text-center transition sm:h-[320px] lg:h-[420px]",
                  dragActive
                    ? "border-emerald-500 bg-emerald-50"
                    : "border-border bg-muted/30 hover:border-emerald-400 hover:bg-emerald-50/40",
                )}
                onClick={() => fileInputRef.current?.click()}
                onDragEnter={(event) => {
                  event.preventDefault();
                  setDragActive(true);
                }}
                onDragOver={(event) => event.preventDefault()}
                onDragLeave={() => setDragActive(false)}
                onDrop={handleDrop}
                role="button"
                tabIndex={0}
              >
                {previewUrl ? (
                  <img
                    src={previewUrl}
                    alt={t("marketplaceBuyer.imageSearch.previewAlt")}
                    className="h-full w-full rounded-md object-contain"
                  />
                ) : (
                  <div className="max-w-xs">
                    <UploadCloud className="mx-auto mb-3 h-10 w-10 text-emerald-700" aria-hidden="true" />
                    <p className="font-semibold text-foreground">
                      {t("marketplaceBuyer.imageSearch.uploadTitle")}
                    </p>
                    <p className="mt-1 text-sm leading-5 text-muted-foreground">
                      {t("marketplaceBuyer.imageSearch.uploadHint")}
                    </p>
                  </div>
                )}
              </div>

              <input
                ref={fileInputRef}
                type="file"
                accept="image/jpeg,image/png,image/webp"
                className="hidden"
                onChange={handleInputChange}
              />

              <Button
                type="button"
                variant="outline"
                className="h-11 w-full"
                onClick={() => fileInputRef.current?.click()}
                disabled={processing || searchMutation.isPending}
              >
                <ImageIcon className="mr-2 h-4 w-4" aria-hidden="true" />
                {previewUrl
                  ? t("marketplaceBuyer.imageSearch.chooseAnother")
                  : t("marketplaceBuyer.imageSearch.chooseImage")}
              </Button>
            </div>

            <div className="min-w-0 space-y-4">
              <StatusBanner state={viewState} message={getStatusMessage(viewState, t, validationError, searchMutation.error)} />

              {analysis ? (
                <div className="space-y-3 rounded-lg border border-border p-4 lg:p-5">
                  <div className="flex flex-wrap items-center gap-2">
                    {analysis.detectedProduct ? (
                      <Badge className="bg-emerald-600 text-white">
                        {analysis.detectedProduct}
                      </Badge>
                    ) : null}
                    <Badge variant="outline">
                      {t(`marketplaceBuyer.imageSearch.confidence.${analysis.confidenceLabel}`, {
                        defaultValue: analysis.confidenceLabel,
                      })}
                      {" "}
                      {Math.round((analysis.confidence ?? 0) * 100)}%
                    </Badge>
                  </div>
                  {analysis.message ? (
                    <p className="text-sm leading-6 text-muted-foreground">{analysis.message}</p>
                  ) : null}
                  <KeywordChips keywords={result?.searchKeywords ?? []} />
                </div>
              ) : null}

              <div className="space-y-2">
                <label className="text-sm font-semibold text-foreground" htmlFor="marketplace-image-keyword">
                  {t("marketplaceBuyer.imageSearch.keywordLabel")}
                </label>
                <div className="grid grid-cols-[minmax(0,1fr)_auto] gap-2">
                  <Input
                    id="marketplace-image-keyword"
                    value={keyword}
                    onChange={(event) => setKeyword(event.target.value)}
                    placeholder={t("marketplaceBuyer.imageSearch.keywordPlaceholder")}
                    className="h-12"
                  />
                  <Button
                    type="button"
                    className="h-12 min-w-24 shrink-0 bg-emerald-600 hover:bg-emerald-700"
                    disabled={!keyword.trim()}
                    onClick={handleManualSearch}
                  >
                    <Search className="mr-2 h-4 w-4" aria-hidden="true" />
                    {t("marketplaceBuyer.imageSearch.searchText")}
                  </Button>
                </div>
              </div>

              {products.length > 0 ? (
                <div className="space-y-2">
                  <div className="flex items-center justify-between gap-2">
                    <h3 className="text-sm font-semibold text-foreground">
                      {t("marketplaceBuyer.imageSearch.resultsTitle")}
                    </h3>
                    <span className="text-xs text-muted-foreground">
                      {result?.products.totalElements ?? products.length}
                    </span>
                  </div>
                  <div className="max-h-72 space-y-2 overflow-y-auto pr-1">
                    {products.slice(0, 6).map((product) => (
                      <Link
                        key={product.id}
                        to={`/marketplace/products/${product.slug}`}
                        className="flex min-w-0 gap-3 rounded-lg border border-border p-2 transition hover:border-emerald-300 hover:bg-emerald-50/40"
                        onClick={() => onOpenChange(false)}
                      >
                        <div className="h-14 w-14 shrink-0 overflow-hidden rounded-md bg-muted">
                          {product.imageUrl ? (
                            <img src={product.imageUrl} alt={product.name} className="h-full w-full object-cover" />
                          ) : null}
                        </div>
                        <div className="min-w-0">
                          <p className="truncate text-sm font-semibold text-foreground">{product.name}</p>
                          <p className="truncate text-xs text-muted-foreground">{product.farmName ?? product.region ?? ""}</p>
                          <p className="mt-1 text-sm font-bold text-emerald-700">
                            {formatVnd(product.price)}
                          </p>
                        </div>
                      </Link>
                    ))}
                  </div>
                </div>
              ) : null}
            </div>
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
}

function AuthPrompt({
  title,
  message,
  primaryLabel,
  secondaryLabel,
  showSignInOnly = false,
}: {
  title: string;
  message: string;
  primaryLabel: string;
  secondaryLabel?: string;
  showSignInOnly?: boolean;
}) {
  return (
    <div className="px-5 pb-5">
      <div className="rounded-lg border border-border bg-muted/30 p-5">
        <h3 className="text-base font-semibold text-foreground">{title}</h3>
        <p className="mt-2 text-sm leading-6 text-muted-foreground">{message}</p>
        <div className="mt-4 flex flex-wrap gap-2">
          <Button asChild className="bg-emerald-600 hover:bg-emerald-700">
            <Link to="/sign-in">{primaryLabel}</Link>
          </Button>
          {!showSignInOnly && secondaryLabel ? (
            <Button asChild variant="outline">
              <Link to="/sign-up">{secondaryLabel}</Link>
            </Button>
          ) : null}
        </div>
      </div>
    </div>
  );
}

function StatusBanner({
  state,
  message,
}: {
  state: ViewState;
  message: string;
}) {
  const loading = state === "validating" || state === "analyzing";
  return (
    <div
      className={cn(
        "flex min-h-14 items-start gap-3 rounded-lg border px-3 py-3 text-sm leading-5",
        state === "error"
          ? "border-destructive/30 bg-destructive/10 text-destructive"
          : state === "success"
            ? "border-emerald-200 bg-emerald-50 text-emerald-950"
            : "border-border bg-muted/30 text-muted-foreground",
      )}
    >
      {loading ? (
        <Loader2 className="mt-0.5 h-4 w-4 shrink-0 animate-spin" aria-hidden="true" />
      ) : (
        <Camera className="mt-0.5 h-4 w-4 shrink-0" aria-hidden="true" />
      )}
      <span>{message}</span>
    </div>
  );
}

function KeywordChips({ keywords }: { keywords: string[] }) {
  if (keywords.length === 0) {
    return null;
  }
  return (
    <div className="flex flex-wrap gap-1.5">
      {keywords.slice(0, 8).map((keyword) => (
        <span
          key={keyword}
          className="max-w-full truncate rounded-full bg-emerald-50 px-2 py-1 text-xs font-medium text-emerald-900"
        >
          {keyword}
        </span>
      ))}
    </div>
  );
}

function getSuggestedKeyword(result: MarketplaceImageSearchResult | null): string {
  if (!result) {
    return "";
  }
  return (
    result.searchKeywords[0]
    ?? result.analysis.detectedProduct
    ?? result.analysis.keywordsVi[0]
    ?? result.analysis.keywords[0]
    ?? ""
  );
}

function getStatusMessage(
  state: ViewState,
  t: ReturnType<typeof useTranslation>["t"],
  validationError: ImageSearchValidationError | null,
  error: unknown,
) {
  if (validationError) {
    return t(`marketplaceBuyer.imageSearch.errors.${validationError}`);
  }
  if (state === "error") {
    const code = typeof error === "object" && error !== null && "code" in error
      ? String((error as { code?: string }).code)
      : "";
    if (code) {
      return t(`marketplaceBuyer.imageSearch.errors.${code}`, {
        defaultValue: t("marketplaceBuyer.imageSearch.errors.generic"),
      });
    }
    return t("marketplaceBuyer.imageSearch.errors.generic");
  }
  return t(`marketplaceBuyer.imageSearch.states.${state}`);
}
