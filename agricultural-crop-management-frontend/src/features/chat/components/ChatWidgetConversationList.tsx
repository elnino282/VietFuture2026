import { ChevronDown, Search } from "lucide-react";
import { cn } from "@/shared/lib";
import type { ChatWidgetConversation, ChatWidgetFilter } from "../model/widgetTypes";
import {
  getTransactionStatusClass,
  getTransactionStatusLabel,
} from "../lib/chatDisplayHelpers";

type ChatWidgetConversationListProps = {
  conversations: ChatWidgetConversation[];
  selectedConversationId: string | null;
  searchQuery: string;
  filter: ChatWidgetFilter;
  isLoading: boolean;
  error: string | null;
  onSearchChange: (query: string) => void;
  onFilterChange: (filter: ChatWidgetFilter) => void;
  onSelectConversation: (conversationId: string) => void;
};

function formatConversationDate(value: Date) {
  return value.toLocaleDateString("vi-VN", { day: "2-digit", month: "2-digit" });
}

export function ChatWidgetConversationList({
  conversations,
  selectedConversationId,
  searchQuery,
  filter,
  isLoading,
  error,
  onSearchChange,
  onFilterChange,
  onSelectConversation,
}: ChatWidgetConversationListProps) {
  return (
    <aside className="chat-widget-sidebar" aria-label="Conversations">
      <div className="chat-widget-sidebar__controls">
        <label className="chat-widget-search">
          <Search className="h-3.5 w-3.5" aria-hidden="true" />
          <span className="sr-only">Search conversations</span>
          <input
            type="search"
            value={searchQuery}
            placeholder="Tìm theo tên..."
            onChange={(event) => onSearchChange(event.target.value)}
          />
        </label>

        <label className="chat-widget-filter">
          <span className="sr-only">Filter conversations</span>
          <select
            value={filter}
            onChange={(event) => onFilterChange(event.target.value as ChatWidgetFilter)}
          >
            <option value="all">Tất cả</option>
            <option value="unread">Chưa đọc</option>
          </select>
          <ChevronDown className="h-3.5 w-3.5" aria-hidden="true" />
        </label>
      </div>

      <div className="chat-widget-sidebar__list">
        {isLoading ? <p className="chat-widget-muted">Đang tải hội thoại...</p> : null}
        {error ? <p className="chat-widget-error">{error}</p> : null}
        {!isLoading && !error && conversations.length === 0 ? (
          <p className="chat-widget-muted">Không có hội thoại phù hợp.</p>
        ) : null}
        {conversations.map((conversation) => {
          const isSelected = selectedConversationId === conversation.id;
          const unreadLabel =
            conversation.unreadCount > 99 ? "99+" : String(conversation.unreadCount);

          return (
            <button
              key={conversation.id}
              type="button"
              aria-label={`Open conversation with ${conversation.farmName}`}
              aria-current={isSelected ? "true" : undefined}
              className={cn(
                "chat-widget-conversation",
                isSelected && "chat-widget-conversation--active",
              )}
              onClick={() => onSelectConversation(conversation.id)}
            >
              <div className="chat-widget-avatar" aria-hidden="true">
                {conversation.avatarUrl ? (
                  <img
                    src={conversation.avatarUrl}
                    alt=""
                    referrerPolicy="no-referrer"
                  />
                ) : (
                  conversation.farmName.slice(0, 1).toUpperCase()
                )}
                {conversation.status === "online" ? <span /> : null}
              </div>
              <div className="min-w-0 flex-1">
                <div className="chat-widget-conversation__top">
                  <strong>{conversation.farmName}</strong>
                  <time dateTime={conversation.lastMessageAt.toISOString()}>
                    {formatConversationDate(conversation.lastMessageAt)}
                  </time>
                </div>
                <p>{conversation.lastMessage}</p>
                <span
                  className={cn(
                    "chat-widget-conversation__status",
                    getTransactionStatusClass(conversation.context.transactionStatus),
                  )}
                >
                  {getTransactionStatusLabel(conversation.context.transactionStatus)}
                </span>
              </div>
              {conversation.unreadCount > 0 ? (
                <span className="chat-widget-unread" aria-label={`${unreadLabel} unread`}>
                  {unreadLabel}
                </span>
              ) : null}
            </button>
          );
        })}
      </div>
    </aside>
  );
}
