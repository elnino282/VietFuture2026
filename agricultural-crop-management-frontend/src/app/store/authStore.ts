/**
 * Auth Store Facade
 *
 * Shared auth facade for cross-domain auth state access.
 * This facade re-exports the canonical useAuth hook and UserRole type
 * so that both public and dashboard code can import auth primitives
 * from a single location without coupling to the AuthContext file path.
 *
 * In the future this could be replaced with a dedicated state store
 * if the auth state needs to be lifted outside the React tree.
 */

export { useAuth } from "@/features/auth";
export type { UserRole, User, AuthContextType } from "@/features/auth";
