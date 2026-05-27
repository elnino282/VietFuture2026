import { useMemo } from "react";
import { MessageSquare, SlidersHorizontal } from "lucide-react";
import { useTranslation } from "react-i18next";
import type { ChatConversation } from "../model/types";
import { ChatContactSearch } from "./ChatContactSearch";
import { ChatConversationItem } from "./ChatConversationItem";

type ChatConversationListProps = {
  currentUid: string | null;
  conversations: ChatConversation[];
  selectedConversationId: string | null;
  onSelectConversation: (conversationId: string) => void;
  onStartConversation: (peerUserId: number) => Promise<void>;
  isLoading: boolean;
  isStartingConversation: boolean;
  error: string | null;
};

export function ChatConversationList({
  currentUid,
  conversations,
  selectedConversationId,
  onSelectConversation,
  onStartConversation,
  isLoading,
  isStartingConversation,
  error,
}: ChatConversationListProps) {
  const { t } = useTranslation();
  const sortedConversations = useMemo(
    () =>
      [...conversations].sort((left, right) => {
        const leftTime = left.lastMessageAt?.getTime() ?? 0;
        const rightTime = right.lastMessageAt?.getTime() ?? 0;
        return rightTime - leftTime;
      }),
    [conversations]
  );

  const totalUnread = useMemo(
    () => Math.min(conversations.reduce((sum, c) => sum + (c.unreadCount ?? 0), 0), 99),
    [conversations]
  );

  return (
    <aside
      className="chat-sidebar-panel chat-panel-card flex h-full flex-col overflow-hidden rounded-2xl"
      data-chat-panel="conversation-list"
      data-chat-conversations-count={String(conversations.length)}
      aria-label={t("chat.messages.listAria", { defaultValue: "Messages" })}
    >
      <div className="chat-sidebar-header shrink-0 border-b px-[14px] pb-[11.3px] pt-[10.5px]">
        {/* Top bar: "Chat" title + unread badge + filter icon */}
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <h2
              className="text-[17.5px] font-medium leading-[26.25px] text-[#0a0a0a]"
              style={{ fontFamily: "'Inter', sans-serif" }}
            >
              {t("chat.messages.title", { defaultValue: "Chat" })}
            </h2>

            {/* Red unread badge — matches Figma red pill */}
            {totalUnread > 0 ? (
              <span
                className="chat-header-unread-badge flex h-[17.5px] min-w-[19.7px] items-center justify-center rounded-full px-[7px] py-[1.75px] text-[10.5px] font-normal"
                aria-label={t("chat.messages.unreadCountAria", {
                  defaultValue: "{{count}} unread",
                  count: totalUnread,
                })}
              >
                {totalUnread > 99 ? "99+" : totalUnread}
              </span>
            ) : null}
          </div>

          {/* Filter / sort icon button (UI only) */}
          <button
            type="button"
            aria-label={t("chat.actions.filter", { defaultValue: "Filter conversations" })}
            className="chat-composer-toolbar-btn"
          >
            <SlidersHorizontal className="h-[13px] w-[13px]" aria-hidden="true" />
          </button>
        </div>

        {/* Search input */}
        <div className="mt-[7px]">
          <ChatContactSearch
            currentUid={currentUid}
            conversations={conversations}
            onStartConversation={onStartConversation}
            onOpenExistingConversation={onSelectConversation}
            isStartingConversation={isStartingConversation}
          />
        </div>

        {error ? (
          <div className="mt-2 rounded-lg border border-red-100 bg-red-50 px-2 py-1.5">
            <p className="text-xs text-red-600">{error}</p>
          </div>
        ) : null}
      </div>


      <div className="chat-sidebar-list flex-1 overflow-y-auto">
        {isLoading ? (
          <div className="space-y-1 p-2">
            {[0, 1, 2, 3, 4].map((i) => (
              <div key={i} className="flex items-center gap-3 rounded-xl px-3 py-3">
                <div className="h-11 w-11 animate-pulse rounded-full bg-slate-100" />
                <div className="flex-1 space-y-2">
                  <div className="h-3.5 w-3/5 animate-pulse rounded bg-slate-100" />
                  <div className="h-3 w-4/5 animate-pulse rounded bg-slate-50" />
                  <div className="h-2.5 w-2/5 animate-pulse rounded bg-slate-50" />
                </div>
              </div>
            ))}
          </div>
        ) : null}

        {!isLoading && sortedConversations.length === 0 ? (
          <div className="px-6 py-12 text-center">
            <div className="chat-empty-orb mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full">
              <MessageSquare className="h-7 w-7" />
            </div>
            <p className="chat-text-strong mb-1 text-sm font-medium">
              {t("chat.messages.noConversations", { defaultValue: "No conversations yet" })}
            </p>
            <p className="text-xs leading-relaxed text-slate-500">
              {t("chat.messages.searchToStart", {
                defaultValue: "Search for a contact above to start chatting.",
              })}
            </p>
          </div>
        ) : null}

        {sortedConversations.length > 0 ? (
          <div className="space-y-0.5 p-1.5">
            {sortedConversations.map((conversation) => (
              <ChatConversationItem
                key={conversation.id}
                conversation={conversation}
                isSelected={selectedConversationId === conversation.id}
                onSelect={() => onSelectConversation(conversation.id)}
              />
            ))}
          </div>
        ) : null}
      </div>
    </aside>
  );
}
