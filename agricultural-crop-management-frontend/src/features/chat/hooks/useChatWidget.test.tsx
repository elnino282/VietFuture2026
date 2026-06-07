import { act, renderHook, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { useChatWidget } from "./useChatWidget";
import type { ChatConversation, ChatMessage } from "../model/types";

const hookMocks = vi.hoisted(() => ({
  useChatBootstrap: vi.fn(),
  useConversations: vi.fn(),
  useMessages: vi.fn(),
  useSendMessage: vi.fn(),
  useMarkConversationRead: vi.fn(),
  useChatRealtimeState: vi.fn(),
}));

vi.mock("../model/useChatBootstrap", () => ({
  useChatBootstrap: hookMocks.useChatBootstrap,
}));

vi.mock("./useConversations", () => ({
  useConversations: hookMocks.useConversations,
}));

vi.mock("./useMessages", () => ({
  useMessages: hookMocks.useMessages,
}));

vi.mock("./useSendMessage", () => ({
  useSendMessage: hookMocks.useSendMessage,
}));

vi.mock("./useMarkConversationRead", () => ({
  useMarkConversationRead: hookMocks.useMarkConversationRead,
}));

vi.mock("./useChatRealtimeState", () => ({
  useChatRealtimeState: hookMocks.useChatRealtimeState,
}));

function createConversation(overrides: Partial<ChatConversation> = {}): ChatConversation {
  return {
    id: "u_24__u_31",
    type: "direct",
    participantIds: ["u_24", "u_31"],
    peerUid: "u_31",
    peerUserId: 31,
    peerProfile: {
      userId: 31,
      firebaseUid: "u_31",
      displayName: "Nong trai An Phu",
      representativeName: "Nguyen An",
      farmName: "Nong trai An Phu",
      address: "Da Lat",
      role: "FARMER",
      avatarUrl: null,
    },
    lastMessageText: "Don hang da san sang.",
    lastMessageAt: new Date("2026-05-25T08:00:00.000Z"),
    lastMessageSenderUid: "u_31",
    lastSeq: 5,
    lastReadSeq: 3,
    unreadCount: 2,
    ...overrides,
  };
}

function createMessage(overrides: Partial<ChatMessage> = {}): ChatMessage {
  return {
    id: "msg-1",
    seq: 5,
    senderUid: "u_31",
    text: "Don hang da san sang.",
    createdAt: new Date("2026-05-25T08:00:00.000Z"),
    status: "sent",
    ...overrides,
  };
}

describe("useChatWidget", () => {
  const markRead = vi.fn().mockResolvedValue(undefined);
  const sendMessage = vi.fn().mockResolvedValue({ conversationId: "u_24__u_31", seq: 6 });
  const startDirectConversation = vi.fn().mockResolvedValue("u_24__u_31");
  const clearTypingState = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();

    hookMocks.useChatBootstrap.mockReturnValue({
      status: "ready",
      user: { uid: "u_24" },
      appUid: "u_24",
      role: "BUYER",
      error: null,
    });

    hookMocks.useConversations.mockReturnValue({
      conversations: [
        createConversation(),
        createConversation({
          id: "u_24__u_40",
          peerUid: "u_40",
          peerUserId: 40,
          peerProfile: {
            userId: 40,
            firebaseUid: "u_40",
            displayName: "GreenFarm Organic",
            representativeName: "Tran Binh",
            farmName: "GreenFarm Organic",
            address: "Can Tho",
            role: "FARMER",
            avatarUrl: null,
          },
          lastMessageText: "Cam on ban da dat mua.",
          lastMessageAt: new Date("2026-05-24T08:00:00.000Z"),
          unreadCount: 0,
        }),
      ],
      isLoading: false,
      hasLoadedConversations: true,
      error: null,
      isStartingConversation: false,
      startDirectConversation,
    });

    hookMocks.useMessages.mockReturnValue({
      messages: [createMessage()],
      isLoading: false,
      error: null,
    });

    hookMocks.useSendMessage.mockReturnValue({
      isSending: false,
      error: null,
      sendMessage,
    });

    hookMocks.useMarkConversationRead.mockReturnValue({
      isMarkingRead: false,
      error: null,
      markRead,
    });

    hookMocks.useChatRealtimeState.mockReturnValue({
      peerState: null,
      isPeerTyping: false,
      peerLastReadSeq: 0,
      publishTypingState: vi.fn(),
      clearTypingState,
    });
  });

  it("uses Firebase conversations and exposes total unread count without a mock service", async () => {
    const { result } = renderHook(() => useChatWidget());

    await waitFor(() => expect(result.current.isLoading).toBe(false));

    expect(hookMocks.useConversations).toHaveBeenCalledWith("u_24", "BUYER");
    expect(result.current.conversations).toHaveLength(2);
    expect(result.current.totalUnreadCount).toBe(2);
    expect(result.current.filteredConversations.map((item) => item.id)).toEqual([
      "u_24__u_31",
      "u_24__u_40",
    ]);
  });

  it("selects a Firebase conversation and marks the latest sequence as read", async () => {
    const { result } = renderHook(() => useChatWidget());

    await act(async () => {
      await result.current.selectConversation("u_24__u_31");
    });

    expect(result.current.selectedConversation?.id).toBe("u_24__u_31");
    expect(markRead).toHaveBeenCalledWith({
      conversationId: "u_24__u_31",
      lastReadSeq: 5,
    });
    expect(hookMocks.useMessages).toHaveBeenLastCalledWith("u_24", "u_24__u_31");
  });

  it("sends trimmed text through the Firebase send hook", async () => {
    const { result } = renderHook(() => useChatWidget());

    await act(async () => {
      await result.current.selectConversation("u_24__u_31");
    });

    await act(async () => {
      await result.current.sendMessage("  Giao giup minh sau 15h  ");
    });

    expect(sendMessage).toHaveBeenCalledWith({
      conversationId: "u_24__u_31",
      text: "Giao giup minh sau 15h",
    });
  });

  it("starts real direct conversations from popup contact search", async () => {
    const { result } = renderHook(() => useChatWidget());

    await act(async () => {
      await result.current.startConversation(40);
    });

    expect(startDirectConversation).toHaveBeenCalledWith(40);
    expect(result.current.selectedConversationId).toBe("u_24__u_31");
  });

  it("surfaces Firebase bootstrap errors instead of fake conversations", () => {
    hookMocks.useChatBootstrap.mockReturnValue({
      status: "disabled",
      user: null,
      appUid: null,
      role: null,
      error: "Firebase chat is disabled by environment flag.",
    });
    hookMocks.useConversations.mockReturnValue({
      conversations: [],
      isLoading: false,
      hasLoadedConversations: false,
      error: null,
      isStartingConversation: false,
      startDirectConversation,
    });

    const { result } = renderHook(() => useChatWidget());

    expect(result.current.conversations).toEqual([]);
    expect(result.current.error).toContain("Firebase chat is disabled");
  });
});
