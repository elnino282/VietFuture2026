import {
    Home, MapPin, Calendar, Package,
    ShoppingCart, BarChart3, Settings, Sprout, FileText,
    Shield, Warehouse, Users, AlertTriangle, Bell, ClipboardList, Clock3, Wallet2, MessageSquare
} from 'lucide-react';
import type { PortalType, PortalConfig } from '../model/types';

/**
 * Portal Configurations
 * Defines navigation and styling for each portal type
 */
export const portalConfig: Record<PortalType, PortalConfig> = {
    ADMIN: {
        name: 'Admin Portal',
        color: '#3BA55D',
        icon: Shield,
        emoji: '🔐',
        navigation: [
            { id: 'dashboard', label: 'Admin Dashboard', icon: Home },
            { id: 'marketplace-dashboard', label: 'Marketplace', icon: ShoppingCart },
            { id: 'marketplace-products', label: 'Marketplace Products', icon: Sprout },
            { id: 'marketplace-orders', label: 'Marketplace Orders', icon: Package },
            { id: 'inventory', label: 'Inventory', icon: Package },
            { id: 'incidents', label: 'Incidents', icon: AlertTriangle },
            { id: 'alerts', label: 'Alerts Center', icon: Bell },
            { id: 'audit-logs', label: 'Audit Logs', icon: ClipboardList },
            { id: 'users-roles', label: 'Users & Roles', icon: Users },
            { id: 'farms-plots', label: 'Farms & Plots', icon: Warehouse },
            { id: 'crops-varieties', label: 'Crops & Varieties', icon: Sprout },
            { id: 'reports', label: 'Reports', icon: BarChart3 },
            { id: 'documents', label: 'Documents', icon: FileText },
        ],
    },
    FARMER: {
        name: 'Farmer Portal',
        color: '#2F9E44',
        icon: Sprout,
        emoji: '🌾',
        navigation: [
            { id: 'dashboard', label: 'Dashboard', icon: Home },
            { id: 'marketplace-workspace', label: 'Marketplace Workspace', icon: ShoppingCart },
            { id: 'farms', label: 'Farms & Plots', icon: MapPin },
            { id: 'seasons', label: 'Seasons', icon: Calendar },
            { id: 'suppliers-supplies', label: 'Suppliers & Supplies', icon: Package },
            { id: 'product-warehouse', label: 'Product Warehouse', icon: Package },
            { id: 'documents', label: 'Documents', icon: FileText },
            { id: 'ai-assistant', label: 'AI Assistant', icon: Sprout },
            { id: 'chat', label: 'Chat', icon: MessageSquare },
            { id: 'notifications', label: 'Notifications', icon: Bell },
        ],
    },
    BUYER: {
        name: 'Buyer Portal',
        color: '#0CA678',
        icon: ShoppingCart,
        emoji: '🛒',
        navigation: [
            { id: 'dashboard', label: 'Dashboard', icon: Home },
            { id: 'marketplace', label: 'Marketplace', icon: ShoppingCart },
            { id: 'orders', label: 'My Orders', icon: Package },
            { id: 'traceability', label: 'Traceability', icon: MapPin },
            { id: 'reports', label: 'Reports', icon: BarChart3 },
            { id: 'settings', label: 'Settings', icon: Settings },
        ],
    },
    EMPLOYEE: {
        name: 'Employee Portal',
        color: '#3BA55D',
        icon: ClipboardList,
        emoji: '🧑‍🌾',
        navigation: [
            { id: 'tasks', label: 'Assigned Tasks', icon: ClipboardList },
            { id: 'workspace', label: 'Season Workspace', icon: Calendar },
            { id: 'progress', label: 'Progress Reports', icon: Clock3 },
            { id: 'payroll', label: 'Payroll', icon: Wallet2 },
            { id: 'chat', label: 'Chat', icon: MessageSquare },
        ],
    },
};

/**
 * Language Names Mapping
 */
export const languageNames = {
    en: 'English',
    vi: 'Vietnamese',
};

/**
 * Search Debounce Delay (ms)
 */
export const SEARCH_DEBOUNCE_DELAY = 300;

/**
 * Search Minimum Length
 */
export const SEARCH_MIN_LENGTH = 2;
