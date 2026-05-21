import {
  collection,
  doc,
  getDoc,
  onSnapshot,
  orderBy,
  query,
  runTransaction,
  serverTimestamp,
  setDoc,
  Timestamp,
  writeBatch,
  where,
  type Firestore,
} from "firebase/firestore";
import { firestoreDb } from "@/shared/lib/firebase/firebaseApp";
import type {
  ChatConversation,
  ChatMessage,
  ChatParticipantState,
  MarkReadInput,
  SendChatMessageInput,
} from "../model/types";

const CONVERSATIONS_COLLECTION = "conversations";
const PARTICIPANTS_SUBCOLLECTION = "participants";
const MESSAGES_SUBCOLLECTION = "messages";
const MAX_MESSAGE_LENGTH = 2000;
const TYPING_TTL_MS = 4500;

type SubscribeConversationParams = {
  currentUid: string;
  onData: (conversations: ChatConversation[]) => void;
  onError?: (error: Error) => void;
};

type SubscribeMessagesParams = {
  conversationId: string;
  onData: (messages: ChatMessage[]) => void;
  onError?: (error: Error) => void;
};

type SubscribeParticipantStateParams = {
  conversationId: string;
  uid: string;
  onData: (state: ChatParticipantState) => void;
  onError?: (error: Error) => void;
};

type EnsureDirectConversationParams = {
  currentUid: string;
  currentRole?: string | null;
  otherUid: string;
  otherRole?: string | null;
};

type SendMessageParams = {
  currentUid: string;
  currentRole?: string | null;
  conversationId?: string | null;
  otherUid?: string | null;
  text: string;
};

type MarkAsReadParams = {
  conversationId: string;
  uid: string;
  lastReadSeq: number;
};

type SetTypingStateParams = {
  conversationId: string;
  uid: string;
  isTyping: boolean;
};

function requireFirestore(): Firestore {
  if (!firestoreDb) {
    throw new Error("Firebase chat is disabled or missing Firebase configuration.");
  }
  return firestoreDb;
}

function safeNumber(value: unknown, fallback = 0): number {
  if (typeof value === "number" && Number.isFinite(value)) {
    return value;
  }
  return fallback;
}

function safeString(value: unknown, fallback = ""): string {
  if (typeof value === "string") {
    return value;
  }
  return fallback;
}

function toDateOrNull(value: unknown): Date | null {
  if (!value || typeof value !== "object") {
    return null;
  }

  const timestamp = value as { toDate?: () => Date };
  if (typeof timestamp.toDate !== "function") {
    return null;
  }

  try {
    return timestamp.toDate();
  } catch {
    return null;
  }
}

function toParticipantState(data: Record<string, unknown> | null): ChatParticipantState {
  const typingUntil = toDateOrNull(data?.typingUntil);
  return {
    lastReadSeq: safeNumber(data?.lastReadSeq, 0),
    lastReadAt: toDateOrNull(data?.lastReadAt),
    typingUntil,
    isTyping: Boolean(typingUntil && typingUntil.getTime() > Date.now()),
  };
}

function normalizeParticipantIds(value: unknown): string[] {
  if (!Array.isArray(value)) {
    return [];
  }

  return value.filter((item): item is string => typeof item === "string");
}

function normalizeRole(role: string | null | undefined): string {
  const raw = String(role ?? "").trim();
  if (!raw) {
    return "UNKNOWN";
  }

  if (raw.toUpperCase().startsWith("ROLE_")) {
    return raw.slice("ROLE_".length).toUpperCase();
  }

  return raw.toUpperCase();
}

function normalizeMessageText(text: string): string {
  const normalized = text.trim();
  if (!normalized) {
    throw new Error("Message text cannot be empty.");
  }

  return normalized.length > MAX_MESSAGE_LENGTH
    ? normalized.slice(0, MAX_MESSAGE_LENGTH)
    : normalized;
}

function normalizeNumericUserIdSegment(value: string): string {
  const compact = value.replace(/^0+/, "");
  return compact || "0";
}

export function toInternalUserId(value: string | null | undefined): number | null {
  if (!value) {
    return null;
  }

  const match = /^u_(\d+)$/i.exec(value.trim());
  if (!match) {
    return null;
  }

  const numericId = Number.parseInt(match[1], 10);
  if (!Number.isInteger(numericId) || numericId <= 0) {
    return null;
  }

  return numericId;
}

export function toFirebaseChatUid(userId: string | number): string {
  const normalized = String(userId).trim();
  if (!normalized) {
    throw new Error("User id is required.");
  }

  const withoutHashPrefix = normalized.startsWith("#")
    ? normalized.slice(1).trim()
    : normalized;

  const prefixedMatch = /^u_(\d+)$/i.exec(withoutHashPrefix);
  if (prefixedMatch) {
    const numericId = normalizeNumericUserIdSegment(prefixedMatch[1]);
    if (numericId === "0") {
      throw new Error("User id must be a positive number.");
    }
    return `u_${numericId}`;
  }

  const plainMatch = /^(\d+)$/.exec(withoutHashPrefix);
  if (plainMatch) {
    const numericId = normalizeNumericUserIdSegment(plainMatch[1]);
    if (numericId === "0") {
      throw new Error("User id must be a positive number.");
    }
    return `u_${numericId}`;
  }

  throw new Error("Invalid user id format. Use internal numeric user id (example: 2).");
}

export function buildDirectConversationId(uidA: string, uidB: string): string {
  return [uidA, uidB].sort((left, right) => left.localeCompare(right)).join("__");
}

export async function ensureDirectConversation({
  currentUid,
  currentRole,
  otherUid,
  otherRole: _otherRole,
}: EnsureDirectConversationParams): Promise<string> {
  const db = requireFirestore();
  const normalizedCurrentUid = toFirebaseChatUid(currentUid);
  const normalizedOtherUid = toFirebaseChatUid(otherUid);

  if (normalizedOtherUid === normalizedCurrentUid) {
    throw new Error("Cannot create direct conversation with yourself.");
  }

  const conversationId = buildDirectConversationId(
    normalizedCurrentUid,
    normalizedOtherUid
  );
  const conversationRef = doc(db, CONVERSATIONS_COLLECTION, conversationId);
  const currentParticipantRef = doc(
    conversationRef,
    PARTICIPANTS_SUBCOLLECTION,
    normalizedCurrentUid
  );

  // Do not read conversation before write.
  // Rules deny reading a conversation that does not exist yet, which causes
  // BatchGetDocuments permission-denied on first direct chat creation.
  const participantIds = [normalizedCurrentUid, normalizedOtherUid].sort((left, right) =>
    left.localeCompare(right)
  );
  const batch = writeBatch(db);

  batch.set(
    conversationRef,
    {
      type: "direct",
      participantIds,
    },
    { merge: true }
  );

  batch.set(
    currentParticipantRef,
    {
      roleSnapshot: normalizeRole(currentRole),
      archived: false,
    },
    { merge: true }
  );

  await batch.commit();

  return conversationId;
}

export function subscribeConversations({
  currentUid,
  onData,
  onError,
}: SubscribeConversationParams): () => void {
  const db = requireFirestore();
  const normalizedCurrentUid = toFirebaseChatUid(currentUid);
  const conversationQuery = query(
    collection(db, CONVERSATIONS_COLLECTION),
    where("participantIds", "array-contains", normalizedCurrentUid)
  );

  return onSnapshot(
    conversationQuery,
    async (snapshot) => {
      try {
        const conversations = await Promise.all(
          snapshot.docs.map(async (conversationDoc) => {
            const data = conversationDoc.data();
            const participantIds = normalizeParticipantIds(data.participantIds);
            const peerUid =
              participantIds.find(
                (participantId) => participantId !== normalizedCurrentUid
              ) ?? normalizedCurrentUid;

            const participantRef = doc(
              conversationDoc.ref,
              PARTICIPANTS_SUBCOLLECTION,
              normalizedCurrentUid
            );
            let participantData: Record<string, unknown> | null = null;
            try {
              const participantSnapshot = await getDoc(participantRef);
              participantData = participantSnapshot.exists()
                ? participantSnapshot.data()
                : null;
            } catch {
              // Keep conversation list functional even when participant metadata is temporarily unavailable.
              participantData = null;
            }

            const lastSeq = safeNumber(data.lastSeq, 0);
            const lastReadSeq = safeNumber(participantData?.lastReadSeq, 0);

            const row: ChatConversation = {
              id: conversationDoc.id,
              type: "direct",
              participantIds,
              peerUid,
              peerUserId: toInternalUserId(peerUid),
              peerProfile: null,
              lastMessageText: safeString(data.lastMessageText),
              lastMessageAt: toDateOrNull(data.lastMessageAt),
              lastMessageSenderUid: data.lastMessageSenderUid
                ? safeString(data.lastMessageSenderUid)
                : null,
              lastSeq,
              lastReadSeq,
              unreadCount: Math.max(0, lastSeq - lastReadSeq),
            };

            return row;
          })
        );

        onData(conversations);
      } catch (error) {
        onError?.(error as Error);
      }
    },
    (error) => {
      onError?.(error as Error);
    }
  );
}

export function subscribeMessages({
  conversationId,
  onData,
  onError,
}: SubscribeMessagesParams): () => void {
  const db = requireFirestore();
  const messageQuery = query(
    collection(db, CONVERSATIONS_COLLECTION, conversationId, MESSAGES_SUBCOLLECTION),
    orderBy("seq", "asc")
  );

  return onSnapshot(
    messageQuery,
    (snapshot) => {
      const messages: ChatMessage[] = snapshot.docs.map((messageDoc) => {
        const data = messageDoc.data();
        return {
          id: messageDoc.id,
          seq: safeNumber(data.seq, 0),
          senderUid: safeString(data.senderUid),
          text: safeString(data.text),
          createdAt: toDateOrNull(data.createdAt),
          status: "sent",
        };
      });

      onData(messages);
    },
    (error) => {
      onError?.(error as Error);
    }
  );
}

export function subscribeParticipantState({
  conversationId,
  uid,
  onData,
  onError,
}: SubscribeParticipantStateParams): () => void {
  const db = requireFirestore();
  const normalizedUid = toFirebaseChatUid(uid);
  const participantRef = doc(
    db,
    CONVERSATIONS_COLLECTION,
    conversationId,
    PARTICIPANTS_SUBCOLLECTION,
    normalizedUid
  );

  return onSnapshot(
    participantRef,
    (snapshot) => {
      onData(toParticipantState(snapshot.exists() ? snapshot.data() : null));
    },
    (error) => {
      onError?.(error as Error);
    }
  );
}

export async function sendMessage({
  currentUid,
  currentRole,
  conversationId,
  otherUid,
  text,
}: SendMessageParams): Promise<{ conversationId: string; seq: number }> {
  const db = requireFirestore();
  const normalizedCurrentUid = toFirebaseChatUid(currentUid);
  const normalizedText = normalizeMessageText(text);

  let targetConversationId = conversationId ?? null;
  if (!targetConversationId) {
    if (!otherUid) {
      throw new Error("Conversation target is required.");
    }

    targetConversationId = await ensureDirectConversation({
      currentUid: normalizedCurrentUid,
      currentRole,
      otherUid,
    });
  }

  const conversationRef = doc(db, CONVERSATIONS_COLLECTION, targetConversationId);
  const senderParticipantRef = doc(
    conversationRef,
    PARTICIPANTS_SUBCOLLECTION,
    normalizedCurrentUid
  );

  const nextSeq = await runTransaction(db, async (transaction) => {
    const conversationSnapshot = await transaction.get(conversationRef);
    if (!conversationSnapshot.exists()) {
      throw new Error("Conversation does not exist.");
    }

    const conversationData = conversationSnapshot.data();
    const participantIds = normalizeParticipantIds(conversationData.participantIds);

    if (!participantIds.includes(normalizedCurrentUid)) {
      throw new Error("Current user is not a participant of this conversation.");
    }

    const lastSeq = safeNumber(conversationData.lastSeq, 0);
    const seq = lastSeq + 1;
    const messageRef = doc(collection(conversationRef, MESSAGES_SUBCOLLECTION));

    transaction.set(messageRef, {
      seq,
      senderUid: normalizedCurrentUid,
      text: normalizedText,
      createdAt: serverTimestamp(),
      status: "sent",
    });

    transaction.set(
      senderParticipantRef,
      {
        roleSnapshot: normalizeRole(currentRole),
        lastReadSeq: seq,
        lastReadAt: serverTimestamp(),
      },
      { merge: true }
    );

    transaction.set(
      conversationRef,
      {
        type: "direct",
        participantIds,
        lastSeq: seq,
        lastMessageText: normalizedText,
        lastMessageAt: serverTimestamp(),
        lastMessageSenderUid: normalizedCurrentUid,
      },
      { merge: true }
    );

    return seq;
  });

  return {
    conversationId: targetConversationId,
    seq: nextSeq,
  };
}

export async function markAsRead({
  conversationId,
  uid,
  lastReadSeq,
}: MarkAsReadParams): Promise<void> {
  const db = requireFirestore();
  const normalizedUid = toFirebaseChatUid(uid);
  const participantRef = doc(
    db,
    CONVERSATIONS_COLLECTION,
    conversationId,
    PARTICIPANTS_SUBCOLLECTION,
    normalizedUid
  );

  await setDoc(
    participantRef,
    {
      lastReadSeq: Math.max(0, lastReadSeq),
      lastReadAt: serverTimestamp(),
    },
    { merge: true }
  );
}

export async function setTypingState({
  conversationId,
  uid,
  isTyping,
}: SetTypingStateParams): Promise<void> {
  const db = requireFirestore();
  const normalizedUid = toFirebaseChatUid(uid);
  const participantRef = doc(
    db,
    CONVERSATIONS_COLLECTION,
    conversationId,
    PARTICIPANTS_SUBCOLLECTION,
    normalizedUid
  );

  await setDoc(
    participantRef,
    {
      typingUntil: isTyping ? Timestamp.fromMillis(Date.now() + TYPING_TTL_MS) : null,
      typingAt: isTyping ? serverTimestamp() : null,
    },
    { merge: true }
  );
}

export async function markConversationRead(input: {
  currentUid: string;
  payload: MarkReadInput;
}): Promise<void> {
  await markAsRead({
    conversationId: input.payload.conversationId,
    uid: input.currentUid,
    lastReadSeq: input.payload.lastReadSeq,
  });
}

export async function sendDirectMessage(input: {
  currentUid: string;
  currentRole?: string | null;
  payload: SendChatMessageInput;
}): Promise<{ conversationId: string; seq: number }> {
  return sendMessage({
    currentUid: input.currentUid,
    currentRole: input.currentRole,
    conversationId: input.payload.conversationId,
    otherUid: input.payload.peerUid,
    text: input.payload.text,
  });
}
