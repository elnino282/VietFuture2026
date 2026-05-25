import { useMemo } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import { useAuth } from "@/features/auth";
import { useChatBootstrap } from "../model/useChatBootstrap";
import { useConversations } from "./useConversations";

/**
 * Domain logic for the global floating chat button.
 *
 * Visibility is based on **authentication status** (not Firebase chat readiness)
 * so the button appears on every page once the user is logged in.
 * The /chat page itself handles all Firebase error / disabled states.
 *
 * Unread count is a best-effort bonus: returns 0 when Firebase chat is
 * not yet ready rather than blocking button visibility.
 *
 * Responsibilities (SRP):
 *  - Determine if the button should be visible
 *  - Compute total unread message count (graceful fallback to 0)
 *  - Provide a stable navigation handler to /chat
 */
export type UseFloatingChatButtonResult = {
  /** Whether the floating button should be rendered at all */
  isVisible: boolean;
  /** Total unread messages across all conversations, capped at 99 */
  unreadCount: number;
  /** Navigate to the chat page */
  navigateToChat: () => void;
};

const UNREAD_CAP = 99;
const CHAT_ROUTE = "/chat";

export function useFloatingChatButton(): UseFloatingChatButtonResult {
  const navigate = useNavigate();
  const location = useLocation();

  // ✅ Visibility is driven by app-level auth — independent of Firebase chat
  const { isAuthenticated } = useAuth();

  // Unread count: only active when Firebase chat is ready (bonus feature)
  const bootstrap = useChatBootstrap();
  const currentUid = bootstrap.status === "ready" ? bootstrap.appUid : null;
  const currentRole = bootstrap.status === "ready" ? bootstrap.role : null;
  const { conversations } = useConversations(currentUid, currentRole);

  const isOnChatPage = location.pathname === CHAT_ROUTE;

  const unreadCount = useMemo(
    () =>
      Math.min(
        conversations.reduce((sum, c) => sum + (c.unreadCount ?? 0), 0),
        UNREAD_CAP
      ),
    [conversations]
  );

  const navigateToChat = () => {
    navigate(CHAT_ROUTE);
  };

  return {
    // Show whenever authenticated and not already on the chat page
    isVisible: isAuthenticated && !isOnChatPage,
    unreadCount,
    navigateToChat,
  };
}
