import { useQuery } from '@tanstack/react-query';
import { useLocation } from 'react-router-dom';

import {
  adminFarmApi,
  adminKeys,
  adminPlotApi,
  adminUsersApi,
} from '@/features/admin/shared/api';
import type { AdminUser } from '@/features/admin/shared/api';
import type { BreadcrumbPath } from '@/features/shared/layout/types';
import { useMarketplaceAdminOrderDetail } from '@/features/marketplace/hooks';
import { useI18n } from '@/shared/lib/hooks/useI18n';

import { getAdminBreadcrumbLabelKey } from '../constants';
import type { AdminView } from '../types';

type AdminPlotBreadcrumbDetail = {
  id: number;
  plotName: string;
  farmId: number | null;
  farmName: string | null;
};

type AdminPlotSeason = {
  id: number;
  seasonName: string;
};

const parsePositiveId = (value: string | null): number | null => {
  const parsed = Number(value);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : null;
};

const entityFallback = (label: string, id: number | null) => (id ? `${label} #${id}` : label);

const normalizePlotDetail = (response: unknown): AdminPlotBreadcrumbDetail | null => {
  const payload = (response as { result?: unknown })?.result ?? response;
  if (!payload || typeof payload !== 'object') return null;
  const item = payload as {
    id?: number;
    plotName?: string;
    farmId?: number | null;
    farmName?: string | null;
  };
  return {
    id: item.id ?? 0,
    plotName: item.plotName ?? '',
    farmId: item.farmId ?? null,
    farmName: item.farmName ?? null,
  };
};

const normalizePlotSeasons = (response: unknown): AdminPlotSeason[] => {
  const result = (response as { result?: unknown })?.result ?? response;
  return Array.isArray(result) ? (result as AdminPlotSeason[]) : [];
};

export function useAdminBreadcrumbs(currentView: AdminView): BreadcrumbPath[] {
  const { t } = useI18n();
  const location = useLocation();
  const searchParams = new URLSearchParams(location.search);

  const farmId = parsePositiveId(searchParams.get('farmId'));
  const plotId = parsePositiveId(searchParams.get('plotId'));
  const seasonId = parsePositiveId(searchParams.get('seasonId'));
  const userId = parsePositiveId(searchParams.get('userId'));
  const orderId = parsePositiveId(searchParams.get('orderId'));
  const tab = searchParams.get('tab');
  const role = searchParams.get('role')?.toUpperCase() ?? null;

  const farmQuery = useQuery({
    queryKey: adminKeys.farmDetail(farmId ?? 0),
    queryFn: () => adminFarmApi.getById(farmId ?? 0),
    enabled: Boolean(currentView === 'farms-plots' && farmId),
  });

  const plotQuery = useQuery({
    queryKey: adminKeys.plotDetail(plotId ?? 0),
    queryFn: async () => normalizePlotDetail(await adminPlotApi.getById(plotId ?? 0)),
    enabled: Boolean(currentView === 'farms-plots' && plotId),
  });

  const plotSeasonsQuery = useQuery({
    queryKey: adminKeys.plotSeasons(plotId ?? 0),
    queryFn: async () => normalizePlotSeasons(await adminPlotApi.getSeasons(plotId ?? 0)),
    enabled: Boolean(currentView === 'farms-plots' && plotId && seasonId),
  });

  const userQuery = useQuery<AdminUser>({
    queryKey: adminKeys.userDetail(userId ?? 0),
    queryFn: () => adminUsersApi.getById(userId ?? 0),
    enabled: Boolean(currentView === 'users-roles' && userId),
  });

  const adminOrderQuery = useMarketplaceAdminOrderDetail(
    currentView === 'marketplace-orders' ? orderId ?? undefined : undefined,
  );

  const portal: BreadcrumbPath = {
    label: t('breadcrumb.portal.admin', 'Admin Portal'),
    href: '/admin/dashboard',
    kind: 'portal',
  };
  const labelKey = getAdminBreadcrumbLabelKey(currentView);
  const moduleLabel = labelKey ? t(labelKey) : currentView;
  const moduleHref = `/admin/${currentView}`;
  const crumbs: BreadcrumbPath[] = [
    portal,
    { label: moduleLabel, href: moduleHref, kind: 'module' },
  ];

  if (currentView === 'farms-plots') {
    const activeTab = tab === 'plots' || plotId || seasonId ? 'plots' : 'farms';
    crumbs.push({
      label: activeTab === 'plots'
        ? t('admin.farmsPlots.tabs.plots', 'Plots')
        : t('admin.farmsPlots.tabs.farms', 'Farms'),
      href: `/admin/farms-plots?tab=${activeTab}`,
      kind: 'module',
    });

    const plot = plotQuery.data;
    const resolvedFarmName = farmQuery.data?.name ?? plot?.farmName ?? null;
    const resolvedFarmId = farmId ?? plot?.farmId ?? null;
    if (resolvedFarmId) {
      crumbs.push({
        label: resolvedFarmName ?? entityFallback(t('breadcrumb.fallback.farm', 'Farm'), resolvedFarmId),
        href: `/admin/farms-plots?tab=farms&farmId=${resolvedFarmId}`,
        kind: 'farm',
        loading: farmQuery.isLoading || (plotId != null && plotQuery.isLoading),
      });
    }

    if (plotId) {
      crumbs.push({
        label: plot?.plotName || entityFallback(t('breadcrumb.fallback.plot', 'Plot'), plotId),
        href: `/admin/farms-plots?tab=plots&plotId=${plotId}`,
        kind: 'plot',
        loading: plotQuery.isLoading,
      });
    }

    if (seasonId) {
      const season = plotSeasonsQuery.data?.find((item) => item.id === seasonId);
      crumbs.push({
        label: season?.seasonName ?? entityFallback(t('breadcrumb.fallback.season', 'Season'), seasonId),
        kind: 'season',
        loading: plotSeasonsQuery.isLoading,
      });
    }

    return crumbs;
  }

  if (currentView === 'users-roles') {
    if (tab === 'roles') {
      crumbs.push({ label: t('admin.users.tabs.roles', 'Roles'), href: '/admin/users-roles?tab=roles', kind: 'module' });
      return crumbs;
    }

    crumbs.push({ label: t('admin.users.tabs.users', 'Users'), href: '/admin/users-roles?tab=users', kind: 'module' });
    if (role) {
      crumbs.push({
        label: role,
        href: `/admin/users-roles?tab=users&role=${role}`,
        kind: 'record',
      });
    }
    if (userId) {
      const user = userQuery.data;
      crumbs.push({
        label: user?.fullName || user?.username || entityFallback(t('breadcrumb.fallback.user', 'User'), userId),
        kind: 'record',
        loading: userQuery.isLoading,
      });
    }
    return crumbs;
  }

  if (currentView === 'marketplace-dashboard') {
    return [
      portal,
      { label: t('nav.marketplaceDashboard', 'Marketplace Dashboard'), kind: 'module' },
    ];
  }

  if (currentView === 'marketplace-products') {
    return [
      portal,
      {
        label: t('nav.marketplaceProducts', 'Marketplace Products'),
        href: '/admin/marketplace-products',
        kind: 'module',
      },
    ];
  }

  if (currentView === 'marketplace-orders') {
    const marketplaceCrumbs: BreadcrumbPath[] = [
      portal,
      {
        label: t('nav.marketplaceOrders', 'Marketplace Orders'),
        href: '/admin/marketplace-orders',
        kind: 'module',
      },
    ];

    if (orderId) {
      marketplaceCrumbs.push({
        label: adminOrderQuery.data?.orderCode ?? entityFallback(t('breadcrumb.fallback.order', 'Order'), orderId),
        kind: 'record',
        loading: adminOrderQuery.isLoading,
      });
    }

    return marketplaceCrumbs;
  }

  return crumbs;
}
