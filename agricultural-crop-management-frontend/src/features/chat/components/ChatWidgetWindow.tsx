import type { RefObject } from "react";
import { ArrowLeft, MapPin, MoreVertical } from "lucide-react";
import { Button } from "@/components/ui/button";
import type { ChatWidgetConversation, ChatWidgetMessage } from "../model/widgetTypes";
import { ChatWidgetContextCard } from "./ChatWidgetContextCard";
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
        {/* Back button: visible on mobile only via CSS */}
        <Button
          type="button"
          variant="ghost"
          size="icon"
          className="chat-widget-back chat-widget-back--mobile-only"
          aria-label="Back to conversations"
          onClick={onBack}
        >
          <ArrowLeft className="h-4 w-4" aria-hidden="true" />
        </Button>
        <div className="chat-widget-avatar chat-widget-avatar--sm" aria-hidden="true">
          {conversation.avatarUrl ? (
            <img src={conversation.avatarUrl} alt="" referrerPolicy="no-referrer" />
          ) : (
            conversation.farmName.slice(0, 1).toUpperCase()
          )}
          {conversation.status === "online" ? <span /> : null}
        </div>
        <div className="min-w-0 flex-1">
          <h3>{conversation.farmName}</h3>
          <p>
            <MapPin className="h-3 w-3" aria-hidden="true" />
            {conversation.region} · {conversation.sellerName}
            <span className="chat-widget-thread-header__status" data-status={conversation.status}>
              {" · "}{onlineLabel}
            </span>
          </p>
        </div>
        <Button
          type="button"
          variant="ghost"
          size="icon"
          className="chat-widget-back"
          aria-label="More options"
        >
          <MoreVertical className="h-4 w-4" aria-hidden="true" />
        </Button>
      </header>

      <div className="chat-widget-window__context">
        <ChatWidgetContextCard context={conversation.context} />
      </div>

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
