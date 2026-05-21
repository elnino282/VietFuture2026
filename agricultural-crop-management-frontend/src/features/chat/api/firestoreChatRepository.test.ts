import { waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

const firestoreMocks = vi.hoisted(() => ({
  collection: vi.fn(),
  doc: vi.fn(),
  getDoc: vi.fn(),
  onSnapshot: vi.fn(),
  orderBy: vi.fn(),
  query: vi.fn(),
  runTransaction: vi.fn(),
  serverTimestamp: vi.fn(),
  setDoc: vi.fn(),
  Timestamp: {
    fromMillis: vi.fn((millis: number) => ({ toDate: () => new Date(millis) })),
  },
  where: vi.fn(),
}));

const firebaseRuntimeMocks = vi.hoisted(() => ({
  firestoreDb: { db: "mock" },
}));

vi.mock("firebase/firestore", () => ({
  collection: firestoreMocks.collection,
  doc: firestoreMocks.doc,
  getDoc: firestoreMocks.getDoc,
  onSnapshot: firestoreMocks.onSnapshot,
  orderBy: firestoreMocks.orderBy,
  query: firestoreMocks.query,
  runTransaction: firestoreMocks.runTransaction,
  serverTimestamp: firestoreMocks.serverTimestamp,
  setDoc: firestoreMocks.setDoc,
  Timestamp: firestoreMocks.Timestamp,
  where: firestoreMocks.where,
}));

vi.mock("@/shared/lib/firebase/firebaseApp", () => ({
  get firestoreDb() {
    return firebaseRuntimeMocks.firestoreDb;
  },
}));

import {
  buildDirectConversationId,
  sendDirectMessage,
  setTypingState,
  subscribeConversations,
  toFirebaseChatUid,
} from "./firestoreChatRepository";

describe("firestoreChatRepository", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    firebaseRuntimeMocks.firestoreDb = { db: "mock" };
    firestoreMocks.collection.mockReturnValue({ kind: "collection" });
    firestoreMocks.where.mockReturnValue({ kind: "where" });
    firestoreMocks.orderBy.mockReturnValue({ kind: "orderBy" });
    firestoreMocks.query.mockReturnValue({ kind: "query" });
    firestoreMocks.doc.mockReturnValue({ kind: "doc" });
  });

  it("normalizes user ids to stable firebase uid format", () => {
    expect(toFirebaseChatUid(15)).toBe("u_15");
    expect(toFirebaseChatUid("u_42")).toBe("u_42");
    expect(toFirebaseChatUid("#2")).toBe("u_2");
    expect(toFirebaseChatUid("U_007")).toBe("u_7");
  });

  it("rejects invalid user id format", () => {
    expect(() => toFirebaseChatUid("abc")).toThrow(
      "Invalid user id format. Use internal numeric user id (example: 2)."
    );
  });

  it("builds deterministic direct conversation id from two participants", () => {
    expect(buildDirectConversationId("u_9", "u_2")).toBe("u_2__u_9");
    expect(buildDirectConversationId("u_2", "u_9")).toBe("u_2__u_9");
  });

  it("maps conversation unread state from lastSeq and participant lastReadSeq", async () => {
    const onData = vi.fn();
    const onError = vi.fn();
    const fakeConversationDoc = {
      id: "u_11__u_12",
      ref: { path: "conversations/u_11__u_12" },
      data: () => ({
        participantIds: ["u_11", "u_12"],
        lastMessageText: "Hello",
        lastMessageSenderUid: "u_12",
        lastMessageAt: {
          toDate: () => new Date("2026-04-26T09:00:00.000Z"),
        },
        lastSeq: 6,
      }),
    };

    firestoreMocks.getDoc.mockResolvedValue({
      exists: () => true,
      data: () => ({ lastReadSeq: 2 }),
    });

    firestoreMocks.onSnapshot.mockImplementation((_query, next) => {
      void next({
        docs: [fakeConversationDoc],
      });
      return () => undefined;
    });

    const unsubscribe = subscribeConversations({
      currentUid: "u_11",
      onData,
      onError,
    });

    await waitFor(() => {
      expect(onData).toHaveBeenCalledTimes(1);
    });

    const payload = onData.mock.calls[0][0];
    expect(payload[0].peerUid).toBe("u_12");
    expect(payload[0].lastSeq).toBe(6);
    expect(payload[0].lastReadSeq).toBe(2);
    expect(payload[0].unreadCount).toBe(4);
    expect(onError).not.toHaveBeenCalled();

    unsubscribe();
  });

  it("writes throttled typing state to the current participant document", async () => {
    firestoreMocks.serverTimestamp.mockReturnValue({ server: "timestamp" });

    await setTypingState({
      conversationId: "u_11__u_12",
      uid: "u_11",
      isTyping: true,
    });

    expect(firestoreMocks.setDoc).toHaveBeenCalledWith(
      { kind: "doc" },
      expect.objectContaining({
        typingAt: { server: "timestamp" },
      }),
      { merge: true }
    );
    expect(firestoreMocks.Timestamp.fromMillis).toHaveBeenCalled();
  });

  it("does not allow sending empty message text", async () => {
    await expect(
      sendDirectMessage({
        currentUid: "u_11",
        currentRole: "FARMER",
        input: {
          conversationId: "u_11__u_12",
          text: "   ",
        },
      })
    ).rejects.toThrow("Message text cannot be empty.");

    expect(firestoreMocks.runTransaction).not.toHaveBeenCalled();
  });
});
