import { Check } from "lucide-react";
import { cn } from "@/shared/lib";
import type { ChatWidgetMessage } from "../model/widgetTypes";

type ChatWidgetMessageBubbleProps = {
  message: ChatWidgetMessage;
  sellerName?: string;
};

function formatMessageTime(value: Date) {
  return value.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });
}

export function ChatWidgetMessageBubble({ message, sellerName }: ChatWidgetMessageBubbleProps) {
  const isMine = message.sender === "buyer";
  const senderLabel = isMine ? "Bạn" : (sellerName ?? "Người bán");

  return (
    <div className={cn("chat-widget-message", isMine && "chat-widget-message--mine")}>
      <span className="chat-widget-message__sender">{senderLabel}</span>
      <div className="chat-widget-message__bubble">
        <p>{message.content}</p>
      </div>
      <div className="chat-widget-message__meta">
        <span>{formatMessageTime(message.sentAt)}</span>
        {isMine ? (
          <span aria-label="Sent" className="inline-flex items-center">
            <Check className="h-3 w-3" aria-hidden="true" />
          </span>
        ) : null}
      </div>
    </div>
  );
}
