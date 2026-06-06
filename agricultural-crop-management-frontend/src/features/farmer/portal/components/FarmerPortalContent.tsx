import { Outlet, useLocation } from 'react-router-dom';
import { SeasonGate } from '@/shared/components';

// Routes that don't require a season to be selected
const EXEMPT_ROUTES = [
  '/farmer/seasons',
  '/farmer/farms',
  '/farmer/profile',
  '/farmer/settings',
  '/farmer/notifications',
  '/farmer/search',
  '/farmer/marketplace-workspace',
  '/farmer/marketplace-dashboard',
  '/farmer/marketplace-products',
  '/farmer/marketplace-orders',
];

/**
 * Renders child routes using React Router v6 Outlet
 * Wraps season-dependent routes with SeasonGate to enforce selection
 */
export function FarmerPortalContent() {
  const location = useLocation();
  
  // Check if current route is exempt from season gate
  const isExempt = EXEMPT_ROUTES.some(route => location.pathname.startsWith(route));

  if (isExempt) {
    return <Outlet />;
  }

  return (
    <SeasonGate>
      <Outlet />
    </SeasonGate>
  );
}
