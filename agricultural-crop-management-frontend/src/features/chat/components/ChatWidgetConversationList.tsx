import { ChevronDown, Search } from "lucide-react";
import { cn } from "@/shared/lib";
import {
  formatChatTime,
  getChatDisplayName,
  getChatSubtitle,
  joinDefinedParts,
} from "../lib/chatDisplayHelpers";
import type { ChatConversation } from "../model/types";
import type { ChatWidgetFilter } from "../model/widgetTypes";
import { ChatContactSearch } from "./ChatContactSearch";

type ChatWidgetConversationListProps = {
  conversations: ChatConversation[];
  selectedConversationId: string | null;
  searchQuery: string;
  filter: ChatWidgetFilter;
  currentUid: string | null;
  isLoading: boolean;
  isStartingConversation: boolean;
  error: string | null;
  onSearchChange: (query: string) => void;
  onFilterChange: (filter: ChatWidgetFilter) => void;
  onSelectConversation: (conversationId: string) => void;
  onStartConversation: (peerUserId: number) => Promise<void>;
};

function getAvatarLabel(conversation: ChatConversation): string {
  return getChatDisplayName(conversation.peerProfile, conversation.peerUid)
    .slice(0, 1)
    .toUpperCase();
}

function getConversationSubtitle(conversation: ChatConversation): string | null {
  const profile = conversation.peerProfile;
  if (!profile) {
    return null;
  }

  return joinDefinedParts([getChatSubtitle(profile), profile.address], " - ");
}

export function ChatWidgetConversationList({
  conversations,
  selectedConversationId,
  searchQuery,
  filter,
  currentUid,
  isLoading,
  isStartingConversation,
  error,
  onSearchChange,
  onFilterChange,
  onSelectConversation,
  onStartConversation,
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
            placeholder="Tim hoi thoai..."
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

      {currentUid ? (
        <div className="chat-widget-contact-search">
          <ChatContactSearch
            currentUid={currentUid}
            conversations={conversations}
            onStartConversation={onStartConversation}
            onOpenExistingConversation={onSelectConversation}
            isStartingConversation={isStartingConversation}
          />
        </div>
      ) : null}

      <div className="chat-widget-sidebar__list">
        {isLoading ? <p className="chat-widget-muted">Dang tai hoi thoai...</p> : null}
        {error ? <p className="chat-widget-error">{error}</p> : null}
        {!isLoading && !error && conversations.length === 0 ? (
          <p className="chat-widget-muted">Khong co hoi thoai phu hop.</p>
        ) : null}
        {conversations.map((conversation) => {
          const isSelected = selectedConversationId === conversation.id;
          const unreadLabel =
            conversation.unreadCount > 99 ? "99+" : String(conversation.unreadCount);
          const displayName = getChatDisplayName(
            conversation.peerProfile,
            conversation.peerUid,
          );
          const subtitle = getConversationSubtitle(conversation);
          const timeLabel = formatChatTime(conversation.lastMessageAt);

          return (
            <button
              key={conversation.id}
              type="button"
              aria-label={`Open conversation with ${displayName}`}
              aria-current={isSelected ? "true" : undefined}
              className={cn(
                "chat-widget-conversation",
                isSelected && "chat-widget-conversation--active",
              )}
              onClick={() => onSelectConversation(conversation.id)}
            >
              <div className="chat-widget-avatar" aria-hidden="true">
                {conversation.peerProfile?.avatarUrl ? (
                  <img
                    src={conversation.peerProfile.avatarUrl}
                    alt=""
                    referrerPolicy="no-referrer"
                  />
                ) : (
                  getAvatarLabel(conversation)
                )}
              </div>
              <div className="min-w-0 flex-1">
                <div className="chat-widget-conversation__top">
                  <strong>{displayName}</strong>
                  {timeLabel ? (
                    <time dateTime={conversation.lastMessageAt?.toISOString()}>
                      {timeLabel}
                    </time>
                  ) : null}
                </div>
                {subtitle ? (
                  <span className="chat-widget-conversation__subtitle">{subtitle}</span>
                ) : null}
                <p>{conversation.lastMessageText || "Chua co tin nhan nao."}</p>
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
