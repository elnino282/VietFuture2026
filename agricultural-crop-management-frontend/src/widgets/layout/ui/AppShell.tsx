import { useAppShell } from '../hooks/useAppShell';
import { Header } from './Header';
import { Sidebar } from './Sidebar';
import { NotificationsDrawer } from './NotificationsDrawer';
import { AiDrawer } from './AiDrawer';
import { BreadcrumbContextBar } from './BreadcrumbContextBar';
import type { AppShellProps } from '../model/types';
import { useCallback, useEffect, useState } from 'react';

/**
 * AppShell Component
 * 
 * Main application shell providing consistent layout across all portals.
 * Refactored into modular architecture following Clean Code principles.
 * 
 * Responsibilities:
 * - Wire logic from custom hook
 * - Compose sub-components
 * - Manage layout structure
 * 
 * Architecture:
 * - Single Responsibility: Container only
 * - Separation of Concerns: Logic in hook, UI in sub-components
 * - Colocation: All related code in widget directory
 */
export function AppShell({
    portalType,
    currentView,
    onViewChange,
    children,
    breadcrumbs = [],
    userName = 'John Doe',
    userEmail = 'john.doe@farm.com',
    userAvatar,
    onAiDrawerChange,
    aiDrawerExternalOpen,
    onLogout,
}: AppShellProps) {
    // Extract all state and logic from custom hook
    const {
        sidebarCollapsed,
        aiDrawerOpen,
        notificationsOpen,
        theme,
        language,
        notifications,
        config,
        unreadCount,
        handleToggleSidebar,
        handleAiDrawerChange,
        handleAiDrawerOpen,
        setNotificationsOpen,
        handleNotificationsOpen,
        markNotificationAsRead,
        markAllAsRead,
        setTheme,
        setLanguage,
    } = useAppShell({
        portalType,
        currentView,
        onViewChange,
        children,
        breadcrumbs,
        userName,
        userEmail,
        userAvatar,
        onAiDrawerChange,
        aiDrawerExternalOpen,
    });

    const portalScopeClass =
        portalType === 'FARMER'
            ? 'portal-farmer'
            : portalType === 'ADMIN'
                ? 'portal-admin'
                : 'portal-default';
    const [isMobile, setIsMobile] = useState(() => {
        if (typeof window === 'undefined') return false;
        return window.matchMedia('(max-width: 767px)').matches;
    });
    const [mobileSidebarOpen, setMobileSidebarOpen] = useState(false);

    useEffect(() => {
        if (typeof window === 'undefined') return undefined;

        const mediaQuery = window.matchMedia('(max-width: 767px)');
        const updateMobileState = (event?: MediaQueryListEvent) => {
            setIsMobile(event ? event.matches : mediaQuery.matches);
        };

        updateMobileState();
        mediaQuery.addEventListener('change', updateMobileState);
        return () => mediaQuery.removeEventListener('change', updateMobileState);
    }, []);

    useEffect(() => {
        if (!isMobile) {
            setMobileSidebarOpen(false);
        }
    }, [isMobile]);

    useEffect(() => {
        if (!isMobile || !mobileSidebarOpen) return undefined;

        const originalOverflow = document.body.style.overflow;
        document.body.style.overflow = 'hidden';
        return () => {
            document.body.style.overflow = originalOverflow;
        };
    }, [isMobile, mobileSidebarOpen]);

    useEffect(() => {
        if (isMobile) {
            setMobileSidebarOpen(false);
        }
    }, [currentView, isMobile]);

    const handleSidebarToggle = useCallback(() => {
        if (isMobile) {
            setMobileSidebarOpen((prev) => !prev);
            return;
        }
        handleToggleSidebar();
    }, [handleToggleSidebar, isMobile]);

    const handleViewNavigate = useCallback((view: string) => {
        if (isMobile) {
            setMobileSidebarOpen(false);
        }
        onViewChange(view);
    }, [isMobile, onViewChange]);

    return (
        <div className={`h-screen flex flex-col overflow-hidden ${portalScopeClass}`} data-portal={portalType.toLowerCase()}>
            {/* Top Navigation Bar */}
            <Header
                config={config}
                breadcrumbs={breadcrumbs}
                sidebarCollapsed={sidebarCollapsed}
                unreadCount={unreadCount}
                userName={userName}
                userEmail={userEmail}
                userAvatar={userAvatar}
                portalType={portalType}
                theme={theme}
                language={language}
                onToggleSidebar={handleSidebarToggle}
                onViewChange={handleViewNavigate}
                onAiDrawerOpen={handleAiDrawerOpen}
                onNotificationsOpen={handleNotificationsOpen}
                onThemeChange={setTheme}
                onLanguageChange={setLanguage}
                onLogout={onLogout}
            />

            {/* Main Content Area */}
            <div className="flex-1 flex overflow-hidden">
                {/* Side Navigation */}
                <div className="hidden md:block">
                    <Sidebar
                        navigationItems={config.navigation}
                        currentView={currentView}
                        collapsed={sidebarCollapsed}
                        portalType={portalType}
                        onNavigate={handleViewNavigate}
                        onToggleCollapse={handleToggleSidebar}
                    />
                </div>

                {isMobile && mobileSidebarOpen && (
                    <>
                        <button
                            type="button"
                            aria-label="Close navigation menu"
                            className="fixed inset-0 z-40 bg-black/45 md:hidden"
                            onClick={() => setMobileSidebarOpen(false)}
                        />
                        <div className="fixed inset-y-0 left-0 z-50 md:hidden">
                            <Sidebar
                                navigationItems={config.navigation}
                                currentView={currentView}
                                collapsed={false}
                                portalType={portalType}
                                onNavigate={handleViewNavigate}
                                onToggleCollapse={() => setMobileSidebarOpen(false)}
                            />
                        </div>
                    </>
                )}

                {/* Page Content */}
                <main className="acm-main-content flex-1 overflow-auto">
                    <BreadcrumbContextBar breadcrumbs={breadcrumbs} />
                    {children}
                </main>
            </div>

            {/* AI Assistant Drawer */}
            <AiDrawer
                open={aiDrawerOpen}
                onOpenChange={handleAiDrawerChange}
                portalColor={config.color}
            />

            {/* Notifications Drawer */}
            <NotificationsDrawer
                open={notificationsOpen}
                onOpenChange={setNotificationsOpen}
                notifications={notifications}
                unreadCount={unreadCount}
                onMarkAsRead={markNotificationAsRead}
                onMarkAllAsRead={markAllAsRead}
            />
        </div>
    );
}
