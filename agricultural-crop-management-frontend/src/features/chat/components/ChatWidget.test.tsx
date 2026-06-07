import { useMemo, useState } from "react";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes, useLocation } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { FloatingChatButton } from "./FloatingChatButton";
import { ChatWidget } from "./ChatWidget";
import type { UseChatWidgetResult } from "../hooks/useChatWidget";
import type { ChatConversation, ChatMessage } from "../model/types";

const widgetHookMocks = vi.hoisted(() => ({
  useChatWidget: vi.fn(),
}));

vi.mock("../hooks/useChatWidget", () => ({
  useChatWidget: widgetHookMocks.useChatWidget,
}));

vi.mock("@/features/auth/context/AuthContext", () => ({
  useAuth: () => ({ isAuthenticated: true }),
}));

vi.mock("react-i18next", () => ({
  useTranslation: () => ({
    t: (_key: string, options?: { defaultValue?: string; count?: number }) => {
      if (!options?.defaultValue) return _key;
      return options.defaultValue.replace("{{count}}", String(options.count ?? ""));
    },
  }),
}));

function LocationProbe() {
  const location = useLocation();
  return <span data-testid="location-path">{location.pathname}</span>;
}

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

function useMockChatWidget(): UseChatWidgetResult {
  const conversations = useMemo(
    () => [
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
        lastMessageAt: new Date("2026-05-24T10:00:00.000Z"),
        lastSeq: 1,
        lastReadSeq: 0,
        unreadCount: 1,
      }),
    ],
    [],
  );
  const [selectedConversationId, setSelectedConversationId] = useState<string | null>(null);
  const [messages, setMessages] = useState<ChatMessage[]>([createMessage()]);
  const [searchQuery, setSearchQuery] = useState("");
  const [filter, setFilter] = useState<UseChatWidgetResult["filter"]>("all");
  const selectedConversation =
    conversations.find((conversation) => conversation.id === selectedConversationId) ?? null;
  const filteredConversations = conversations.filter((conversation) => {
    if (filter === "unread" && conversation.unreadCount <= 0) {
      return false;
    }
    if (!searchQuery.trim()) {
      return true;
    }
    return (conversation.peerProfile?.displayName ?? "")
      .toLowerCase()
      .includes(searchQuery.trim().toLowerCase());
  });

  return {
    conversations,
    filteredConversations,
    selectedConversation,
    selectedConversationId,
    messages: selectedConversationId ? messages : [],
    searchQuery,
    filter,
    totalUnreadCount: 3,
    isLoading: false,
    isMessagesLoading: false,
    isSending: false,
    isStartingConversation: false,
    error: null,
    messagesError: null,
    currentUid: "u_24",
    setSearchQuery,
    setFilter,
    selectConversation: async (conversationId) => setSelectedConversationId(conversationId),
    startConversation: async (peerUserId) => setSelectedConversationId(`u_24__u_${peerUserId}`),
    clearSelectedConversation: () => setSelectedConversationId(null),
    sendMessage: async (content) =>
      setMessages((current) => [
        ...current,
        createMessage({
          id: `local-${current.length}`,
          seq: current.length + 6,
          senderUid: "u_24",
          text: content,
          createdAt: new Date("2026-05-25T09:00:00.000Z"),
        }),
      ]),
  };
}

function renderFloatingButton() {
  return render(
    <MemoryRouter
      initialEntries={["/marketplace"]}
      future={{ v7_relativeSplatPath: true, v7_startTransition: true }}
    >
      <FloatingChatButton />
      <Routes>
        <Route path="*" element={<LocationProbe />} />
      </Routes>
    </MemoryRouter>,
  );
}

beforeEach(() => {
  HTMLElement.prototype.scrollIntoView = vi.fn();
  widgetHookMocks.useChatWidget.mockImplementation(useMockChatWidget);
});

describe("Floating chat widget", () => {
  it("shows unread badge on the floating button and opens the popup", async () => {
    const user = userEvent.setup();
    renderFloatingButton();

    expect(screen.getByRole("button", { name: /open chat/i })).toBeInTheDocument();
    expect(await screen.findByText("3")).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: /open chat/i }));

    expect(await screen.findByRole("dialog", { name: /chat/i })).toBeInTheDocument();
    expect(screen.getByText("Nong trai An Phu")).toBeInTheDocument();
  });

  it("minimizes and closes back to the floating button", async () => {
    const user = userEvent.setup();
    renderFloatingButton();

    await user.click(screen.getByRole("button", { name: /open chat/i }));
    await user.click(await screen.findByRole("button", { name: /minimize chat/i }));

    expect(screen.queryByRole("dialog", { name: /chat/i })).not.toBeInTheDocument();
    expect(screen.getByRole("button", { name: /open chat/i })).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: /open chat/i }));
    await user.click(await screen.findByRole("button", { name: /close chat/i }));

    expect(screen.queryByRole("dialog", { name: /chat/i })).not.toBeInTheDocument();
    expect(screen.getByRole("button", { name: /open chat/i })).toBeInTheDocument();
  });

  it("expands by navigating to the existing chat route", async () => {
    const user = userEvent.setup();
    renderFloatingButton();

    await user.click(screen.getByRole("button", { name: /open chat/i }));
    await user.click(await screen.findByRole("button", { name: /expand chat/i }));

    expect(screen.getByTestId("location-path")).toHaveTextContent("/chat");
  });

  it("selects a conversation, sends a trimmed message, and keeps the popup open", async () => {
    const user = userEvent.setup();
    render(
      <MemoryRouter future={{ v7_relativeSplatPath: true, v7_startTransition: true }}>
        <ChatWidget isOpen onMinimize={vi.fn()} onClose={vi.fn()} onExpand={vi.fn()} />
      </MemoryRouter>,
    );

    await user.click(
      await screen.findByRole("button", { name: /open conversation with Nong trai An Phu/i }),
    );
    expect((await screen.findAllByText("Nguyen An - Da Lat")).length).toBeGreaterThan(0);
    expect(screen.getAllByText("Don hang da san sang.").length).toBeGreaterThan(0);

    const input = screen.getByRole("textbox", { name: /message input/i });
    await user.type(input, "  Giao giup minh sau 15h  ");
    await user.click(screen.getByRole("button", { name: /send message/i }));

    await waitFor(() => {
      expect(screen.getAllByText("Giao giup minh sau 15h").length).toBeGreaterThan(0);
    });
    expect(screen.getByRole("dialog", { name: /chat/i })).toBeInTheDocument();
  });

  it("supports list-detail-back flow for small screens", async () => {
    const user = userEvent.setup();
    render(
      <MemoryRouter future={{ v7_relativeSplatPath: true, v7_startTransition: true }}>
        <ChatWidget isOpen onMinimize={vi.fn()} onClose={vi.fn()} onExpand={vi.fn()} />
      </MemoryRouter>,
    );

    await user.click(
      await screen.findByRole("button", { name: /open conversation with GreenFarm Organic/i }),
    );
    expect((await screen.findAllByText("Tran Binh - Can Tho")).length).toBeGreaterThan(0);

    await user.click(screen.getByRole("button", { name: /back to conversations/i }));

    expect(
      screen.getByRole("button", { name: /open conversation with Nong trai An Phu/i }),
    ).toBeInTheDocument();
  });

  it("shows Firebase setup errors without offering contact search", () => {
    widgetHookMocks.useChatWidget.mockReturnValue({
      conversations: [],
      filteredConversations: [],
      selectedConversation: null,
      selectedConversationId: null,
      messages: [],
      searchQuery: "",
      filter: "all",
      totalUnreadCount: 0,
      isLoading: false,
      isMessagesLoading: false,
      isSending: false,
      isStartingConversation: false,
      error: "Firebase chat is disabled by environment flag.",
      messagesError: null,
      currentUid: null,
      setSearchQuery: vi.fn(),
      setFilter: vi.fn(),
      selectConversation: vi.fn(),
      startConversation: vi.fn(),
      clearSelectedConversation: vi.fn(),
      sendMessage: vi.fn(),
    });

    render(
      <MemoryRouter future={{ v7_relativeSplatPath: true, v7_startTransition: true }}>
        <ChatWidget isOpen onMinimize={vi.fn()} onClose={vi.fn()} onExpand={vi.fn()} />
      </MemoryRouter>,
    );

    expect(screen.getByText("Firebase chat is disabled by environment flag.")).toBeInTheDocument();
    expect(screen.queryByRole("textbox", { name: /search chat contacts/i })).not.toBeInTheDocument();
  });
});
