import "./FloatingChatButton.css";
import { MessageSquare } from "lucide-react";
import { useTranslation } from "react-i18next";
import { useFloatingChatButton } from "../hooks/useFloatingChatButton";

/**
 * FloatingChatButton — a globally-mounted, fixed-position button.
 *
 * Design principles (SOLID):
 *  - SRP: renders only the button UI; all logic lives in useFloatingChatButton
 *  - OCP: appearance is controlled via CSS classes in ChatPage.css tokens
 *  - LSP / ISP: no unused props; accepts nothing
 *  - DIP: depends on the hook abstraction, not on Firestore/Firebase directly
 *
 * Accessibility:
 *  - Keyboard-focusable button with aria-label
 *  - role="status" on the badge for live unread count announcements
 */
export function FloatingChatButton() {
  const { t } = useTranslation();
  const { isVisible, unreadCount, navigateToChat } = useFloatingChatButton();

  if (!isVisible) {
    return null;
  }

  const hasUnread = unreadCount > 0;
  const unreadLabel = unreadCount > 99 ? "99+" : String(unreadCount);

  return (
    <div className="floating-chat-btn-wrapper">
      <button
        id="floating-chat-btn"
        type="button"
        aria-label={
          hasUnread
            ? t("chat.floatingButton.ariaWithUnread", {
                defaultValue: "Open chat — {{count}} unread",
                count: unreadCount,
              })
            : t("chat.floatingButton.aria", { defaultValue: "Open chat" })
        }
        onClick={navigateToChat}
        className="floating-chat-btn"
      >
        <MessageSquare className="floating-chat-btn__icon" aria-hidden="true" />

        {hasUnread && (
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
        )}

        <span className="floating-chat-btn__tooltip">
          {t("chat.floatingButton.tooltip", { defaultValue: "Chat" })}
        </span>
      </button>
    </div>
  );
}
