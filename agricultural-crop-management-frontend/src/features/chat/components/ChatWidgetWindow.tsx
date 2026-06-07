import type { RefObject } from "react";
import { ArrowLeft } from "lucide-react";
import {
  getChatDisplayName,
  getChatSubtitle,
  joinDefinedParts,
} from "../lib/chatDisplayHelpers";
import type { ChatConversation, ChatMessage } from "../model/types";
import { ChatWidgetEmptyState } from "./ChatWidgetEmptyState";
import { ChatWidgetInput } from "./ChatWidgetInput";
import { ChatWidgetMessageBubble } from "./ChatWidgetMessageBubble";

type ChatWidgetWindowProps = {
  conversation: ChatConversation | null;
  messages: ChatMessage[];
  currentUid: string | null;
  isMessagesLoading: boolean;
  isSending: boolean;
  error: string | null;
  onBack: () => void;
  onSend: (content: string) => Promise<void>;
  bottomRef: RefObject<HTMLDivElement>;
};

function getAvatarLabel(conversation: ChatConversation): string {
  return getChatDisplayName(conversation.peerProfile, conversation.peerUid)
    .slice(0, 1)
    .toUpperCase();
}

export function ChatWidgetWindow({
  conversation,
  messages,
  currentUid,
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

  const profile = conversation.peerProfile;
  const displayName = getChatDisplayName(profile, conversation.peerUid);
  const subtitle = joinDefinedParts([getChatSubtitle(profile), profile?.address], " - ");

  return (
    <main className="chat-widget-window">
      <header className="chat-widget-thread-header">
        <button
          type="button"
          className="chat-widget-back"
          aria-label="Back to conversations"
          onClick={onBack}
        >
          <ArrowLeft className="h-4 w-4" aria-hidden="true" />
        </button>

        <div className="chat-widget-thread-header__identity">
          <div className="chat-widget-avatar chat-widget-avatar--sm" aria-hidden="true">
            {profile?.avatarUrl ? (
              <img src={profile.avatarUrl} alt="" referrerPolicy="no-referrer" />
            ) : (
              getAvatarLabel(conversation)
            )}
          </div>

          <div className="min-w-0">
            <h3 className="chat-widget-thread-header__name">
              <span>{displayName}</span>
            </h3>
            {subtitle ? <p>{subtitle}</p> : null}
          </div>
        </div>
      </header>

      <div className="chat-widget-messages" aria-live="polite">
        {isMessagesLoading ? <p className="chat-widget-muted">Dang tai tin nhan...</p> : null}
        {error ? <p className="chat-widget-error">{error}</p> : null}
        {!isMessagesLoading && !error && messages.length === 0 ? (
          <p className="chat-widget-muted">Chua co tin nhan nao.</p>
        ) : null}
        {messages.map((message) => (
          <ChatWidgetMessageBubble
            key={message.id}
            message={message}
            currentUid={currentUid}
            peerLabel={displayName}
          />
        ))}
        <div ref={bottomRef} />
      </div>

      <ChatWidgetInput disabled={!conversation} isSending={isSending} onSend={onSend} />
    </main>
  );
}
