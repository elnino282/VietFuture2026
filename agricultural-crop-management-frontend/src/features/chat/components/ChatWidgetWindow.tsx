import type { RefObject } from "react";
import { useState, useRef, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { ArrowLeft, Bell, ChevronDown, Flag, MoreVertical, Store, UserRound } from "lucide-react";
import { Button } from "@/components/ui/button";
import type { ChatWidgetConversation, ChatWidgetMessage } from "../model/widgetTypes";
import { ChatWidgetEmptyState } from "./ChatWidgetEmptyState";
import { ChatWidgetInput } from "./ChatWidgetInput";
import { ChatWidgetMessageBubble } from "./ChatWidgetMessageBubble";

type ChatWidgetWindowProps = {
  conversation: ChatWidgetConversation | null;
  messages: ChatWidgetMessage[];
  isMessagesLoading: boolean;
  isSending: boolean;
  error: string | null;
  onBack: () => void;
  onSend: (content: string) => Promise<void>;
  bottomRef: RefObject<HTMLDivElement>;
};

export function ChatWidgetWindow({
  conversation,
  messages,
  isMessagesLoading,
  isSending,
  error,
  onBack,
  onSend,
  bottomRef,
}: ChatWidgetWindowProps) {
  const navigate = useNavigate();
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement>(null);
  const triggerRef = useRef<HTMLButtonElement>(null);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (
        isMenuOpen &&
        menuRef.current &&
        !menuRef.current.contains(event.target as Node) &&
        triggerRef.current &&
        !triggerRef.current.contains(event.target as Node)
      ) {
        setIsMenuOpen(false);
      }
    };

    document.addEventListener("mousedown", handleClickOutside);
    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, [isMenuOpen]);


  if (!conversation) {
    return (
      <main className="chat-widget-window chat-widget-window--empty">
        <ChatWidgetEmptyState />
      </main>
    );
  }

  const onlineLabel = conversation.status === "online" ? "Đang hoạt động" : "Ngoại tuyến";

  return (
    <main className="chat-widget-window">
      <header className="chat-widget-thread-header">

        <div className="chat-widget-thread-header__identity">
          <div className="chat-widget-avatar chat-widget-avatar--sm" aria-hidden="true">
            {conversation.avatarUrl ? (
              <img src={conversation.avatarUrl} alt="" referrerPolicy="no-referrer" />
            ) : (
              conversation.farmName.slice(0, 1).toUpperCase()
            )}
            {conversation.status === "online" ? <span /> : null}
          </div>

          <div className="min-w-0">
            <button
              ref={triggerRef}
              type="button"
              className="chat-widget-thread-header__name"
              aria-expanded={isMenuOpen}
              onClick={() => setIsMenuOpen((current) => !current)}
            >
              <span>{conversation.farmName}</span>
              <ChevronDown className="h-4 w-4" aria-hidden="true" />
            </button>
            <p>
              {conversation.sellerName}
              <span className="chat-widget-thread-header__status" data-status={conversation.status}>
                {" - "}
                {onlineLabel}
              </span>
            </p>
          </div>
        </div>

        <div className="chat-widget-thread-header__menu">
          

          {isMenuOpen ? (
            <div ref={menuRef} className="chat-widget-profile-menu" role="menu">
              <div className="chat-widget-profile-menu__profile">
                <div className="chat-widget-avatar" aria-hidden="true">
                  {conversation.avatarUrl ? (
                    <img src={conversation.avatarUrl} alt="" referrerPolicy="no-referrer" />
                  ) : (
                    conversation.farmName.slice(0, 1).toUpperCase()
                  )}
                </div>
                <strong>{conversation.farmName}</strong>
              </div>

              <button type="button" role="menuitem">
                <span>
                  <Bell className="h-4 w-4" aria-hidden="true" />
                  Tắt thông báo
                </span>
                <span className="chat-widget-profile-menu__toggle" aria-hidden="true" />
              </button>
              <button type="button" role="menuitem">
                <span>
                  <Flag className="h-4 w-4" aria-hidden="true" />
                  Báo cáo
                </span>
                <ChevronDown className="h-4 w-4 -rotate-90" aria-hidden="true" />
              </button>
              <button
                type="button"
                role="menuitem"
                disabled={!conversation.farm?.id}
                onClick={() => {
                  if (conversation.farm?.id) {
                    setIsMenuOpen(false);
                    navigate(`/marketplace/farms/${conversation.farm.id}`);
                  }
                }}
              >
                <span>
                  <Store className="h-4 w-4" aria-hidden="true" />
                  Xem trang nông trại
                </span>
                <ChevronDown className="h-4 w-4 -rotate-90" aria-hidden="true" />
              </button>
            </div>
          ) : null}
        </div>
      </header>

      <div className="chat-widget-messages" aria-live="polite">
        {isMessagesLoading ? <p className="chat-widget-muted">Đang tải tin nhắn...</p> : null}
        {error ? <p className="chat-widget-error">{error}</p> : null}
        {!isMessagesLoading && !error && messages.length === 0 ? (
          <p className="chat-widget-muted">Chưa có tin nhắn nào.</p>
        ) : null}
        {messages.map((message) => (
          <ChatWidgetMessageBubble
            key={message.id}
            message={message}
            sellerName={conversation.sellerName}
          />
        ))}
        <div ref={bottomRef} />
      </div>

      <ChatWidgetInput disabled={!conversation} isSending={isSending} onSend={onSend} />
    </main>
  );
}
