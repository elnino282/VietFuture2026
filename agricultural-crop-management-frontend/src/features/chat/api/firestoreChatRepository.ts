import type { MarkReadInput, SendChatMessageInput } from "../model/types";
import {
  buildDirectConversationId,
  ensureDirectConversation as ensureDirectConversationApi,
  markAsRead,
  sendMessage,
  setTypingState,
  subscribeConversations,
  subscribeMessages,
  subscribeParticipantState,
  toInternalUserId,
  toFirebaseChatUid,
} from "./chatFirestoreApi";

export {
  buildDirectConversationId,
  subscribeConversations,
  subscribeMessages,
  subscribeParticipantState,
  setTypingState,
  toInternalUserId,
  toFirebaseChatUid,
};

export async function ensureDirectConversation(params: {
  currentUid: string;
  currentRole: string | null;
  peerUid: string;
}): Promise<string> {
  return ensureDirectConversationApi({
    currentUid: params.currentUid,
    currentRole: params.currentRole,
    otherUid: params.peerUid,
  });
}

export async function sendDirectMessage(params: {
  currentUid: string;
  currentRole: string | null;
  input: SendChatMessageInput;
}): Promise<{ conversationId: string; seq: number }> {
  return sendMessage({
    currentUid: params.currentUid,
    currentRole: params.currentRole,
    conversationId: params.input.conversationId,
    otherUid: params.input.peerUid,
    text: params.input.text,
  });
}

export async function markConversationRead(params: {
  currentUid: string;
  input: MarkReadInput;
}): Promise<void> {
  await markAsRead({
    conversationId: params.input.conversationId,
    uid: params.currentUid,
    lastReadSeq: params.input.lastReadSeq,
  });
}
