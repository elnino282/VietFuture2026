import "./FloatingChatButton.css";
import { useCallback, useState, useEffect } from "react";
import { MessageSquare } from "lucide-react";
import { useTranslation } from "react-i18next";
import { useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "@/features/auth/context/AuthContext";
import { ChatWidget } from "./ChatWidget";

/**
 * FloatingChatButton owns only launcher-level state. The mounted ChatWidget
 * owns conversations, messages, filtering, and unread mutations.
 */
export function FloatingChatButton() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const location = useLocation();
  const { isAuthenticated } = useAuth();
  const [isOpen, setIsOpen] = useState(false);
  const [unreadCount, setUnreadCount] = useState(0);
  const [activePeerUserId, setActivePeerUserId] = useState<number | null>(null);

  useEffect(() => {
    const handleOpenChat = (event: Event) => {
      const customEvent = event as CustomEvent<{ peerUserId?: number }>;
      setIsOpen(true);
      if (customEvent.detail?.peerUserId) {
        setActivePeerUserId(customEvent.detail.peerUserId);
      }
    };
    window.addEventListener("open-chat-widget", handleOpenChat);
    return () => {
      window.removeEventListener("open-chat-widget", handleOpenChat);
    };
  }, []);

  useEffect(() => {
    setIsOpen(false);
    setActivePeerUserId(null);
  }, [location.pathname]);

  const isVisible = isAuthenticated && location.pathname !== "/chat";
  const hasUnread = unreadCount > 0;
  const unreadLabel = unreadCount > 99 ? "99+" : String(unreadCount);

  const handleExpand = useCallback(() => {
    navigate("/chat");
  }, [navigate]);

  if (!isVisible) {
    return null;
  }

  return (
    <div className="floating-chat-btn-wrapper">
      {!isOpen ? (
        <button
          id="floating-chat-btn"
          type="button"
          aria-label={
            hasUnread
              ? t("chat.floatingButton.ariaWithUnread", {
                  defaultValue: "Open chat - {{count}} unread",
                  count: unreadCount,
                })
              : t("chat.floatingButton.aria", { defaultValue: "Open chat" })
          }
          onClick={() => setIsOpen(true)}
          className="floating-chat-btn"
        >
          <MessageSquare className="floating-chat-btn__icon" aria-hidden="true" />

          {hasUnread ? (
            <span
              className="floating-chat-btn__badge"
              role="status"
              aria-label={t("chat.floatingButton.unreadBadgeAria", {
                defaultValue: "{{count}} unread messages",
                count: unreadCount,
              })}
            >
              {unreadLabel}
            </span>
          ) : null}

          <span className="floating-chat-btn__tooltip">
            {t("chat.floatingButton.tooltip", { defaultValue: "Chat" })}
          </span>
        </button>
      ) : null}

      <ChatWidget
        isOpen={isOpen}
        onMinimize={() => {
          setIsOpen(false);
          setActivePeerUserId(null);
        }}
        onClose={() => {
          setIsOpen(false);
          setActivePeerUserId(null);
        }}
        onExpand={handleExpand}
        onUnreadCountChange={setUnreadCount}
        activePeerUserId={activePeerUserId}
      />
    </div>
  );
}

