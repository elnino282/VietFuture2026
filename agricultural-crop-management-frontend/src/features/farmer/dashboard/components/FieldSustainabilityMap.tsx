import { Card, CardContent, CardHeader, CardTitle } from '@/shared/ui/card';
import { Skeleton } from '@/shared/ui/skeleton';
import type { DashboardFieldMapItem, DashboardFieldMapResponse, FdnAlertLevel } from '@/entities/dashboard';
import { FDN_LEVEL_COLORS } from '@/entities/dashboard';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/shared/ui/select';
import { useEffect, useMemo, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';

interface FieldSustainabilityMapProps {
  mapData: DashboardFieldMapResponse | null;
  isLoading: boolean;
  apiErrorMessage?: string | null;
}

const GOOGLE_MAPS_SCRIPT_ID = 'google-maps-js';
const GOOGLE_MAPS_SCRIPT_LOAD_ERROR = 'GOOGLE_MAPS_SCRIPT_LOAD_ERROR';
let googleMapsPromise: Promise<void> | null = null;

function ensureGoogleMaps(apiKey: string): Promise<void> {
  if ((window as Window & { google?: unknown }).google) {
    return Promise.resolve();
  }
  if (googleMapsPromise) {
    return googleMapsPromise;
  }

  googleMapsPromise = new Promise((resolve, reject) => {
    const existingScript = document.getElementById(GOOGLE_MAPS_SCRIPT_ID);
    if (existingScript) {
      existingScript.addEventListener('load', () => resolve(), { once: true });
      existingScript.addEventListener(
        'error',
        () => reject(new Error(GOOGLE_MAPS_SCRIPT_LOAD_ERROR)),
        { once: true }
      );
      return;
    }

    const script = document.createElement('script');
    script.id = GOOGLE_MAPS_SCRIPT_ID;
    script.src = `https://maps.googleapis.com/maps/api/js?key=${apiKey}`;
    script.async = true;
    script.defer = true;
    script.onload = () => resolve();
    script.onerror = () => reject(new Error(GOOGLE_MAPS_SCRIPT_LOAD_ERROR));
    document.head.appendChild(script);
  });

  return googleMapsPromise;
}

function colorByLevel(level: FdnAlertLevel): string {
  return FDN_LEVEL_COLORS[level];
}

function formatPercent(value: number | null): string {
  if (value === null || !Number.isFinite(value)) {
    return '--';
  }
  return `${value.toFixed(2)}%`;
}

function formatInput(value: number | null): string {
  if (value === null || !Number.isFinite(value)) {
    return '--';
  }
  return value.toFixed(2);
}

function setBoundsByDataLayer(googleMaps: any, dataLayer: any, map: any) {
  const bounds = new googleMaps.maps.LatLngBounds();
  const extendGeometry = (geometry: any) => {
    if (!geometry) {
      return;
    }
    const type = geometry.getType();
    if (type === 'Point') {
      bounds.extend(geometry.get());
      return;
    }
    geometry.getArray().forEach((item: any) => extendGeometry(item));
  };

  dataLayer.forEach((feature: any) => {
    extendGeometry(feature.getGeometry());
  });

  if (!bounds.isEmpty()) {
    map.fitBounds(bounds, 48);
  }
}

function extractFirstLngLat(coordinates: unknown): { lng: number; lat: number } | null {
  if (!Array.isArray(coordinates)) {
    return null;
  }
  if (
    coordinates.length >= 2 &&
    typeof coordinates[0] === 'number' &&
    typeof coordinates[1] === 'number' &&
    Number.isFinite(coordinates[0]) &&
    Number.isFinite(coordinates[1])
  ) {
    return {
      lng: coordinates[0],
      lat: coordinates[1],
    };
  }
  for (const child of coordinates) {
    const nested = extractFirstLngLat(child);
    if (nested) {
      return nested;
    }
  }
  return null;
}

function deriveCenterFromBoundary(item: DashboardFieldMapItem): { lat: number; lng: number } | null {
  if (item.center && Number.isFinite(item.center.lat) && Number.isFinite(item.center.lng)) {
    return item.center;
  }
  if (!item.boundaryGeoJson) {
    return null;
  }
  const first = extractFirstLngLat(item.boundaryGeoJson.coordinates);
  if (!first) {
    return null;
  }
  return {
    lat: first.lat,
    lng: first.lng,
  };
}

function toUnavailableReasonMessage(
  reason: string | null | undefined,
  t: (key: string, options?: Record<string, unknown>) => string
): string | null {
  if (!reason) {
    return null;
  }
  if (reason === 'MISSING_BOUNDARY_AND_FARM_LOCATION') {
    return t('dashboard.fdn.map.unavailableNoBoundaryNoFarmLocation');
  }
  if (reason === 'NO_FIELDS_FOR_FILTERS') {
    return t('dashboard.fdn.map.noFieldMatch');
  }
  return reason;
}

function matchesFilters(
  item: DashboardFieldMapItem,
  selectedFarm: string,
  selectedCrop: string,
  selectedLevel: string
): boolean {
  if (selectedFarm !== 'all' && String(item.farmId ?? '') !== selectedFarm) {
    return false;
  }
  if (selectedCrop !== 'all' && item.cropName !== selectedCrop) {
    return false;
  }
  if (selectedLevel !== 'all' && item.fdnLevel !== selectedLevel) {
    return false;
  }
  return true;
}

function pickInitialCenter(
  boundaryItems: DashboardFieldMapItem[],
  viewportCenter: { lat: number; lng: number } | null
): { lat: number; lng: number } | null {
  if (viewportCenter && Number.isFinite(viewportCenter.lat) && Number.isFinite(viewportCenter.lng)) {
    return viewportCenter;
  }
  for (const item of boundaryItems) {
    const center = deriveCenterFromBoundary(item);
    if (center) {
      return center;
    }
  }
  return null;
}

function formatFdnLevel(level: FdnAlertLevel, t: (key: string) => string): string {
  return t(`dashboard.fdn.map.${level}`);
}

export function FieldSustainabilityMap({
  mapData,
  isLoading,
  apiErrorMessage = null,
}: FieldSustainabilityMapProps) {
  const { t } = useTranslation();
  const mapContainerRef = useRef<HTMLDivElement | null>(null);
  const mapRef = useRef<any>(null);
  const dataLayerRef = useRef<any>(null);

  const [googleError, setGoogleError] = useState<string | null>(null);
  const [selectedFarm, setSelectedFarm] = useState<string>('all');
  const [selectedCrop, setSelectedCrop] = useState<string>('all');
  const [selectedLevel, setSelectedLevel] = useState<string>('all');
  const [selectedFieldId, setSelectedFieldId] = useState<number | null>(null);

  const fieldsWithBoundary = mapData?.fieldsWithBoundary ?? [];
  const fieldsMissingBoundary = mapData?.fieldsMissingBoundary ?? [];

  const allItems = useMemo(
    () => [...fieldsWithBoundary, ...fieldsMissingBoundary],
    [fieldsWithBoundary, fieldsMissingBoundary]
  );

  const farmOptions = useMemo(() => {
    const optionMap = new Map<string, string>();
    allItems.forEach((item) => {
      if (item.farmId !== null) {
        optionMap.set(
          String(item.farmId),
          item.farmName ??
            t('dashboard.fdn.map.farmFallback', { id: item.farmId })
        );
      }
    });
    return [...optionMap.entries()].map(([value, label]) => ({ value, label }));
  }, [allItems, t]);

  const cropOptions = useMemo(() => {
    const values = new Set<string>();
    allItems.forEach((item) => {
      if (item.cropName) {
        values.add(item.cropName);
      }
    });
    return [...values];
  }, [allItems]);

  const filteredItems = useMemo(() => {
    return allItems.filter((item) => matchesFilters(item, selectedFarm, selectedCrop, selectedLevel));
  }, [allItems, selectedFarm, selectedCrop, selectedLevel]);

  const filteredBoundaryItems = useMemo(() => {
    return fieldsWithBoundary.filter((item) =>
      matchesFilters(item, selectedFarm, selectedCrop, selectedLevel)
    );
  }, [fieldsWithBoundary, selectedFarm, selectedCrop, selectedLevel]);

  const filteredMissingBoundaryItems = useMemo(() => {
    return fieldsMissingBoundary.filter((item) =>
      matchesFilters(item, selectedFarm, selectedCrop, selectedLevel)
    );
  }, [fieldsMissingBoundary, selectedFarm, selectedCrop, selectedLevel]);

  useEffect(() => {
    if (!filteredItems.length) {
      setSelectedFieldId(null);
      return;
    }
    if (!filteredItems.some((item) => item.fieldId === selectedFieldId)) {
      setSelectedFieldId(filteredItems[0].fieldId);
    }
  }, [filteredItems, selectedFieldId]);

  useEffect(() => {
    if (filteredBoundaryItems.length === 0) {
      setGoogleError(null);
      return;
    }
    if (!mapContainerRef.current) {
      return;
    }

    const apiKey = import.meta.env.VITE_GOOGLE_MAPS_API_KEY;
    if (!apiKey) {
      setGoogleError(t('dashboard.fdn.map.missingKey'));
      return;
    }

    const viewportCenter = mapData?.defaultViewport?.center ?? null;
    const initialCenter = pickInitialCenter(filteredBoundaryItems, viewportCenter);
    if (!initialCenter) {
      setGoogleError(t('dashboard.fdn.map.noViewport'));
      return;
    }

    ensureGoogleMaps(apiKey)
      .then(() => {
        const googleMaps = (window as Window & { google?: any }).google;
        if (!googleMaps || !mapContainerRef.current) {
          setGoogleError(t('dashboard.fdn.map.unavailable'));
          return;
        }

        const viewportZoom = mapData?.defaultViewport?.zoom;
        const initialZoom = Number.isFinite(viewportZoom) ? viewportZoom : 14;
        if (!mapRef.current) {
          mapRef.current = new googleMaps.maps.Map(mapContainerRef.current, {
            center: initialCenter,
            zoom: initialZoom,
            mapTypeControl: false,
            fullscreenControl: false,
            streetViewControl: false,
          });

          dataLayerRef.current = new googleMaps.maps.Data({ map: mapRef.current });
          dataLayerRef.current.setStyle((feature: any) => {
            const level = feature.getProperty('fdnLevel') as FdnAlertLevel;
            return {
              fillColor: colorByLevel(level),
              fillOpacity: 0.35,
              strokeColor: colorByLevel(level),
              strokeWeight: 2,
            };
          });

          dataLayerRef.current.addListener('click', (event: any) => {
            const fieldId = Number(event.feature.getProperty('fieldId'));
            if (!Number.isNaN(fieldId)) {
              setSelectedFieldId(fieldId);
            }
          });
        } else {
          mapRef.current.setCenter(initialCenter);
          if (Number.isFinite(viewportZoom)) {
            mapRef.current.setZoom(viewportZoom);
          }
        }

        setGoogleError(null);
      })
      .catch((error: Error) => {
        setGoogleError(
          error.message === GOOGLE_MAPS_SCRIPT_LOAD_ERROR
            ? t('dashboard.fdn.map.scriptLoadError')
            : error.message
        );
      });
  }, [filteredBoundaryItems, mapData?.defaultViewport, t]);

  useEffect(() => {
    const googleMaps = (window as Window & { google?: any }).google;
    if (!googleMaps || !mapRef.current || !dataLayerRef.current) {
      return;
    }

    const dataLayer = dataLayerRef.current;
    const toRemove: any[] = [];
    dataLayer.forEach((feature: any) => {
      toRemove.push(feature);
    });
    toRemove.forEach((feature) => dataLayer.remove(feature));

    const features = filteredBoundaryItems
      .filter((item) => item.boundaryGeoJson)
      .map((item) => ({
        type: 'Feature' as const,
        geometry: item.boundaryGeoJson,
        properties: {
          fieldId: item.fieldId,
          fdnLevel: item.fdnLevel,
        },
      }));

    if (features.length > 0) {
      dataLayer.addGeoJson({
        type: 'FeatureCollection',
        features,
      });
      setBoundsByDataLayer(googleMaps, dataLayer, mapRef.current);
    }
  }, [filteredBoundaryItems]);

  const selectedItem = filteredItems.find((item) => item.fieldId === selectedFieldId) ?? null;
  const missingBoundaryCount = filteredMissingBoundaryItems.length;
  const hasBoundaryToRender = filteredBoundaryItems.length > 0;
  const hasAnyFilteredField = filteredItems.length > 0;
  const hasApiKey = Boolean(import.meta.env.VITE_GOOGLE_MAPS_API_KEY);
  const mapUnavailableReasonFromApi = toUnavailableReasonMessage(mapData?.unavailableReason, t);

  const mapUnavailableReason = apiErrorMessage
    ? apiErrorMessage
    : !hasApiKey
      ? t('dashboard.fdn.map.missingKey')
      : googleError
        ? googleError
        : !hasBoundaryToRender && missingBoundaryCount > 0
          ? t('dashboard.fdn.map.allMissingBoundary')
          : !hasAnyFilteredField
            ? t('dashboard.fdn.map.noFieldMatch')
            : !hasBoundaryToRender
              ? mapUnavailableReasonFromApi ??
                t('dashboard.fdn.map.noGeometryForFilters')
              : null;

  const shouldRenderMap = hasBoundaryToRender && !mapUnavailableReason;

  if (isLoading) {
    return (
      <Card className="border-border acm-card-elevated">
        <CardHeader>
          <CardTitle>{t('dashboard.fdn.map.title')}</CardTitle>
        </CardHeader>
        <CardContent>
          <Skeleton className="h-[420px] w-full" />
        </CardContent>
      </Card>
    );
  }

  return (
    <Card className="border-border acm-card-elevated acm-hover-surface">
      <CardHeader className="space-y-3">
        <CardTitle>{t('dashboard.fdn.map.title')}</CardTitle>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
          <Select value={selectedFarm} onValueChange={setSelectedFarm}>
            <SelectTrigger>
              <SelectValue placeholder={t('dashboard.fdn.map.filterFarm')} />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">{t('dashboard.fdn.map.allFarms')}</SelectItem>
              {farmOptions.map((farm) => (
                <SelectItem key={farm.value} value={farm.value}>
                  {farm.label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          <Select value={selectedCrop} onValueChange={setSelectedCrop}>
            <SelectTrigger>
              <SelectValue placeholder={t('dashboard.fdn.map.filterCrop')} />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">{t('dashboard.fdn.map.allCrops')}</SelectItem>
              {cropOptions.map((crop) => (
                <SelectItem key={crop} value={crop}>
                  {crop}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          <Select value={selectedLevel} onValueChange={setSelectedLevel}>
            <SelectTrigger>
              <SelectValue placeholder={t('dashboard.fdn.map.filterAlert')} />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">{t('dashboard.fdn.map.allLevels')}</SelectItem>
              <SelectItem value="low">{t('dashboard.fdn.map.low')}</SelectItem>
              <SelectItem value="medium">{t('dashboard.fdn.map.medium')}</SelectItem>
              <SelectItem value="high">{t('dashboard.fdn.map.high')}</SelectItem>
            </SelectContent>
          </Select>
        </div>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="flex items-center gap-6 text-base">
          <div className="flex items-center gap-2">
            <span className="h-3 w-3 rounded-full" style={{ background: colorByLevel('low') }} />
            <span>{t('dashboard.fdn.map.low')}</span>
          </div>
          <div className="flex items-center gap-2">
            <span className="h-3 w-3 rounded-full" style={{ background: colorByLevel('medium') }} />
            <span>{t('dashboard.fdn.map.medium')}</span>
          </div>
          <div className="flex items-center gap-2">
            <span className="h-3 w-3 rounded-full" style={{ background: colorByLevel('high') }} />
            <span>{t('dashboard.fdn.map.high')}</span>
          </div>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-[1.4fr_0.8fr] gap-4">
          <div className="rounded-lg border border-border overflow-hidden">
            {shouldRenderMap ? (
              <div ref={mapContainerRef} className="h-[420px] w-full" />
            ) : (
              <div className="h-[420px] p-4 bg-muted/20 flex flex-col gap-3 overflow-auto">
                <p className="text-base text-foreground font-medium">
                  {!hasApiKey
                    ? t('dashboard.fdn.map.configRequiredTitle')
                    : apiErrorMessage
                      ? t('dashboard.fdn.map.apiErrorTitle')
                      : missingBoundaryCount > 0
                        ? t('dashboard.fdn.map.missingBoundaryTitle')
                        : t('dashboard.fdn.map.fallbackTitle')}
                </p>
                {mapUnavailableReason && (
                  <p className="acm-body-text text-muted-foreground">{mapUnavailableReason}</p>
                )}
                <p className="acm-body-text text-muted-foreground">
                  {t('dashboard.fdn.map.fallbackHint')}
                </p>
                <Link
                  to="/farmer/farms"
                  className="inline-flex w-fit rounded-md border border-border px-3 py-2 acm-body-text acm-hover-surface hover:bg-muted"
                >
                  {t('dashboard.fdn.map.manageFieldsCta')}
                </Link>
                {missingBoundaryCount > 0 && (
                  <div className="rounded-md border border-border bg-background p-3 space-y-2">
                    <p className="acm-body-text font-medium">
                      {t('dashboard.fdn.map.missingBoundaryListTitle')}
                    </p>
                    <ul className="space-y-1 acm-body-text">
                      {filteredMissingBoundaryItems.map((item) => (
                        <li key={`missing-boundary-${item.fieldId}`} className="text-muted-foreground">
                          {item.fieldName} ({item.farmName ?? t('common.notAvailable')})
                        </li>
                      ))}
                    </ul>
                  </div>
                )}
                <div className="rounded-md border border-border bg-background p-3">
                  <p className="acm-body-text font-medium mb-2">
                    {t('dashboard.fdn.map.listFallbackTitle')}
                  </p>
                  {filteredItems.length === 0 ? (
                    <p className="acm-body-text text-muted-foreground">
                      {t('dashboard.fdn.map.noFieldMatch')}
                    </p>
                  ) : (
                    <ul className="space-y-2 acm-body-text">
                      {filteredItems.map((item) => (
                        <li key={item.fieldId} className="flex items-center justify-between gap-2">
                          <span className="text-foreground">{item.fieldName}</span>
                          <span className="text-muted-foreground">
                            {formatPercent(item.fdnTotal)} | {formatFdnLevel(item.fdnLevel, t)}
                          </span>
                        </li>
                      ))}
                    </ul>
                  )}
                </div>
              </div>
            )}
          </div>

          <div className="rounded-lg border border-border p-4 space-y-3 acm-hover-surface">
            {selectedItem ? (
              <>
                <div>
                  <p className="acm-body-text text-muted-foreground">
                    {t('dashboard.fdn.map.field')}
                  </p>
                  <p className="text-lg font-semibold">{selectedItem.fieldName}</p>
                  <p className="acm-body-text text-muted-foreground">
                    {selectedItem.farmName ?? t('common.notAvailable')} | {selectedItem.cropName} | {selectedItem.seasonName}
                  </p>
                  {selectedItem.boundaryIssue && (
                    <p className="acm-body-text text-amber-700">
                      {t('dashboard.fdn.map.boundaryIssue', {
                        issue: selectedItem.boundaryIssue,
                      })}
                    </p>
                  )}
                </div>
                <div className="grid grid-cols-2 gap-2 acm-body-text">
                  <div>{t('dashboard.fdn.map.fdnTotal')}</div>
                  <div className="font-medium">{formatPercent(selectedItem.fdnTotal)}</div>
                  <div>{t('dashboard.fdn.map.fdnMineral')}</div>
                  <div className="font-medium">{formatPercent(selectedItem.fdnMineral)}</div>
                  <div>{t('dashboard.fdn.map.fdnOrganic')}</div>
                  <div className="font-medium">{formatPercent(selectedItem.fdnOrganic)}</div>
                  <div>{t('dashboard.fdn.map.nue')}</div>
                  <div className="font-medium">{formatPercent(selectedItem.nue)}</div>
                  <div>{t('dashboard.fdn.map.confidence')}</div>
                  <div className="font-medium">
                    {selectedItem.confidence !== null && Number.isFinite(selectedItem.confidence)
                      ? `${Math.round(selectedItem.confidence * 100)}%`
                      : '--'}
                  </div>
                </div>
                <div className="space-y-1">
                  <p className="text-base font-medium">
                    {t('dashboard.fdn.map.inputBreakdown')}
                  </p>
                  <p className="acm-body-text text-muted-foreground">
                    {t('dashboard.fdn.map.input.mineral')} {formatInput(selectedItem.inputsBreakdown.mineralFertilizerN)} |{' '}
                    {t('dashboard.fdn.map.input.organic')} {formatInput(selectedItem.inputsBreakdown.organicFertilizerN)} |{' '}
                    {t('dashboard.fdn.map.input.irrigation')}{' '}
                    {formatInput(selectedItem.inputsBreakdown.irrigationWaterN)}
                  </p>
                </div>
                <div className="space-y-1">
                  <p className="text-base font-medium">
                    {t('dashboard.fdn.map.assistant')}
                  </p>
                  {selectedItem.recommendations.length === 0 ? (
                    <p className="acm-body-text text-muted-foreground">
                      {t('dashboard.fdn.map.noRecommendation')}
                    </p>
                  ) : (
                    <ul className="space-y-1">
                      {selectedItem.recommendations.slice(0, 4).map((item, idx) => (
                        <li key={idx} className="acm-body-text text-muted-foreground">
                          - {item}
                        </li>
                      ))}
                    </ul>
                  )}
                </div>
              </>
            ) : (
              <p className="acm-body-text text-muted-foreground">
                {t('dashboard.fdn.map.noFieldMatch')}
              </p>
            )}
          </div>
        </div>

        {missingBoundaryCount > 0 && (
          <p className="acm-body-text text-amber-700">
            {t('dashboard.fdn.map.missingGeometry', {
              count: missingBoundaryCount,
            })}
          </p>
        )}
      </CardContent>
    </Card>
  );
}
