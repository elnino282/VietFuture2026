import { useI18n } from '@/hooks/useI18n';
import { Badge, Button, Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from '@/shared/ui';
import { ChevronLeft, ChevronRight } from 'lucide-react';
import type { SidebarProps } from '../model/types';

// Map navigation item IDs to translation keys
const navTranslationKeys: Record<string, string> = {
    'dashboard': 'nav.dashboard',
    'farms': 'nav.farms',
    'farms-plots': 'nav.farms',
    'seasons': 'nav.seasons',
    'tasks': 'nav.tasks',
    'field-logs': 'nav.fieldLogs',
    'expenses': 'nav.expenses',
    'harvest': 'nav.harvest',
    'suppliers-supplies': 'nav.suppliersSupplies',
    'labor-management': 'nav.laborManagement',
    'inventory': 'nav.inventory',
    'product-warehouse': 'nav.productWarehouse',
    'documents': 'nav.documents',

    'notifications': 'nav.notifications',
    'ai-assistant': 'nav.aiAssistant',
    'chat': 'nav.chat',
    'alerts': 'nav.alertsCenter',
    'incidents': 'nav.incidents',
    'audit-logs': 'nav.auditLogs',
    'users-roles': 'nav.usersRoles',
    'crops-varieties': 'nav.cropsVarieties',
    'reports': 'nav.reports',
    'marketplace': 'nav.marketplace',
    'marketplace-workspace': 'nav.marketplace',
    'marketplace-dashboard': 'nav.marketplaceDashboard',
    'marketplace-products': 'nav.marketplaceProducts',
    'marketplace-orders': 'nav.orders',
    'orders': 'nav.orders',
    'traceability': 'nav.traceability',
    'progress': 'nav.progress',
    'payroll': 'nav.payroll',
    'settings': 'nav.settings',
};

/**
 * Sidebar Component
 * 
 * Side navigation panel with collapsible functionality.
 * Displays navigation items with icons, labels, and optional badges.
 * 
 * Single Responsibility: Side navigation UI
 */
export function Sidebar({
    navigationItems,
    currentView,
    collapsed,
    onNavigate,
    onToggleCollapse,
}: SidebarProps) {
    const { t } = useI18n();
    
    // Get translated label for navigation item
    const getNavLabel = (item: { id: string; label: string }) => {
        const translationKey = navTranslationKeys[item.id];
        if (translationKey) {
            const translated = t(translationKey);
            // If translation returns the key itself, fall back to label
            return translated !== translationKey ? translated : item.label;
        }
        return item.label;
    };
    
    return (
        <aside
            className={`acm-sidebar h-full overflow-hidden shadow-lg transition-all duration-300 shrink-0 ${collapsed ? 'w-[72px]' : 'w-64'
                }`}
        >
            <div className="h-full min-h-0 flex flex-col">
                {/* Navigation Items */}
                <nav className="flex-1 min-h-0 p-3 space-y-1 overflow-y-auto overscroll-contain">
                    {navigationItems.map((item) => {
                        const isActive = currentView === item.id;

                        return (
                        <TooltipProvider key={item.id}>
                            <Tooltip>
                                <TooltipTrigger asChild>
                                    <button
                                        onClick={() => onNavigate(item.id)}
                                        aria-current={isActive ? 'page' : undefined}
                                        className="acm-sidebar-nav-item acm-hover-surface relative w-full min-h-12 flex items-center gap-3 px-3 py-2 rounded-lg border border-l-4 transition-all duration-200"
                                    >
                                        <item.icon className="w-5 h-5 shrink-0" />
                                        {!collapsed && (
                                            <span className="text-sm font-medium truncate">{getNavLabel(item)}</span>
                                        )}
                                        {!collapsed && item.badge && (
                                            <Badge
                                                variant="secondary"
                                                className={`acm-sidebar-nav-badge ml-auto ${isActive ? 'acm-sidebar-nav-badge-active' : ''}`}
                                            >
                                                {item.badge}
                                            </Badge>
                                        )}
                                    </button>
                                </TooltipTrigger>
                                {collapsed && (
                                    <TooltipContent side="right">
                                        <p>{getNavLabel(item)}</p>
                                    </TooltipContent>
                                )}
                            </Tooltip>
                        </TooltipProvider>
                        );
                    })}
                </nav>

                {/* Collapse Toggle */}
                <div className="acm-sidebar-footer p-3 border-t">
                    <Button
                        variant="ghost"
                        size="sm"
                        onClick={onToggleCollapse}
                        className="acm-sidebar-collapse-btn acm-hover-surface w-full min-h-12 justify-center border"
                    >
                        {collapsed ? (
                            <ChevronRight className="w-4 h-4" />
                        ) : (
                            <>
                                <ChevronLeft className="w-4 h-4 mr-2" />
                                {t('nav.collapse')}
                            </>
                        )}
                    </Button>
                </div>
            </div>
        </aside>
    );
}
