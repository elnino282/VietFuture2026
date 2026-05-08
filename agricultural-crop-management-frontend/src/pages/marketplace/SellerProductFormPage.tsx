import { useEffect, useMemo, useState, type FormEvent } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import type { MarketplaceFarmerProductUpsertRequest } from "@/shared/api";
import {
  Button,
  Card,
  CardContent,
  CardHeader,
  CardTitle,
  Input,
  Label,
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
  Textarea,
} from "@/shared/ui";
import {
  useMarketplaceCreateFarmerProductMutation,
  useMarketplaceFarmerProductDetail,
  useMarketplaceFarmerProductFormOptions,
  useMarketplaceUpdateFarmerProductMutation,
  useMarketplaceUpdateFarmerProductStatusMutation,
} from "@/features/marketplace/hooks";
import { SellerMarketplaceTabs } from "@/features/marketplace/layout";
import { formatDate, formatVnd } from "@/features/marketplace/lib/format";
import {
  getNextSellerProductStatusAction,
  getNextSellerProductStatusLabel,
} from "@/features/marketplace/lib/sellerProductStatus";

type ProductFormState = {
  name: string;
  category: string;
  shortDescription: string;
  description: string;
  price: string;
  stockQuantity: string;
  imageUrl: string;
  selectedFarmId: string;
  selectedSeasonId: string;
  selectedLotId: string;
};

const EMPTY_FORM: ProductFormState = {
  name: "",
  category: "",
  shortDescription: "",
  description: "",
  price: "",
  stockQuantity: "",
  imageUrl: "",
  selectedFarmId: "",
  selectedSeasonId: "",
  selectedLotId: "",
};

export function SellerProductFormPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const isEdit = Boolean(id);
  const productId = Number(id ?? 0);
  const [form, setForm] = useState<ProductFormState>(EMPTY_FORM);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const formOptionsQuery = useMarketplaceFarmerProductFormOptions();
  const productQuery = useMarketplaceFarmerProductDetail(isEdit ? productId : undefined);
  const product = productQuery.data;

  const createMutation = useMarketplaceCreateFarmerProductMutation();
  const updateMutation = useMarketplaceUpdateFarmerProductMutation(productId);
  const statusMutation = useMarketplaceUpdateFarmerProductStatusMutation(productId);

  useEffect(() => {
    if (!isEdit) {
      setForm(EMPTY_FORM);
      return;
    }

    if (!product) {
      return;
    }

    setForm({
      name: product.name,
      category: product.category ?? "",
      shortDescription: product.shortDescription ?? "",
      description: product.description ?? "",
      price: String(product.price),
      stockQuantity: String(product.stockQuantity),
      imageUrl: product.imageUrl ?? "",
      selectedFarmId: product.farmId ? String(product.farmId) : "",
      selectedSeasonId: product.seasonId ? String(product.seasonId) : "",
      selectedLotId: product.lotId ? String(product.lotId) : "",
    });
  }, [isEdit, product]);

  const selectableLots = useMemo(
    () =>
      (formOptionsQuery.data?.lots ?? []).filter(
        (lot) => lot.linkedProductId == null || lot.linkedProductId === productId,
      ),
    [formOptionsQuery.data?.lots, productId],
  );

  const filteredSeasons = useMemo(() => {
    const seasons = formOptionsQuery.data?.seasons ?? [];
    if (!form.selectedFarmId) {
      return seasons;
    }
    return seasons.filter((season) => String(season.farmId ?? "") === form.selectedFarmId);
  }, [form.selectedFarmId, formOptionsQuery.data?.seasons]);

  const filteredLots = useMemo(() => {
    return selectableLots.filter((lot) => {
      const matchesFarm = !form.selectedFarmId || String(lot.farmId ?? "") === form.selectedFarmId;
      const matchesSeason =
        !form.selectedSeasonId || String(lot.seasonId ?? "") === form.selectedSeasonId;
      return matchesFarm && matchesSeason;
    });
  }, [form.selectedFarmId, form.selectedSeasonId, selectableLots]);

  const selectedLot = useMemo(
    () => selectableLots.find((lot) => String(lot.id) === form.selectedLotId),
    [form.selectedLotId, selectableLots],
  );

  const lotsAlreadyLinkedCount = useMemo(
    () =>
      (formOptionsQuery.data?.lots ?? []).filter(
        (lot) => lot.linkedProductId != null && lot.linkedProductId !== productId,
      ).length,
    [formOptionsQuery.data?.lots, productId],
  );

  const canSubmit =
    Boolean(form.name.trim()) &&
    Boolean(form.selectedLotId) &&
    Number(form.price) > 0 &&
    Number(form.stockQuantity) > 0 &&
    Boolean(selectedLot) &&
    Number(form.stockQuantity) <= Number(selectedLot?.availableQuantity ?? 0);

  const productModerationReason = product?.rejectionReason ?? product?.statusReason ?? null;

  function updateForm(patch: Partial<ProductFormState>) {
    setForm((current) => ({ ...current, ...patch }));
  }

  function handleFarmChange(value: string) {
    updateForm({
      selectedFarmId: value,
      selectedSeasonId: "",
      selectedLotId: "",
      stockQuantity: "",
    });
  }

  function handleSeasonChange(value: string) {
    updateForm({
      selectedSeasonId: value,
      selectedLotId: "",
      stockQuantity: "",
    });
  }

  function handleLotChange(value: string) {
    const lot = selectableLots.find((candidate) => String(candidate.id) === value);
    if (!lot) {
      updateForm({ selectedLotId: "", stockQuantity: "" });
      return;
    }

    updateForm({
      selectedFarmId: lot.farmId ? String(lot.farmId) : "",
      selectedSeasonId: lot.seasonId ? String(lot.seasonId) : "",
      selectedLotId: String(lot.id),
      stockQuantity:
        form.selectedLotId === String(lot.id) && form.stockQuantity
          ? form.stockQuantity
          : String(lot.availableQuantity),
    });
  }

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setErrorMessage(null);

    if (!selectedLot) {
      setErrorMessage("Choose a harvested lot before saving the listing.");
      return;
    }

    const price = Number(form.price);
    const stockQuantity = Number(form.stockQuantity);

    if (!Number.isFinite(price) || price <= 0) {
      setErrorMessage("Price must be greater than 0.");
      return;
    }

    if (!Number.isFinite(stockQuantity) || stockQuantity <= 0) {
      setErrorMessage("Quantity to sell must be greater than 0.");
      return;
    }

    if (stockQuantity > selectedLot.availableQuantity) {
      setErrorMessage(
        `Quantity to sell cannot exceed the harvested lot availability of ${selectedLot.availableQuantity} ${selectedLot.unit ?? ""}.`.trim(),
      );
      return;
    }

    try {
      const payload: MarketplaceFarmerProductUpsertRequest = {
        name: form.name.trim(),
        category: form.category.trim() || undefined,
        shortDescription: form.shortDescription.trim() || undefined,
        description: form.description.trim() || undefined,
        price,
        stockQuantity,
        imageUrl: form.imageUrl.trim() || undefined,
        lotId: Number(form.selectedLotId),
      };

      if (isEdit) {
        await updateMutation.mutateAsync(payload);
      } else {
        await createMutation.mutateAsync(payload);
      }

      navigate("/farmer/marketplace-products");
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "Failed to save product.");
    }
  }

  async function handleStatusTransition() {
    if (!product) {
      return;
    }

    const request = getNextSellerProductStatusAction(product.status);
    if (!request) {
      return;
    }

    setErrorMessage(null);
    try {
      await statusMutation.mutateAsync(request);
      navigate("/farmer/marketplace-products");
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "Failed to update product status.");
    }
  }

  const isLoading = formOptionsQuery.isLoading || (isEdit && productQuery.isLoading);
  const isError = formOptionsQuery.isError || (isEdit && productQuery.isError);
  const hasNoLots = !formOptionsQuery.isLoading && !formOptionsQuery.isError && selectableLots.length === 0;

  if (isLoading) {
    return (
      <div className="max-w-[1800px] mx-auto px-6 pt-6 space-y-6">
        <SellerMarketplaceTabs />
        <Card className="border-dashed">
          <CardContent className="p-8 text-sm text-muted-foreground">
            Loading harvest-based seller workflow...
          </CardContent>
        </Card>
      </div>
    );
  }

  if (isError) {
    return (
      <div className="max-w-[1800px] mx-auto px-6 pt-6 space-y-6">
        <SellerMarketplaceTabs />
        <Card className="border-destructive/30">
          <CardContent className="space-y-4 p-8">
            <p className="text-sm text-destructive">
              {isEdit
                ? "Failed to load the product detail or harvest options."
                : "Failed to load your harvest-based selling options."}
            </p>
            <div className="flex flex-wrap gap-3">
              <Button
                type="button"
                variant="outline"
                onClick={() => void Promise.all([formOptionsQuery.refetch(), productQuery.refetch()])}
              >
                Try again
              </Button>
              <Button type="button" variant="outline" onClick={() => navigate("/farmer/marketplace-products")}>
                Back to products
              </Button>
            </div>
          </CardContent>
        </Card>
      </div>
    );
  }

  if (isEdit && !product) {
    return (
      <div className="max-w-[1800px] mx-auto px-6 pt-6 space-y-6">
        <SellerMarketplaceTabs />
        <Card>
          <CardContent className="space-y-4 p-8">
            <p className="text-sm text-muted-foreground">This product could not be found for your account.</p>
            <Button type="button" variant="outline" onClick={() => navigate("/farmer/marketplace-products")}>
              Back to products
            </Button>
          </CardContent>
        </Card>
      </div>
    );
  }

  if (!isEdit && hasNoLots) {
    return (
      <div className="max-w-[1800px] mx-auto px-6 pt-6 space-y-6">
        <SellerMarketplaceTabs />
        <Card>
          <CardContent className="space-y-4 p-8">
            <p className="text-sm text-foreground">
              You do not have any harvested lots with remaining quantity ready to sell yet.
            </p>
            <p className="text-sm text-muted-foreground">
              Finish harvest intake first, then come back here to turn a harvested lot into a marketplace listing.
            </p>
            <Button type="button" variant="outline" onClick={() => navigate("/farmer/marketplace-products")}>
              Back to products
            </Button>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="max-w-[1800px] mx-auto px-6 pt-6 space-y-6">
      <SellerMarketplaceTabs />

      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <p className="text-sm font-medium text-primary">FarmTrace Seller Portal</p>
          <h1 className="mt-1 text-3xl font-bold text-foreground">
            {isEdit ? "Edit marketplace listing" : "Create marketplace listing"}
          </h1>
          <p className="mt-2 max-w-2xl text-sm text-muted-foreground">
            Keep the current harvest-backed workflow, but present it with the simpler legacy seller form layout.
          </p>
        </div>
        <Link
          to="/farmer/marketplace-products"
          className="rounded-full border border-border px-4 py-2 text-sm font-medium text-foreground transition-colors hover:bg-muted"
        >
          Back to products
        </Link>
      </div>

      <form
        className="grid gap-6 xl:grid-cols-[minmax(0,1.4fr)_360px]"
        onSubmit={onSubmit}
      >
        <div className="space-y-6">
          <Card className="border-border shadow-sm">
            <CardHeader>
              <CardTitle>Basic information</CardTitle>
            </CardHeader>
            <CardContent className="grid gap-4 md:grid-cols-2">
              <div className="space-y-2 md:col-span-2">
                <Label htmlFor="product-name">Listing name *</Label>
                <Input
                  id="product-name"
                  value={form.name}
                  onChange={(event) => updateForm({ name: event.target.value })}
                  placeholder="Example: Premium jasmine rice from harvest lot 2026"
                  required
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="product-category">Category</Label>
                <Input
                  id="product-category"
                  value={form.category}
                  onChange={(event) => updateForm({ category: event.target.value })}
                  placeholder="Grain, vegetable, fruit..."
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="product-price">Price *</Label>
                <Input
                  id="product-price"
                  type="number"
                  min="0"
                  step="1000"
                  value={form.price}
                  onChange={(event) => updateForm({ price: event.target.value })}
                  required
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="product-stock">
                  Quantity to sell *{selectedLot?.unit ? ` (${selectedLot.unit})` : ""}
                </Label>
                <Input
                  id="product-stock"
                  type="number"
                  min="0"
                  max={selectedLot?.availableQuantity}
                  step="0.001"
                  value={form.stockQuantity}
                  onChange={(event) => updateForm({ stockQuantity: event.target.value })}
                  required
                />
                {selectedLot ? (
                  <p className="text-xs text-muted-foreground">
                    Max allowed: {selectedLot.availableQuantity} {selectedLot.unit ?? ""}
                  </p>
                ) : null}
              </div>

              <div className="space-y-2 md:col-span-2">
                <Label htmlFor="product-image-url">Main image URL</Label>
                <Input
                  id="product-image-url"
                  value={form.imageUrl}
                  onChange={(event) => updateForm({ imageUrl: event.target.value })}
                  placeholder="https://..."
                />
              </div>
            </CardContent>
          </Card>

          <Card className="border-border shadow-sm">
            <CardHeader>
              <CardTitle>Descriptions</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="product-short-desc">Short description</Label>
                <Input
                  id="product-short-desc"
                  value={form.shortDescription}
                  onChange={(event) => updateForm({ shortDescription: event.target.value })}
                  placeholder="A short summary buyers can scan quickly"
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="product-description">Full description</Label>
                <Textarea
                  id="product-description"
                  value={form.description}
                  onChange={(event) => updateForm({ description: event.target.value })}
                  placeholder="Add packaging notes, harvest quality, shipping notes, or handling instructions"
                  className="min-h-36"
                />
              </div>
            </CardContent>
          </Card>
        </div>

        <div className="space-y-6">
          <Card className="border-border shadow-sm">
            <CardHeader>
              <CardTitle>Harvest source</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="space-y-2">
                <Label id="product-farm-label">Farm</Label>
                <Select value={form.selectedFarmId} onValueChange={handleFarmChange}>
                  <SelectTrigger aria-labelledby="product-farm-label">
                    <SelectValue placeholder="Choose your farm" />
                  </SelectTrigger>
                  <SelectContent>
                    {formOptionsQuery.data?.farms.map((farm) => (
                      <SelectItem key={farm.id} value={String(farm.id)}>
                        {farm.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              <div className="space-y-2">
                <Label id="product-season-label">Season</Label>
                <Select
                  value={form.selectedSeasonId}
                  onValueChange={handleSeasonChange}
                  disabled={!form.selectedFarmId}
                >
                  <SelectTrigger aria-labelledby="product-season-label">
                    <SelectValue placeholder={form.selectedFarmId ? "Choose a season" : "Pick a farm first"} />
                  </SelectTrigger>
                  <SelectContent>
                    {filteredSeasons.map((season) => (
                      <SelectItem key={season.id} value={String(season.id)}>
                        {season.seasonName}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              <div className="space-y-2">
                <Label id="product-lot-label">Harvested lot</Label>
                <Select
                  value={form.selectedLotId}
                  onValueChange={handleLotChange}
                  disabled={!form.selectedFarmId}
                >
                  <SelectTrigger aria-labelledby="product-lot-label">
                    <SelectValue placeholder={form.selectedFarmId ? "Choose a harvested lot" : "Pick a farm first"} />
                  </SelectTrigger>
                  <SelectContent>
                    {filteredLots.map((lot) => (
                      <SelectItem key={lot.id} value={String(lot.id)}>
                        {lot.lotCode} - {lot.productName ?? "Harvested lot"}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              <div className="rounded-xl border border-emerald-100 bg-emerald-50 p-4 text-sm">
                {selectedLot ? (
                  <div className="space-y-3">
                    <div className="flex items-center justify-between gap-3">
                      <span className="text-muted-foreground">Lot code</span>
                      <span className="font-medium text-foreground">{selectedLot.lotCode}</span>
                    </div>
                    <div className="flex items-center justify-between gap-3">
                      <span className="text-muted-foreground">Farm</span>
                      <span className="font-medium text-foreground">{selectedLot.farmName ?? "Unknown"}</span>
                    </div>
                    <div className="flex items-center justify-between gap-3">
                      <span className="text-muted-foreground">Season</span>
                      <span className="font-medium text-foreground">{selectedLot.seasonName ?? "Unknown"}</span>
                    </div>
                    <div className="flex items-center justify-between gap-3">
                      <span className="text-muted-foreground">Available quantity</span>
                      <span className="font-medium text-foreground">
                        {selectedLot.availableQuantity} {selectedLot.unit ?? ""}
                      </span>
                    </div>
                    <div className="flex items-center justify-between gap-3">
                      <span className="text-muted-foreground">Harvested</span>
                      <span className="font-medium text-foreground">
                        {selectedLot.harvestedAt ? formatDate(selectedLot.harvestedAt) : "N/A"}
                      </span>
                    </div>
                  </div>
                ) : (
                  <div className="space-y-1">
                    <p className="font-medium text-foreground">Select a harvested lot first.</p>
                    <p className="text-muted-foreground">
                      Farm, season, traceability, and quantity limits are driven by that lot.
                    </p>
                  </div>
                )}
              </div>

              {lotsAlreadyLinkedCount > 0 ? (
                <p className="text-xs text-muted-foreground">
                  {lotsAlreadyLinkedCount} harvested lot{lotsAlreadyLinkedCount > 1 ? "s are" : " is"} already linked to other marketplace listings and excluded here.
                </p>
              ) : null}
            </CardContent>
          </Card>

          <Card className="border-border shadow-sm">
            <CardHeader>
              <CardTitle>Listing preview</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="overflow-hidden rounded-xl border border-border bg-muted/50">
                {form.imageUrl ? (
                  <img
                    src={form.imageUrl}
                    alt={form.name || "Listing preview"}
                    className="h-48 w-full object-cover"
                    referrerPolicy="no-referrer"
                  />
                ) : (
                  <div className="flex h-48 items-center justify-center text-sm text-muted-foreground/60">
                    Product image preview
                  </div>
                )}
              </div>

              <div>
                <p className="text-lg font-semibold text-foreground">{form.name || "Untitled listing"}</p>
                <p className="mt-1 text-sm text-muted-foreground">{form.shortDescription || "Short description preview"}</p>
              </div>

              <div className="rounded-xl border border-border bg-muted/50 p-4 text-sm">
                <div className="flex items-center justify-between gap-3">
                  <span className="text-muted-foreground">Price</span>
                  <span className="font-semibold text-primary">
                    {Number(form.price) > 0 ? formatVnd(Number(form.price)) : "--"}
                    {selectedLot?.unit ? ` / ${selectedLot.unit}` : ""}
                  </span>
                </div>
                <div className="mt-3 flex items-center justify-between gap-3">
                  <span className="text-muted-foreground">Quantity to sell</span>
                  <span className="font-medium text-foreground">
                    {form.stockQuantity || "--"} {selectedLot?.unit ?? ""}
                  </span>
                </div>
                <div className="mt-3 flex items-center justify-between gap-3">
                  <span className="text-muted-foreground">Traceability</span>
                  <span className="font-medium text-primary">
                    {selectedLot ? "Enabled from harvest lot" : "Waiting for lot"}
                  </span>
                </div>
              </div>

              {productModerationReason ? (
                <div className="rounded-xl border border-destructive/30 bg-red-50 p-3 text-sm text-red-700">
                  Admin reason: {productModerationReason}
                </div>
              ) : null}

              {errorMessage ? <p className="text-sm text-destructive">{errorMessage}</p> : null}

              <div className="flex flex-col gap-3">
                <Button
                  type="submit"
                  disabled={!canSubmit || createMutation.isPending || updateMutation.isPending}
                  className="w-full"
                >
                  {createMutation.isPending || updateMutation.isPending
                    ? "Saving..."
                    : isEdit
                      ? "Save changes"
                      : "Create draft"}
                </Button>

                {isEdit && product && getNextSellerProductStatusAction(product.status) ? (
                  <Button
                    type="button"
                    variant="outline"
                    onClick={handleStatusTransition}
                    disabled={statusMutation.isPending}
                    className="w-full"
                  >
                    {statusMutation.isPending
                      ? "Updating..."
                      : getNextSellerProductStatusLabel(product.status)}
                  </Button>
                ) : null}
              </div>
            </CardContent>
          </Card>
        </div>
      </form>
    </div>
  );
}
