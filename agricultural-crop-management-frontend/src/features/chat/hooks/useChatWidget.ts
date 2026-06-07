import { useCallback, useMemo, useState } from "react";
import {
  formatRole,
  getChatDisplayName,
  getChatSubtitle,
} from "../lib/chatDisplayHelpers";
import { useChatBootstrap } from "../model/useChatBootstrap";
import type { ChatConversation, ChatMessage } from "../model/types";
import type { ChatWidgetFilter } from "../model/widgetTypes";
import { useChatRealtimeState } from "./useChatRealtimeState";
import { useConversations } from "./useConversations";
import { useMarkConversationRead } from "./useMarkConversationRead";
import { useMessages } from "./useMessages";
import { useSendMessage } from "./useSendMessage";

export type UseChatWidgetResult = {
  conversations: ChatConversation[];
  filteredConversations: ChatConversation[];
  selectedConversation: ChatConversation | null;
  selectedConversationId: string | null;
  messages: ChatMessage[];
  searchQuery: string;
  filter: ChatWidgetFilter;
  totalUnreadCount: number;
  isLoading: boolean;
  isMessagesLoading: boolean;
  isSending: boolean;
  isStartingConversation: boolean;
  error: string | null;
  messagesError: string | null;
  currentUid: string | null;
  setSearchQuery: (query: string) => void;
  setFilter: (filter: ChatWidgetFilter) => void;
  selectConversation: (conversationId: string) => Promise<void>;
  startConversation: (peerUserId: number) => Promise<void>;
  clearSelectedConversation: () => void;
  sendMessage: (content: string) => Promise<void>;
};

function sortConversations(conversations: ChatConversation[]): ChatConversation[] {
  return [...conversations].sort((left, right) => {
    const leftTime = left.lastMessageAt?.getTime() ?? 0;
    const rightTime = right.lastMessageAt?.getTime() ?? 0;
    return rightTime - leftTime;
  });
}

function getConversationSearchText(conversation: ChatConversation): string {
  const profile = conversation.peerProfile;
  return [
    getChatDisplayName(profile, conversation.peerUid),
    getChatSubtitle(profile),
    profile?.farmName,
    profile?.representativeName,
    profile?.address,
    profile?.role ? formatRole(profile.role) : null,
    conversation.lastMessageText,
    conversation.peerUid,
    conversation.peerUserId ? `#${conversation.peerUserId}` : null,
  ]
    .filter(Boolean)
    .join(" ")
    .toLowerCase();
}

function matchesSearch(conversation: ChatConversation, searchQuery: string): boolean {
  const normalizedQuery = searchQuery.trim().toLowerCase();
  if (!normalizedQuery) {
    return true;
  }

  return getConversationSearchText(conversation).includes(normalizedQuery);
}

function getBootstrapError(status: ReturnType<typeof useChatBootstrap>): string | null {
  if (status.status === "disabled" || status.status === "error") {
    return status.error;
  }
  return null;
}

export function useChatWidget(): UseChatWidgetResult {
  const bootstrap = useChatBootstrap();
  const currentUid = bootstrap.status === "ready" ? bootstrap.appUid : null;
  const currentRole = bootstrap.status === "ready" ? bootstrap.role : null;
  const [selectedConversationId, setSelectedConversationId] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState("");
  const [filter, setFilter] = useState<ChatWidgetFilter>("all");

  const {
    conversations,
    isLoading: isConversationsLoading,
    error: conversationsError,
    isStartingConversation,
    startDirectConversation,
  } = useConversations(currentUid, currentRole);

  const selectedConversation = useMemo(
    () =>
      conversations.find((conversation) => conversation.id === selectedConversationId) ??
      null,
    [conversations, selectedConversationId],
  );

  const messagesState = useMessages(currentUid, selectedConversationId);
  const sendState = useSendMessage(currentUid, currentRole);
  const markReadState = useMarkConversationRead(currentUid);

  const realtimeState = useChatRealtimeState({
    currentUid,
    conversationId: selectedConversationId,
    peerUid: selectedConversation?.peerUid ?? null,
  });

  const totalUnreadCount = useMemo(
    () => conversations.reduce((sum, conversation) => sum + conversation.unreadCount, 0),
    [conversations],
  );

  const filteredConversations = useMemo(() => {
    return sortConversations(conversations).filter((conversation) => {
      if (filter === "unread" && conversation.unreadCount <= 0) {
        return false;
      }
      return matchesSearch(conversation, searchQuery);
    });
  }, [conversations, filter, searchQuery]);

  const markSelectedConversationRead = useCallback(
    async (conversation: ChatConversation | null) => {
      if (!conversation) {
        return;
      }

      const latestSeq = Math.max(conversation.lastSeq, conversation.lastReadSeq);
      if (latestSeq <= 0 || latestSeq <= conversation.lastReadSeq) {
        return;
      }

      await markReadState.markRead({
        conversationId: conversation.id,
        lastReadSeq: latestSeq,
      });
    },
    [markReadState],
  );

  const selectConversation = useCallback(
    async (conversationId: string) => {
      realtimeState.clearTypingState();
      setSelectedConversationId(conversationId);
      const conversation =
        conversations.find((item) => item.id === conversationId) ?? null;
      await markSelectedConversationRead(conversation);
    },
    [conversations, markSelectedConversationRead, realtimeState],
  );

  const startConversation = useCallback(
    async (peerUserId: number) => {
      const conversationId = await startDirectConversation(peerUserId);
      realtimeState.clearTypingState();
      setSelectedConversationId(conversationId);
    },
    [realtimeState, startDirectConversation],
  );

  const clearSelectedConversation = useCallback(() => {
    realtimeState.clearTypingState();
    setSelectedConversationId(null);
  }, [realtimeState]);

  const sendMessage = useCallback(
    async (content: string) => {
      const trimmedContent = content.trim();
      if (!trimmedContent || !selectedConversationId) {
        return;
      }

      realtimeState.clearTypingState();
      await sendState.sendMessage({
        conversationId: selectedConversationId,
        text: trimmedContent,
      });
    },
    [realtimeState, selectedConversationId, sendState],
  );

  const bootstrapError = getBootstrapError(bootstrap);
  const error = bootstrapError ?? conversationsError;
  const messagesError = messagesState.error ?? sendState.error ?? markReadState.error;
  const isLoading = bootstrap.status === "loading" || isConversationsLoading;

  return useMemo(
    () => ({
      conversations,
      filteredConversations,
      selectedConversation,
      selectedConversationId,
      messages: messagesState.messages,
      searchQuery,
      filter,
      totalUnreadCount,
      isLoading,
      isMessagesLoading: messagesState.isLoading,
      isSending: sendState.isSending,
      isStartingConversation,
      error,
      messagesError,
      currentUid,
      setSearchQuery,
      setFilter,
      selectConversation,
      startConversation,
      clearSelectedConversation,
      sendMessage,
    }),
    [
      clearSelectedConversation,
      conversations,
      currentUid,
      error,
      filter,
      filteredConversations,
      isLoading,
      isStartingConversation,
      messagesError,
      messagesState.isLoading,
      messagesState.messages,
      searchQuery,
      selectConversation,
      selectedConversation,
      selectedConversationId,
      sendMessage,
      sendState.isSending,
      startConversation,
      totalUnreadCount,
    ],
  );
}
