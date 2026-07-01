import { Outlet } from 'react-router-dom';

/**
 * Renders child routes using React Router v6 Outlet
 * No longer wraps routes with SeasonGate - season selection is optional
 */
export function FarmerPortalContent() {
  return <Outlet />;
}
