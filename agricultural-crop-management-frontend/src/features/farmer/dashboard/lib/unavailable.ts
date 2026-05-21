import type {
  DashboardFdnOverview,
  DashboardUnavailableReason,
} from '@/entities/dashboard';

type TranslateFn = (key: string, options?: Record<string, unknown>) => string;

export interface UnavailableActionLink {
  href: string;
  key: string;
  label: string;
}

function normalizeReason(reason: string): DashboardUnavailableReason {
  return reason.trim().toUpperCase() as DashboardUnavailableReason;
}

function seasonWorkspaceHref(
  seasonId: number | null,
  workspacePath: 'harvest' | 'nutrient-inputs' | 'irrigation-water-analyses' | 'soil-tests'
): string {
  if (typeof seasonId === 'number' && Number.isFinite(seasonId)) {
    return `/farmer/seasons/${seasonId}/workspace/${workspacePath}`;
  }
  return workspacePath === 'harvest' ? '/farmer/harvest' : '/farmer/seasons';
}

function pushAction(
  out: UnavailableActionLink[],
  seen: Set<string>,
  action: UnavailableActionLink
): void {
  if (!seen.has(action.key)) {
    seen.add(action.key);
    out.push(action);
  }
}

export function getUnavailableReasons(
  overview: DashboardFdnOverview | null
): DashboardUnavailableReason[] {
  if (!overview) {
    return [];
  }
  return (overview.unavailableReasons ?? []).map(normalizeReason);
}

export function hasUnavailableCoreMetrics(overview: DashboardFdnOverview | null): boolean {
  if (!overview) {
    return true;
  }
  return (
    overview.fdnTotalMetric.status === 'unavailable' ||
    overview.sustainableScoreMetric.status === 'unavailable'
  );
}

export function unavailableReasonLabel(
  reason: DashboardUnavailableReason,
  t: TranslateFn
): string {
  if (reason === 'NO_ACTIVE_SEASON') {
    return t('dashboard.fdn.unavailableReason.NO_ACTIVE_SEASON', {
      defaultValue: 'No active season is available for this dashboard context.',
    });
  }
  if (reason === 'NO_HARVEST') {
    return t('dashboard.fdn.unavailableReason.NO_HARVEST', {
      defaultValue: 'No harvest record is available yet for the selected season.',
    });
  }
  if (reason === 'MISSING_NITROGEN_INPUT') {
    return t('dashboard.fdn.unavailableReason.MISSING_NITROGEN_INPUT', {
      defaultValue: 'Nitrogen input records are missing (mineral/organic fertilizer).',
    });
  }
  if (reason === 'MISSING_PLOT_AREA') {
    return t('dashboard.fdn.unavailableReason.MISSING_PLOT_AREA', {
      defaultValue: 'Plot area is missing, so per-hectare indicators cannot be computed.',
    });
  }
  if (reason === 'INSUFFICIENT_HISTORY') {
    return t('dashboard.fdn.unavailableReason.INSUFFICIENT_HISTORY', {
      defaultValue: 'There is not enough season history to build a trend chart yet.',
    });
  }
  return reason;
}

export function buildUnavailableActionLinks(
  overview: DashboardFdnOverview | null,
  reasons: DashboardUnavailableReason[],
  t: TranslateFn
): UnavailableActionLink[] {
  if (!overview) {
    return [];
  }
  const actions: UnavailableActionLink[] = [];
  const seen = new Set<string>();
  const seasonId = overview.seasonId ?? null;

  for (const reason of reasons) {
    if (reason === 'NO_ACTIVE_SEASON' || reason === 'INSUFFICIENT_HISTORY') {
      pushAction(actions, seen, {
        key: 'create-season',
        href: '/farmer/seasons',
        label: t('dashboard.fdn.cta.createSeason', {
          defaultValue: 'Go to Seasons',
        }),
      });
    }
    if (reason === 'NO_HARVEST' || reason === 'INSUFFICIENT_HISTORY') {
      pushAction(actions, seen, {
        key: 'add-harvest',
        href: seasonWorkspaceHref(seasonId, 'harvest'),
        label: t('dashboard.fdn.cta.addHarvest', {
          defaultValue: 'Add harvest record',
        }),
      });
    }
    if (reason === 'MISSING_NITROGEN_INPUT') {
      pushAction(actions, seen, {
        key: 'add-nutrient-input',
        href: seasonWorkspaceHref(seasonId, 'nutrient-inputs'),
        label: t('dashboard.fdn.cta.addNutrientInput', {
          defaultValue: 'Add nutrient inputs',
        }),
      });
    }
  }

  for (const input of overview.missingInputs ?? []) {
    const normalized = input.trim().toUpperCase();
    if (
      normalized === 'MINERAL_FERTILIZER' ||
      normalized === 'ORGANIC_FERTILIZER' ||
      normalized === 'CONTROL_SUPPLY' ||
      normalized === 'BIOLOGICAL_FIXATION' ||
      normalized === 'ATMOSPHERIC_DEPOSITION' ||
      normalized === 'SEED_IMPORT'
    ) {
      pushAction(actions, seen, {
        key: 'add-nutrient-input',
        href: seasonWorkspaceHref(seasonId, 'nutrient-inputs'),
        label: t('dashboard.fdn.cta.addNutrientInput', {
          defaultValue: 'Add nutrient inputs',
        }),
      });
      continue;
    }
    if (normalized === 'IRRIGATION_WATER') {
      pushAction(actions, seen, {
        key: 'add-irrigation-analysis',
        href: seasonWorkspaceHref(seasonId, 'irrigation-water-analyses'),
        label: t('dashboard.fdn.cta.addIrrigationAnalysis', {
          defaultValue: 'Add irrigation analysis',
        }),
      });
      continue;
    }
    if (normalized === 'SOIL_LEGACY') {
      pushAction(actions, seen, {
        key: 'add-soil-test',
        href: seasonWorkspaceHref(seasonId, 'soil-tests'),
        label: t('dashboard.fdn.cta.addSoilTest', {
          defaultValue: 'Add soil test',
        }),
      });
    }
  }

  return actions;
}
