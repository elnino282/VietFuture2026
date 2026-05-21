import { Navigate } from 'react-router-dom';

/**
 * Legacy route wrapper.
 * Buyer account management is unified under UsersRolesPage.
 */
export function BuyersPage() {
    return <Navigate to="/admin/users-roles?tab=users&role=BUYER" replace />;
}

