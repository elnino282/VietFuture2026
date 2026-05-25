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

/**
 * ChatMessageBubble — renders a single message in the thread.
 *
 * Figma spec (node 324-107):
 *  - Outgoing: #d4f4dd background, dark text, rounded-[8.75px]
 *  - Incoming: white background, rgba(0,0,0,0.1) border
 *  - Timestamp: OUTSIDE the bubble (below it), color #717182, 10.5px
 *  - Read receipt: two check marks (lucide CheckCheck) next to timestamp
 */
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

  const timeString = formatMessageTime(message.createdAt);

  return (
    <div
      data-message-id={message.id}
      className={cn("flex flex-col", isMine ? "items-end" : "items-start")}
    >
      {/* Bubble */}
      <div
        className={cn(
          "chat-bubble px-[11.3px] py-[7.8px] text-[12.25px] leading-[17.5px]",
          isMine ? "chat-bubble--mine" : "chat-bubble--peer"
        )}
      >
        {/* Peer name label — only on incoming */}
        {!isMine ? (
          <p className="chat-peer-name mb-1 text-[11px] font-semibold">{peerLabel}</p>
        ) : null}

        <p className="whitespace-pre-wrap break-words">{message.text}</p>
      </div>

      {/* Timestamp + read receipt — OUTSIDE bubble (Figma spec) */}
      <div
        className={cn(
          "chat-bubble-timestamp mt-1 flex items-center gap-1",
          isMine ? "chat-bubble-timestamp--mine" : ""
        )}
      >
        <span>{timeString}</span>

        {isMine ? (
          <>
            <span aria-hidden="true">·</span>
            <span
              className="inline-flex items-center gap-0.5 text-[10.5px]"
              aria-label={statusLabel}
            >
              {isReadByPeer ? (
                <CheckCheck className="h-3 w-3" aria-hidden="true" />
              ) : (
                <Check className="h-3 w-3" aria-hidden="true" />
              )}
              <span className="sr-only">{statusLabel}</span>
            </span>
          </>
        ) : null}
      </div>
    </div>
  );
}
