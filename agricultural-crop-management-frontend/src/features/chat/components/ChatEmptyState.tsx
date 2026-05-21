import {
  ArrowRight,
  MessagesSquare,
  MessageSquare,
  Search,
  ShieldCheck,
  Sprout,
} from "lucide-react";
import { useTranslation } from "react-i18next";

type ChatEmptyStateProps = {
  variant: "no-conversation" | "no-messages";
};

export function ChatEmptyState({ variant }: ChatEmptyStateProps) {
  const { t } = useTranslation();
  const quickTips = [
    {
      icon: Search,
      title: t("chat.empty.searchContactsTitle", { defaultValue: "Search contacts" }),
      description: t("chat.empty.searchContactsDescription", {
        defaultValue: "Find farms, buyers, or representatives by name, address, or ID.",
      }),
    },
    {
      icon: ShieldCheck,
      title: t("chat.empty.verifyIdentityTitle", { defaultValue: "Verify identity" }),
      description: t("chat.empty.verifyIdentityDescription", {
        defaultValue: "Confirm the farm and representative before messaging.",
      }),
    },
    {
      icon: MessagesSquare,
      title: t("chat.empty.startMessagingTitle", { defaultValue: "Start messaging" }),
      description: t("chat.empty.startMessagingDescription", {
        defaultValue: "Discuss orders, pricing, delivery, or farm tasks securely.",
      }),
    },
  ];

  if (variant === "no-conversation") {
    return (
      <div className="relative flex h-full items-center justify-center overflow-hidden p-6">
        <div className="chat-empty-glow pointer-events-none absolute inset-0">
          <div className="chat-empty-glow__primary absolute left-1/4 top-1/4 h-64 w-64 rounded-full blur-3xl" />
          <div className="chat-empty-glow__secondary absolute bottom-1/4 right-1/4 h-48 w-48 rounded-full blur-3xl" />
        </div>

        <div className="relative z-10 max-w-sm text-center">
          <div className="relative mx-auto mb-6 h-24 w-24">
            <div className="chat-empty-pulse absolute inset-0 animate-pulse rounded-full opacity-40" />
            <div className="chat-empty-orb absolute inset-1 rounded-full" />
            <div className="absolute inset-0 flex items-center justify-center rounded-full">
              <Sprout className="h-10 w-10" />
            </div>
          </div>

          <h3 className="chat-text-strong mb-2 text-lg font-semibold">
            {t("chat.empty.selectConversationTitle", { defaultValue: "Select a conversation" })}
          </h3>

          <p className="mb-8 text-sm leading-relaxed text-slate-500">
            {t("chat.empty.selectConversationDescription", {
              defaultValue:
                "Choose a recent chat on the left, or search by farm, representative, address, or ID to start a new conversation.",
            })}
          </p>

          <div className="space-y-3">
            {quickTips.map((tip) => (
              <div
                key={tip.title}
                className="chat-tip-card flex items-start gap-3 rounded-xl border bg-white/80 p-3 text-left shadow-sm transition-colors"
              >
                <div className="chat-info-icon flex h-9 w-9 shrink-0 items-center justify-center rounded-lg">
                  <tip.icon className="h-4.5 w-4.5" />
                </div>
                <div className="min-w-0 flex-1">
                  <p className="chat-text-strong text-sm font-medium">{tip.title}</p>
                  <p className="mt-0.5 text-xs leading-relaxed text-slate-500">{tip.description}</p>
                </div>
                <ArrowRight className="mt-0.5 h-4 w-4 shrink-0 text-slate-300" />
              </div>
            ))}
          </div>

          <p className="mt-6 text-[11px] text-slate-400">
            {t("chat.empty.recentConversationsHint", {
              defaultValue: "Your recent conversations appear on the left panel.",
            })}
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="flex h-full items-center justify-center p-8">
      <div className="max-w-xs text-center">
        <div className="relative mx-auto mb-5 h-20 w-20">
          <div className="chat-empty-pulse absolute inset-0 animate-pulse rounded-full opacity-50" />
          <div className="chat-empty-orb absolute inset-2 flex items-center justify-center rounded-full">
            <MessageSquare className="h-8 w-8" />
          </div>
        </div>
        <h4 className="chat-text-strong mb-2 text-base font-semibold">
          {t("chat.empty.noMessagesTitle", { defaultValue: "No messages yet" })}
        </h4>
        <p className="text-sm leading-relaxed text-slate-500">
          {t("chat.empty.noMessagesDescription", {
            defaultValue: "Send the first message below to start the conversation.",
          })}
        </p>
      </div>
    </div>
  );
}
