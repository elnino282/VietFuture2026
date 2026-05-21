import { AI_FloatButton } from "@/features/shared/aiButton/AI_FloatButton";
import { AppShell } from "@/features/shared/layout";
import i18n from "@/i18n";
import { Loader2 } from "lucide-react";
import { useCallback, useEffect, useRef, useState } from "react";
import { useLocation } from "react-router-dom";
import { FarmerPortalContent } from "./components/FarmerPortalContent";
import { useFarmerPortalShell } from "./hooks/useFarmerPortalShell";

/**
 * Main farmer portal container with application shell
 *
 * Integrates:
 * - AppShell layout with navigation
 * - View-based content rendering
 * - AI assistant float button
 * - Authentication and user management
 */
export function FarmerPortalWithShell() {
  const location = useLocation();
  const {
    currentView,
    setCurrentView,
    aiChatOpen,
    setAiChatOpen,
    userName,
    userEmail,
    breadcrumbs,
    handleLogout,
  } = useFarmerPortalShell();
  const [isRouteTransitionLoading, setIsRouteTransitionLoading] = useState(false);
  const previousPathRef = useRef(location.pathname);
  const routeLoadingTimeoutRef = useRef<number | null>(null);

  const clearRouteLoadingTimeout = useCallback(() => {
    if (routeLoadingTimeoutRef.current !== null) {
      window.clearTimeout(routeLoadingTimeoutRef.current);
      routeLoadingTimeoutRef.current = null;
    }
  }, []);

  const triggerRouteTransitionLoading = useCallback((duration = 650) => {
    clearRouteLoadingTimeout();
    setIsRouteTransitionLoading(true);
    routeLoadingTimeoutRef.current = window.setTimeout(() => {
      setIsRouteTransitionLoading(false);
      routeLoadingTimeoutRef.current = null;
    }, duration);
  }, [clearRouteLoadingTimeout]);

  const handleViewChange = useCallback((view: string) => {
    triggerRouteTransitionLoading();
    setCurrentView(view);
  }, [setCurrentView, triggerRouteTransitionLoading]);

  useEffect(() => {
    if (previousPathRef.current === location.pathname) {
      return;
    }
    previousPathRef.current = location.pathname;
    triggerRouteTransitionLoading(320);
  }, [location.pathname, triggerRouteTransitionLoading]);

  useEffect(() => () => clearRouteLoadingTimeout(), [clearRouteLoadingTimeout]);

  return (
    <AppShell
      portalType="FARMER"
      currentView={currentView}
      onViewChange={handleViewChange}
      breadcrumbs={breadcrumbs}
      userName={userName}
      userEmail={userEmail}
      aiDrawerExternalOpen={aiChatOpen}
      onAiDrawerChange={setAiChatOpen}
      onLogout={handleLogout}
    >
      <FarmerPortalContent />
      {isRouteTransitionLoading && (
        <div
          className="acm-page-loading-overlay"
          role="status"
          aria-live="polite"
          aria-label={i18n.t("common.loadingPageTransition")}
        >
          <div className="acm-page-loading-card acm-body-text">
            <Loader2 className="h-5 w-5 animate-spin text-primary" />
            <span>{i18n.t("common.loadingPageTransition")}...</span>
          </div>
        </div>
      )}

      {/* Global AI Assistant Float Button */}
      <AI_FloatButton
        onClick={() => setAiChatOpen(true)}
        isHidden={aiChatOpen}
      />
    </AppShell>
  );
}
