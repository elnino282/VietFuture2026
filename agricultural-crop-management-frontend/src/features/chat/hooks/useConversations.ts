import { useCallback, useEffect, useMemo, useState } from "react";
import {
  ensureDirectConversation,
  subscribeConversations,
  toFirebaseChatUid,
} from "../api/firestoreChatRepository";
import { searchChatContacts } from "../api/chatContactApi";
import type { ChatContactProfile, ChatConversation } from "../model/types";

type UseConversationsResult = {
  conversations: ChatConversation[];
  isLoading: boolean;
  hasLoadedConversations: boolean;
  error: string | null;
  isStartingConversation: boolean;
  startDirectConversation: (peerUserId: number) => Promise<string>;
};

function getErrorMessage(error: unknown): string {
  const maybeFirebaseError = error as { code?: string; message?: string } | null;
  if (maybeFirebaseError?.code === "permission-denied") {
    return "Missing or insufficient permissions.";
  }

  if (error instanceof Error && error.message) {
    return error.message;
  }

  return "Unable to load conversations.";
}

export function useConversations(
  currentUid: string | null,
  currentRole: string | null
): UseConversationsResult {
  const [conversations, setConversations] = useState<ChatConversation[]>([]);
  const [peerProfileByUserId, setPeerProfileByUserId] = useState<Record<number, ChatContactProfile>>({});
  const [isLoading, setIsLoading] = useState(false);
  const [hasLoadedConversations, setHasLoadedConversations] = useState(false);
  const [isStartingConversation, setIsStartingConversation] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!currentUid) {
      setConversations([]);
      setPeerProfileByUserId({});
      setIsLoading(false);
      setHasLoadedConversations(false);
      setError(null);
      return;
    }

    setIsLoading(true);
    setHasLoadedConversations(false);
    setError(null);

    const unsubscribe = subscribeConversations({
      currentUid,
      onData: (data) => {
        setConversations(data);
        setIsLoading(false);
        setHasLoadedConversations(true);
      },
      onError: (subscribeError) => {
        setError(getErrorMessage(subscribeError));
        setIsLoading(false);
      },
    });

    return () => {
      unsubscribe();
    };
  }, [currentUid]);

  useEffect(() => {
    if (!currentUid || conversations.length === 0) {
      return;
    }

    const unresolvedPeerIds = [...new Set(
      conversations
        .map((conversation) => conversation.peerUserId)
        .filter((peerUserId): peerUserId is number =>
          typeof peerUserId === "number" &&
          peerUserId > 0 &&
          !(peerUserId in peerProfileByUserId)
        )
    )];

    if (unresolvedPeerIds.length === 0) {
      return;
    }

    let cancelled = false;

    void searchChatContacts({
      userIds: unresolvedPeerIds,
      limit: unresolvedPeerIds.length,
    })
      .then((profiles) => {
        if (cancelled || profiles.length === 0) {
          return;
        }

        setPeerProfileByUserId((prev) => {
          const next = { ...prev };
          profiles.forEach((profile) => {
            next[profile.userId] = profile;
          });
          return next;
        });
      })
      .catch(() => {
        // Conversation list still works without profile enrichment.
      });

    return () => {
      cancelled = true;
    };
  }, [conversations, currentUid, peerProfileByUserId]);

  const startDirectConversation = useCallback(
    async (peerUserId: number) => {
      if (!currentUid) {
        throw new Error("Chat is not ready yet.");
      }
      setIsStartingConversation(true);
      setError(null);

      try {
        const conversationId = await ensureDirectConversation({
          currentUid,
          currentRole,
          peerUid: toFirebaseChatUid(String(peerUserId)),
        });
        return conversationId;
      } catch (startError) {
        const mappedError = getErrorMessage(startError);
        setError(mappedError);
        throw startError;
      } finally {
        setIsStartingConversation(false);
      }
    },
    [currentRole, currentUid]
  );

  const enrichedConversations = useMemo(
    () =>
      conversations.map((conversation) => ({
        ...conversation,
        peerProfile:
          conversation.peerUserId && peerProfileByUserId[conversation.peerUserId]
            ? peerProfileByUserId[conversation.peerUserId]
            : null,
      })),
    [conversations, peerProfileByUserId]
  );

  return useMemo(
    () => ({
      conversations: enrichedConversations,
      isLoading,
      hasLoadedConversations,
      error,
      isStartingConversation,
      startDirectConversation,
    }),
    [
      enrichedConversations,
      error,
      hasLoadedConversations,
      isLoading,
      isStartingConversation,
      startDirectConversation,
    ]
  );
}
