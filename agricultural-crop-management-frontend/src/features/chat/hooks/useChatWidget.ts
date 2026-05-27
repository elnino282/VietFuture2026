import { useCallback, useEffect, useMemo, useState } from "react";
import { mockChatWidgetService } from "../api/mockChatWidgetService";
import { getTransactionStatusLabel } from "../lib/chatDisplayHelpers";
import type {
  ChatWidgetConversation,
  ChatWidgetFilter,
  ChatWidgetMessage,
  ChatWidgetService,
} from "../model/widgetTypes";

type UseChatWidgetOptions = {
  service?: ChatWidgetService;
};

export type UseChatWidgetResult = {
  conversations: ChatWidgetConversation[];
  filteredConversations: ChatWidgetConversation[];
  selectedConversation: ChatWidgetConversation | null;
  selectedConversationId: string | null;
  messages: ChatWidgetMessage[];
  searchQuery: string;
  filter: ChatWidgetFilter;
  totalUnreadCount: number;
  isLoading: boolean;
  isMessagesLoading: boolean;
  isSending: boolean;
  error: string | null;
  messagesError: string | null;
  setSearchQuery: (query: string) => void;
  setFilter: (filter: ChatWidgetFilter) => void;
  selectConversation: (conversationId: string) => Promise<void>;
  clearSelectedConversation: () => void;
  sendMessage: (content: string) => Promise<void>;
};

function getErrorMessage(error: unknown, fallback: string): string {
  if (error instanceof Error && error.message) {
    return error.message;
  }
  return fallback;
}

function sortConversations(conversations: ChatWidgetConversation[]): ChatWidgetConversation[] {
  return [...conversations].sort(
    (left, right) => right.lastMessageAt.getTime() - left.lastMessageAt.getTime(),
  );
}

function matchesSearch(conversation: ChatWidgetConversation, searchQuery: string): boolean {
  const normalizedQuery = searchQuery.trim().toLowerCase();
  if (!normalizedQuery) {
    return true;
  }

  const searchableText = [
    conversation.farmName,
    conversation.sellerName,
    conversation.region,
    conversation.lastMessage,
    conversation.context.title,
    conversation.context.subtitle,
    conversation.context.traceCode,
    getTransactionStatusLabel(conversation.context.transactionStatus),
  ]
    .join(" ")
    .toLowerCase();

  return searchableText.includes(normalizedQuery);
}

export function useChatWidget({
  service = mockChatWidgetService,
}: UseChatWidgetOptions = {}): UseChatWidgetResult {
  const [conversations, setConversations] = useState<ChatWidgetConversation[]>([]);
  const [selectedConversationId, setSelectedConversationId] = useState<string | null>(null);
  const [messages, setMessages] = useState<ChatWidgetMessage[]>([]);
  const [searchQuery, setSearchQuery] = useState("");
  const [filter, setFilter] = useState<ChatWidgetFilter>("all");
  const [isLoading, setIsLoading] = useState(true);
  const [isMessagesLoading, setIsMessagesLoading] = useState(false);
  const [isSending, setIsSending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [messagesError, setMessagesError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    async function loadConversations() {
      setIsLoading(true);
      setError(null);

      try {
        const loadedConversations = await service.getConversations();
        if (!cancelled) {
          setConversations(sortConversations(loadedConversations));
        }
      } catch (loadError) {
        if (!cancelled) {
          setError(getErrorMessage(loadError, "Unable to load chat conversations."));
        }
      } finally {
        if (!cancelled) {
          setIsLoading(false);
        }
      }
    }

    void loadConversations();

    return () => {
      cancelled = true;
    };
  }, [service]);

  const selectedConversation = useMemo(
    () =>
      conversations.find((conversation) => conversation.id === selectedConversationId) ??
      null,
    [conversations, selectedConversationId],
  );

  const totalUnreadCount = useMemo(
    () => conversations.reduce((sum, conversation) => sum + conversation.unreadCount, 0),
    [conversations],
  );

  const filteredConversations = useMemo(() => {
    return conversations.filter((conversation) => {
      if (filter === "unread" && conversation.unreadCount <= 0) {
        return false;
      }
      return matchesSearch(conversation, searchQuery);
    });
  }, [conversations, filter, searchQuery]);

  const markConversationReadLocally = useCallback((conversationId: string) => {
    setConversations((current) =>
      current.map((conversation) =>
        conversation.id === conversationId
          ? { ...conversation, unreadCount: 0 }
          : conversation,
      ),
    );
  }, []);

  const selectConversation = useCallback(
    async (conversationId: string) => {
      setSelectedConversationId(conversationId);
      setIsMessagesLoading(true);
      setMessagesError(null);
      markConversationReadLocally(conversationId);

      try {
        await service.markConversationAsRead(conversationId);
        const loadedMessages = await service.getConversationMessages(conversationId);
        setMessages(loadedMessages);
      } catch (loadMessagesError) {
        setMessagesError(
          getErrorMessage(loadMessagesError, "Unable to load chat messages."),
        );
      } finally {
        setIsMessagesLoading(false);
      }
    },
    [markConversationReadLocally, service],
  );

  const clearSelectedConversation = useCallback(() => {
    setSelectedConversationId(null);
    setMessages([]);
    setMessagesError(null);
  }, []);

  const sendMessage = useCallback(
    async (content: string) => {
      const trimmedContent = content.trim();
      if (!trimmedContent || !selectedConversationId) {
        return;
      }

      setIsSending(true);
      setMessagesError(null);

      try {
        const sentMessage = await service.sendMessage(
          selectedConversationId,
          trimmedContent,
        );
        setMessages((current) => [...current, sentMessage]);
        setConversations((current) =>
          sortConversations(
            current.map((conversation) =>
              conversation.id === selectedConversationId
                ? {
                    ...conversation,
                    lastMessage: trimmedContent,
                    lastMessageAt: sentMessage.sentAt,
                    unreadCount: 0,
                  }
                : conversation,
            ),
          ),
        );
      } catch (sendError) {
        setMessagesError(getErrorMessage(sendError, "Unable to send message."));
      } finally {
        setIsSending(false);
      }
    },
    [selectedConversationId, service],
  );

  return useMemo(
    () => ({
      conversations,
      filteredConversations,
      selectedConversation,
      selectedConversationId,
      messages,
      searchQuery,
      filter,
      totalUnreadCount,
      isLoading,
      isMessagesLoading,
      isSending,
      error,
      messagesError,
      setSearchQuery,
      setFilter,
      selectConversation,
      clearSelectedConversation,
      sendMessage,
    }),
    [
      clearSelectedConversation,
      conversations,
      error,
      filter,
      filteredConversations,
      isLoading,
      isMessagesLoading,
      isSending,
      messages,
      messagesError,
      searchQuery,
      selectConversation,
      selectedConversation,
      selectedConversationId,
      sendMessage,
      totalUnreadCount,
    ],
  );
}
