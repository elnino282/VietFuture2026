import { act, renderHook, waitFor } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { useChatWidget } from "./useChatWidget";
import type {
  ChatWidgetConversation,
  ChatWidgetMessage,
  ChatWidgetService,
} from "../model/widgetTypes";

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
      lastMessage: "Cam on ban da dat mua!",
      lastMessageAt: new Date("2026-05-24T08:00:00.000Z"),
      unreadCount: 1,
      status: "offline",
    }),
  ];
  const messagesByConversation: Record<string, ChatWidgetMessage[]> = {
    "conv-an-phu": [createMessage()],
    "conv-greenfarm": [
      createMessage({
        id: "msg-greenfarm-1",
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
        id: `msg-local-${messagesByConversation[conversationId]?.length ?? 0}`,
        conversationId,
        sender: "buyer",
        content,
        sentAt: new Date("2026-05-25T09:30:00.000Z"),
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

describe("useChatWidget", () => {
  it("loads conversations and exposes total unread count", async () => {
    const service = createService();
    const { result } = renderHook(() => useChatWidget({ service }));

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });

    expect(result.current.conversations).toHaveLength(2);
    expect(result.current.totalUnreadCount).toBe(3);
    expect(result.current.selectedConversation).toBeNull();
  });

  it("filters conversations by search query and unread filter", async () => {
    const service = createService();
    const { result } = renderHook(() => useChatWidget({ service }));

    await waitFor(() => expect(result.current.isLoading).toBe(false));

    act(() => result.current.setSearchQuery("green"));
    expect(result.current.filteredConversations.map((item) => item.id)).toEqual([
      "conv-greenfarm",
    ]);

    act(() => {
      result.current.setSearchQuery("");
      result.current.setFilter("unread");
    });
    expect(result.current.filteredConversations.map((item) => item.id)).toEqual([
      "conv-an-phu",
      "conv-greenfarm",
    ]);
  });

  it("selects a conversation, loads messages, and clears unread count", async () => {
    const service = createService();
    const { result } = renderHook(() => useChatWidget({ service }));

    await waitFor(() => expect(result.current.isLoading).toBe(false));

    await act(async () => {
      await result.current.selectConversation("conv-an-phu");
    });

    expect(result.current.selectedConversation?.id).toBe("conv-an-phu");
    expect(result.current.messages).toHaveLength(1);
    expect(result.current.selectedConversation?.unreadCount).toBe(0);
    expect(result.current.totalUnreadCount).toBe(1);
  });

  it("trims and sends messages while keeping the active conversation read", async () => {
    const service = createService();
    const { result } = renderHook(() => useChatWidget({ service }));

    await waitFor(() => expect(result.current.isLoading).toBe(false));
    await act(async () => {
      await result.current.selectConversation("conv-an-phu");
    });

    await act(async () => {
      await result.current.sendMessage("  Xin giao hang trong hom nay.  ");
    });

    expect(result.current.messages.at(-1)?.content).toBe("Xin giao hang trong hom nay.");
    expect(result.current.selectedConversation?.lastMessage).toBe("Xin giao hang trong hom nay.");
    expect(result.current.selectedConversation?.unreadCount).toBe(0);
    expect(result.current.totalUnreadCount).toBe(1);
  });

  it("ignores whitespace-only messages", async () => {
    const service = createService();
    const { result } = renderHook(() => useChatWidget({ service }));

    await waitFor(() => expect(result.current.isLoading).toBe(false));
    await act(async () => {
      await result.current.selectConversation("conv-an-phu");
    });

    await act(async () => {
      await result.current.sendMessage("   ");
    });

    expect(result.current.messages).toHaveLength(1);
  });
});
