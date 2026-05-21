import { render, screen, waitFor, within } from "@testing-library/react";
import { beforeAll, beforeEach, describe, expect, it, vi } from "vitest";
import { ChatPage } from "./ChatPage";
import { useChatRealtimeState } from "@/features/chat/hooks/useChatRealtimeState";
import { useConversations } from "@/features/chat/hooks/useConversations";
import { useMarkConversationRead } from "@/features/chat/hooks/useMarkConversationRead";
import { useMessages } from "@/features/chat/hooks/useMessages";
import { useSendMessage } from "@/features/chat/hooks/useSendMessage";
import { useChatBootstrap } from "@/features/chat/model/useChatBootstrap";
import type { ChatConversation, ChatMessage } from "@/features/chat/model/types";

vi.mock("@/features/chat/model/useChatBootstrap", () => ({
  useChatBootstrap: vi.fn(),
}));

vi.mock("@/features/chat/hooks/useConversations", () => ({
  useConversations: vi.fn(),
}));

vi.mock("@/features/chat/hooks/useChatRealtimeState", () => ({
  useChatRealtimeState: vi.fn(),
}));

vi.mock("@/features/chat/hooks/useMessages", () => ({
  useMessages: vi.fn(),
}));

vi.mock("@/features/chat/hooks/useSendMessage", () => ({
  useSendMessage: vi.fn(),
}));

vi.mock("@/features/chat/hooks/useMarkConversationRead", () => ({
  useMarkConversationRead: vi.fn(),
}));

const mockedUseChatBootstrap = vi.mocked(useChatBootstrap);
const mockedUseChatRealtimeState = vi.mocked(useChatRealtimeState);
const mockedUseConversations = vi.mocked(useConversations);
const mockedUseMessages = vi.mocked(useMessages);
const mockedUseSendMessage = vi.mocked(useSendMessage);
const mockedUseMarkConversationRead = vi.mocked(useMarkConversationRead);

const markReadSpy = vi.fn().mockResolvedValue(undefined);
const sendMessageSpy = vi.fn().mockResolvedValue({ conversationId: "u_1__u_2", seq: 1 });
const startConversationSpy = vi.fn().mockResolvedValue("u_1__u_2");

beforeAll(() => {
  HTMLElement.prototype.scrollIntoView = vi.fn();
});

function createConversation(): ChatConversation {
  return {
    id: "u_1__u_2",
    type: "direct",
    participantIds: ["u_1", "u_2"],
    peerUid: "u_2",
    peerUserId: 2,
    peerProfile: {
      userId: 2,
      firebaseUid: "u_2",
      displayName: "Tran Van B",
      representativeName: "Tran Van B",
      farmName: "Rice Valley Farm",
      address: "Can Tho",
      role: "FARMER",
      avatarUrl: null,
    },
    lastMessageText: "Hello there",
    lastMessageAt: new Date("2026-05-19T08:00:00.000Z"),
    lastMessageSenderUid: "u_2",
    lastSeq: 2,
    lastReadSeq: 1,
    unreadCount: 1,
  };
}

function createMessage(): ChatMessage {
  return {
    id: "m_1",
    seq: 2,
    senderUid: "u_2",
    text: "Hello there",
    createdAt: new Date("2026-05-19T08:01:00.000Z"),
    status: "sent",
  };
}

beforeEach(() => {
  vi.clearAllMocks();

  mockedUseChatBootstrap.mockReturnValue({
    status: "ready",
    user: {} as never,
    appUid: "u_1",
    role: "FARMER",
    error: null,
  });

  mockedUseChatRealtimeState.mockReturnValue({
    isPeerTyping: false,
    peerLastReadSeq: 0,
    peerLastReadAt: null,
    publishTypingState: vi.fn(),
    clearTypingState: vi.fn(),
  });

  mockedUseConversations.mockReturnValue({
    conversations: [],
    isLoading: false,
    hasLoadedConversations: true,
    error: null,
    isStartingConversation: false,
    startDirectConversation: startConversationSpy,
  });

  mockedUseMessages.mockReturnValue({
    messages: [],
    isLoading: false,
    error: null,
  });

  mockedUseSendMessage.mockReturnValue({
    isSending: false,
    error: null,
    sendMessage: sendMessageSpy,
  });

  mockedUseMarkConversationRead.mockReturnValue({
    isMarkingRead: false,
    error: null,
    markRead: markReadSpy,
  });
});

describe("ChatPage layout", () => {
  it("renders a desktop grid wrapper with three direct panels and keeps right panel for no selection", () => {
    render(<ChatPage />);

    const desktopGrid = screen.getByTestId("chat-desktop-grid");
    expect(desktopGrid).toBeInTheDocument();

    const directChildren = Array.from(desktopGrid.children);
    expect(directChildren).toHaveLength(3);

    expect(within(desktopGrid).getByTestId("chat-left-panel")).toBeInTheDocument();
    expect(within(desktopGrid).getByTestId("chat-center-panel")).toBeInTheDocument();
    expect(within(desktopGrid).getByTestId("chat-right-panel")).toBeInTheDocument();

    expect(
      within(screen.getByTestId("chat-right-panel")).getByText(
        "Select a conversation to view farm, representative, role, address, and internal ID."
      )
    ).toBeInTheDocument();
    expect(
      within(screen.getByTestId("chat-right-panel")).getByText(/verify identity/i)
    ).toBeInTheDocument();
  });

  it("shows thread in center and contact details in right panel after selecting a conversation", async () => {
    mockedUseConversations.mockReturnValue({
      conversations: [createConversation()],
      isLoading: false,
      hasLoadedConversations: true,
      error: null,
      isStartingConversation: false,
      startDirectConversation: startConversationSpy,
    });

    mockedUseMessages.mockReturnValue({
      messages: [createMessage()],
      isLoading: false,
      error: null,
    });

    render(<ChatPage />);

    const centerPanel = screen.getByTestId("chat-center-panel");
    const rightPanel = screen.getByTestId("chat-right-panel");

    await waitFor(() => {
      expect(within(centerPanel).getByText("Hello there")).toBeInTheDocument();
    });

    await waitFor(() => {
      expect(within(rightPanel).getAllByText("Rice Valley Farm").length).toBeGreaterThan(0);
    });

    expect(within(rightPanel).getByText("Tran Van B")).toBeInTheDocument();
    expect(within(rightPanel).getByText("Can Tho")).toBeInTheDocument();
  });

  it("renders real avatar images when profile avatarUrl is present", async () => {
    mockedUseConversations.mockReturnValue({
      conversations: [
        {
          ...createConversation(),
          peerProfile: {
            ...createConversation().peerProfile!,
            avatarUrl: "https://example.com/avatar.png",
          },
        },
      ],
      isLoading: false,
      hasLoadedConversations: true,
      error: null,
      isStartingConversation: false,
      startDirectConversation: startConversationSpy,
    });

    render(<ChatPage />);

    await waitFor(() => {
      const avatarImages = screen.getAllByAltText("Rice Valley Farm avatar");
      expect(avatarImages.length).toBeGreaterThan(0);
      expect(avatarImages[0]).toHaveAttribute("src", "https://example.com/avatar.png");
    });
  });
});
