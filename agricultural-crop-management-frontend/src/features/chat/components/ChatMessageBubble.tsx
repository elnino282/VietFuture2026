import { Check, CheckCheck } from "lucide-react";
import { useTranslation } from "react-i18next";
import { cn } from "@/shared/lib";
import { formatMessageTime } from "../lib/chatDisplayHelpers";
import type { ChatMessage } from "../model/types";

type ChatMessageBubbleProps = {
  message: ChatMessage;
  isMine: boolean;
  peerLabel: string;
  peerLastReadSeq?: number;
};

export function ChatMessageBubble({
  message,
  isMine,
  peerLabel,
  peerLastReadSeq = 0,
}: ChatMessageBubbleProps) {
  const { t } = useTranslation();
  const isReadByPeer = isMine && peerLastReadSeq >= message.seq;
  const statusLabel = isReadByPeer
    ? t("chat.receipts.read", { defaultValue: "Read" })
    : t("chat.receipts.sent", { defaultValue: "Sent" });

  return (
    <div
      data-message-id={message.id}
      className={cn("flex", isMine ? "justify-end" : "justify-start")}
    >
      <div
        className={cn(
          "chat-bubble px-4 py-2.5 text-sm",
          isMine ? "chat-bubble--mine" : "chat-bubble--peer"
        )}
      >
        {!isMine ? (
          <p className="chat-peer-name mb-1 text-[11px] font-semibold">{peerLabel}</p>
        ) : null}

        <p className="whitespace-pre-wrap break-words leading-relaxed">{message.text}</p>

        <p
          className={cn(
            "mt-1.5 flex items-center justify-end gap-1 text-right text-[10px]",
            isMine ? "chat-bubble-meta--mine" : "text-slate-500"
          )}
        >
          <span>{formatMessageTime(message.createdAt)}</span>
          {isMine ? (
            <>
              <span aria-hidden="true">·</span>
              <span className="inline-flex items-center gap-0.5">
                {isReadByPeer ? (
                  <CheckCheck className="h-3 w-3" aria-hidden="true" />
                ) : (
                  <Check className="h-3 w-3" aria-hidden="true" />
                )}
                {statusLabel}
              </span>
            </>
          ) : null}
        </p>
      </div>
    </div>
  );
}
