import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes, useLocation } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { FloatingChatButton } from "./FloatingChatButton";
import { ChatWidget } from "./ChatWidget";
import type {
  ChatWidgetConversation,
  ChatWidgetMessage,
  ChatWidgetService,
} from "../model/widgetTypes";

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

function createConversation(overrides: Partial<ChatWidgetConversation> = {}): ChatWidgetConversation {
  return {
    id: "conv-an-phu",
    farmName: "Nong trai An Phu",
    sellerName: "Nguyen An",
    avatarUrl: null,
    region: "Da Lat",
    lastMessage: "Don hang #FT12345 da san sang.",
    lastMessageAt: new Date("2026-05-25T08:00:00.000Z"),
    unreadCount: 2,
    status: "online",
    context: {
      title: "Don hang #FT12345",
      subtitle: "Rau huu co theo lo thu hoach 05/2026",
      imageUrl: null,
      traceCode: "FT-ANPHU-0526",
      transactionStatus: "delivered",
    },
    ...overrides,
  };
}

function createMessage(overrides: Partial<ChatWidgetMessage> = {}): ChatWidgetMessage {
  return {
    id: "msg-1",
    conversationId: "conv-an-phu",
    sender: "seller",
    content: "Don hang #FT12345 da san sang.",
    sentAt: new Date("2026-05-25T08:00:00.000Z"),
    status: "sent",
    ...overrides,
  };
}

function createService(): ChatWidgetService {
  let conversations = [
    createConversation(),
    createConversation({
      id: "conv-greenfarm",
      farmName: "GreenFarm Organic",
      sellerName: "Tran Binh",
      region: "Can Tho",
      unreadCount: 1,
      lastMessage: "Cam on ban da dat mua!",
      lastMessageAt: new Date("2026-05-24T10:00:00.000Z"),
      context: {
        title: "Gio rau theo mua",
        subtitle: "Giao hang du kien ngay 26/05",
        imageUrl: null,
        traceCode: "FT-GREEN-1805",
        transactionStatus: "trading",
      },
    }),
  ];
  const messagesByConversation: Record<string, ChatWidgetMessage[]> = {
    "conv-an-phu": [createMessage()],
    "conv-greenfarm": [
      createMessage({
        id: "msg-green-1",
        conversationId: "conv-greenfarm",
        content: "Cam on ban da dat mua!",
      }),
    ],
  };

  return {
    async getConversations() {
      return conversations;
    },
    async getConversationMessages(conversationId) {
      return messagesByConversation[conversationId] ?? [];
    },
    async sendMessage(conversationId, content) {
      const message = createMessage({
        id: `local-${content}`,
        conversationId,
        sender: "buyer",
        content,
        sentAt: new Date("2026-05-25T09:00:00.000Z"),
      });
      messagesByConversation[conversationId] = [
        ...(messagesByConversation[conversationId] ?? []),
        message,
      ];
      conversations = conversations.map((conversation) =>
        conversation.id === conversationId
          ? {
              ...conversation,
              lastMessage: content,
              lastMessageAt: message.sentAt,
              unreadCount: 0,
            }
          : conversation,
      );
      return message;
    },
    async markConversationAsRead(conversationId) {
      conversations = conversations.map((conversation) =>
        conversation.id === conversationId
          ? { ...conversation, unreadCount: 0 }
          : conversation,
      );
    },
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
});

describe("Floating chat widget", () => {
  it("shows unread badge on the floating button and opens the popup", async () => {
    const user = userEvent.setup();
    renderFloatingButton();

    expect(screen.getByRole("button", { name: /open chat/i })).toBeInTheDocument();
    expect(await screen.findByText("3")).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: /open chat/i }));

    expect(await screen.findByRole("dialog", { name: /chat/i })).toBeInTheDocument();
    expect(screen.getByText("Nông trại An Phú")).toBeInTheDocument();
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
        <ChatWidget
          isOpen
          onMinimize={vi.fn()}
          onClose={vi.fn()}
          onExpand={vi.fn()}
          service={createService()}
        />
      </MemoryRouter>,
    );

    await user.click(await screen.findByRole("button", { name: /open conversation with Nong trai An Phu/i }));
    expect(await screen.findByRole("button", { name: /more options/i })).toBeInTheDocument();
    expect(screen.getByText("Don hang #FT12345 da san sang.")).toBeInTheDocument();

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
        <ChatWidget
          isOpen
          onMinimize={vi.fn()}
          onClose={vi.fn()}
          onExpand={vi.fn()}
          service={createService()}
        />
      </MemoryRouter>,
    );

    await user.click(await screen.findByRole("button", { name: /open conversation with GreenFarm Organic/i }));
    expect(await screen.findByRole("button", { name: /more options/i })).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: /back to conversations/i }));

    expect(screen.getByRole("button", { name: /open conversation with Nong trai An Phu/i })).toBeInTheDocument();
  });
});
