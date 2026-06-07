import { Check } from "lucide-react";
import { cn } from "@/shared/lib";
import { formatMessageTime } from "../lib/chatDisplayHelpers";
import type { ChatMessage } from "../model/types";

type ChatWidgetMessageBubbleProps = {
  message: ChatMessage;
  currentUid: string | null;
  peerLabel?: string;
};

export function ChatWidgetMessageBubble({
  message,
  currentUid,
  peerLabel,
}: ChatWidgetMessageBubbleProps) {
  const isMine = currentUid === message.senderUid;
  const senderLabel = isMine ? "Ban" : (peerLabel ?? "Nguoi ban");

  return (
    <div className={cn("chat-widget-message", isMine && "chat-widget-message--mine")}>
      <span className="chat-widget-message__sender">{senderLabel}</span>
      <div className="chat-widget-message__bubble">
        <p>{message.text}</p>
      </div>
      <div className="chat-widget-message__meta">
        <span>{formatMessageTime(message.createdAt)}</span>
        {isMine ? (
          <span aria-label="Sent" className="inline-flex items-center">
            <Check className="h-3 w-3" aria-hidden="true" />
          </span>
        ) : null}
      </div>
    </div>
  );
}
