import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { ChatConversationList } from "./ChatConversationList";

describe("ChatConversationList", () => {
  it("renders conversations and triggers selection", () => {
    const onSelectConversation = vi.fn();
    const onStartConversation = vi.fn().mockResolvedValue(undefined);

    render(
      <ChatConversationList
        currentUid="u_1"
        conversations={[
          {
            id: "u_1__u_2",
            type: "direct",
            participantIds: ["u_1", "u_2"],
            peerUid: "u_2",
            peerUserId: 2,
            peerProfile: null,
            lastMessageText: "Xin chao",
            lastMessageAt: new Date("2026-04-26T10:00:00.000Z"),
            lastMessageSenderUid: "u_2",
            lastSeq: 5,
            lastReadSeq: 2,
            unreadCount: 3,
          },
        ]}
        selectedConversationId={null}
        onSelectConversation={onSelectConversation}
        onStartConversation={onStartConversation}
        isLoading={false}
        isStartingConversation={false}
        error={null}
      />
    );

    expect(screen.getByText("Account #2")).toBeInTheDocument();
    expect(screen.getByLabelText("3 unread messages")).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: /Account #2/i }));
    expect(onSelectConversation).toHaveBeenCalledWith("u_1__u_2");
  });

  it("renders enriched profile display name", () => {
    const onSelectConversation = vi.fn();
    const onStartConversation = vi.fn().mockResolvedValue(undefined);

    render(
      <ChatConversationList
        currentUid="u_1"
        conversations={[
          {
            id: "u_1__u_3",
            type: "direct",
            participantIds: ["u_1", "u_3"],
            peerUid: "u_3",
            peerUserId: 3,
            peerProfile: {
              userId: 3,
              firebaseUid: "u_3",
              displayName: "Nguyen Van B",
              representativeName: "Nguyen Van B",
              farmName: "Green Valley Farm",
              address: "Ward A, Province B",
              role: "FARMER",
            },
            lastMessageText: "Hello",
            lastMessageAt: new Date("2026-04-26T12:00:00.000Z"),
            lastMessageSenderUid: "u_3",
            lastSeq: 3,
            lastReadSeq: 3,
            unreadCount: 0,
          },
        ]}
        selectedConversationId={null}
        onSelectConversation={onSelectConversation}
        onStartConversation={onStartConversation}
        isLoading={false}
        isStartingConversation={false}
        error={null}
      />
    );

    expect(screen.getByText("Green Valley Farm")).toBeInTheDocument();
    expect(screen.getByText("Nguyen Van B · Ward A, Province B")).toBeInTheDocument();
  });

  it("marks selected conversation with aria-current", () => {
    const onSelectConversation = vi.fn();
    const onStartConversation = vi.fn().mockResolvedValue(undefined);

    render(
      <ChatConversationList
        currentUid="u_1"
        conversations={[
          {
            id: "u_1__u_2",
            type: "direct",
            participantIds: ["u_1", "u_2"],
            peerUid: "u_2",
            peerUserId: 2,
            peerProfile: null,
            lastMessageText: "Xin chao",
            lastMessageAt: new Date("2026-04-26T10:00:00.000Z"),
            lastMessageSenderUid: "u_2",
            lastSeq: 5,
            lastReadSeq: 2,
            unreadCount: 0,
          },
        ]}
        selectedConversationId="u_1__u_2"
        onSelectConversation={onSelectConversation}
        onStartConversation={onStartConversation}
        isLoading={false}
        isStartingConversation={false}
        error={null}
      />
    );

    const selectedButton = screen.getByRole("button", { name: /Account #2/i });
    expect(selectedButton).toHaveAttribute("aria-current", "true");
  });
});
