import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import {
  setTypingState,
  subscribeParticipantState,
} from "../api/firestoreChatRepository";
import type { ChatParticipantState } from "../model/types";

type UseChatRealtimeStateResult = {
  isPeerTyping: boolean;
  peerLastReadSeq: number;
  peerLastReadAt: Date | null;
  publishTypingState: (isTyping: boolean) => void;
  clearTypingState: () => void;
};

const EMPTY_PARTICIPANT_STATE: ChatParticipantState = {
  lastReadSeq: 0,
  lastReadAt: null,
  typingUntil: null,
  isTyping: false,
};

const TYPING_REFRESH_INTERVAL_MS = 2500;

function getErrorMessage(error: unknown): string {
  if (error instanceof Error && error.message) {
    return error.message;
  }
  return "Unable to update chat realtime state.";
}

export function useChatRealtimeState(params: {
  currentUid: string | null;
  conversationId: string | null;
  peerUid: string | null;
}): UseChatRealtimeStateResult {
  const { currentUid, conversationId, peerUid } = params;
  const [peerState, setPeerState] = useState<ChatParticipantState>(EMPTY_PARTICIPANT_STATE);
  const expiryTimerRef = useRef<number | null>(null);
  const lastTypingWriteAtRef = useRef(0);
  const lastPublishedTypingStateRef = useRef(false);

  useEffect(() => {
    if (expiryTimerRef.current) {
      window.clearTimeout(expiryTimerRef.current);
      expiryTimerRef.current = null;
    }

    if (!conversationId || !peerUid) {
      setPeerState(EMPTY_PARTICIPANT_STATE);
      return;
    }

    const unsubscribe = subscribeParticipantState({
      conversationId,
      uid: peerUid,
      onData: (nextState) => {
        if (expiryTimerRef.current) {
          window.clearTimeout(expiryTimerRef.current);
          expiryTimerRef.current = null;
        }

        setPeerState(nextState);

        if (nextState.typingUntil && nextState.typingUntil.getTime() > Date.now()) {
          expiryTimerRef.current = window.setTimeout(() => {
            setPeerState((current) => ({
              ...current,
              isTyping: false,
            }));
          }, nextState.typingUntil.getTime() - Date.now() + 50);
        }
      },
      onError: () => {
        setPeerState(EMPTY_PARTICIPANT_STATE);
      },
    });

    return () => {
      unsubscribe();
      if (expiryTimerRef.current) {
        window.clearTimeout(expiryTimerRef.current);
        expiryTimerRef.current = null;
      }
    };
  }, [conversationId, peerUid]);

  useEffect(() => {
    lastTypingWriteAtRef.current = 0;
    lastPublishedTypingStateRef.current = false;
  }, [conversationId, currentUid]);

  const publishTypingState = useCallback(
    (isTyping: boolean) => {
      if (!currentUid || !conversationId) {
        return;
      }

      const now = Date.now();
      const lastWriteAt = lastTypingWriteAtRef.current;
      const lastPublishedState = lastPublishedTypingStateRef.current;

      if (
        isTyping &&
        lastPublishedState &&
        now - lastWriteAt < TYPING_REFRESH_INTERVAL_MS
      ) {
        return;
      }

      if (!isTyping && !lastPublishedState) {
        return;
      }

      lastTypingWriteAtRef.current = now;
      lastPublishedTypingStateRef.current = isTyping;

      void setTypingState({
        conversationId,
        uid: currentUid,
        isTyping,
      }).catch((error) => {
        if (import.meta.env.DEV) {
          console.warn(getErrorMessage(error));
        }
      });
    },
    [conversationId, currentUid]
  );

  const clearTypingState = useCallback(() => {
    publishTypingState(false);
  }, [publishTypingState]);

  useEffect(() => {
    return () => {
      if (lastPublishedTypingStateRef.current && currentUid && conversationId) {
        void setTypingState({
          conversationId,
          uid: currentUid,
          isTyping: false,
        }).catch(() => undefined);
      }
    };
  }, [conversationId, currentUid]);

  return useMemo(
    () => ({
      isPeerTyping: peerState.isTyping,
      peerLastReadSeq: peerState.lastReadSeq,
      peerLastReadAt: peerState.lastReadAt,
      publishTypingState,
      clearTypingState,
    }),
    [
      clearTypingState,
      peerState.isTyping,
      peerState.lastReadAt,
      peerState.lastReadSeq,
      publishTypingState,
    ]
  );
}
