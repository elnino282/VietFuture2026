import { useState } from "react";
import { AlertCircle, Loader2, SendHorizonal } from "lucide-react";
import { useTranslation } from "react-i18next";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { cn } from "@/shared/lib";

type ChatComposerProps = {
  disabled: boolean;
  isSending: boolean;
  targetLabel?: string | null;
  onSend: (text: string) => Promise<void>;
  onTypingStateChange?: (isTyping: boolean) => void;
  error: string | null;
};

const MAX_CHARACTERS = 2000;

export function ChatComposer({
  disabled,
  isSending,
  targetLabel,
  onSend,
  onTypingStateChange,
  error,
}: ChatComposerProps) {
  const { t } = useTranslation();
  const [text, setText] = useState("");

  const handleSend = async () => {
    const trimmed = text.trim();
    if (!trimmed || disabled || isSending) {
      return;
    }

    onTypingStateChange?.(false);
    await onSend(trimmed);
    setText("");
  };

  const charCount = text.length;
  const isNearLimit = charCount > MAX_CHARACTERS * 0.9;

  return (
    <div className="chat-composer rounded-b-2xl border-t p-3">
      {error ? (
        <div className="mb-2 flex items-center gap-2 rounded-lg border border-red-100 bg-red-50 px-2 py-1.5">
          <AlertCircle className="h-3.5 w-3.5 shrink-0 text-red-500" />
          <p className="flex-1 text-xs text-red-600">{error}</p>
        </div>
      ) : null}

      <div className="flex items-end gap-2">
        <div className="relative flex-1">
          <Textarea
            value={text}
            onChange={(event) => {
              const nextValue = event.target.value;
              const normalizedValue =
                nextValue.length > MAX_CHARACTERS
                  ? nextValue.slice(0, MAX_CHARACTERS)
                  : nextValue;

              setText(normalizedValue);
              onTypingStateChange?.(Boolean(normalizedValue.trim()));
            }}
            onBlur={() => onTypingStateChange?.(false)}
            onKeyDown={(event) => {
              if (event.key === "Enter" && !event.shiftKey) {
                event.preventDefault();
                void handleSend();
              }
            }}
            placeholder={
              disabled
                ? t("chat.composer.selectConversationPlaceholder", {
                    defaultValue: "Select a conversation to start typing...",
                  })
                : targetLabel
                  ? t("chat.composer.messageTargetPlaceholder", {
                      defaultValue: "Message {{target}}...",
                      target: targetLabel,
                    })
                  : t("chat.composer.messagePlaceholder", {
                      defaultValue: "Type a message...",
                    })
            }
            aria-label={t("chat.composer.messageInputAria", {
              defaultValue: "Message input",
            })}
            rows={2}
            disabled={disabled || isSending}
            className={cn(
              "chat-input min-h-[60px] resize-none pr-14",
              disabled && "bg-slate-50"
            )}
          />
          <div className="absolute bottom-1.5 right-2">
            <span
              className={cn(
                "text-[10px]",
                isNearLimit ? "font-medium text-amber-500" : "text-slate-300"
              )}
            >
              {charCount}/{MAX_CHARACTERS}
            </span>
          </div>
        </div>

        <Button
          type="button"
          aria-label={t("chat.composer.sendAria", {
            defaultValue: "Send message",
          })}
          className={cn(
            "chat-send-button h-10 w-10 rounded-xl p-0 transition-all",
            disabled || isSending || !text.trim()
              ? "cursor-not-allowed bg-slate-200 text-slate-400"
              : "text-white shadow-sm hover:shadow-md"
          )}
          disabled={disabled || isSending || !text.trim()}
          onClick={() => {
            void handleSend();
          }}
        >
          {isSending ? (
            <Loader2 className="h-4 w-4 animate-spin" />
          ) : (
            <SendHorizonal className="h-4 w-4" />
          )}
        </Button>
      </div>
    </div>
  );
}
