import { Navigate } from 'react-router-dom';

/**
 * Legacy route wrapper.
 * Farmer account management is unified under UsersRolesPage.
 */
export function FarmersPage() {
    return <Navigate to="/admin/users-roles?tab=users&role=FARMER" replace />;
}

