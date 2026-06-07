export type ChatBootstrapStatus = "idle" | "disabled" | "loading" | "ready" | "error";

export type ChatContactProfile = {
  userId: number;
  firebaseUid: string;
  displayName: string;
  representativeName: string;
  farmName: string | null;
  farmId?: number | null;
  address: string | null;
  role: string | null;
  avatarUrl?: string | null;
  ratingAverage?: number | null;
  ratingCount?: number | null;
};

export type ChatConversation = {
  id: string;
  type: "direct";
  participantIds: string[];
  peerUid: string;
  peerUserId: number | null;
  peerProfile: ChatContactProfile | null;
  lastMessageText: string;
  lastMessageAt: Date | null;
  lastMessageSenderUid: string | null;
  lastSeq: number;
  lastReadSeq: number;
  unreadCount: number;
};

export type ChatMessage = {
  id: string;
  seq: number;
  senderUid: string;
  text: string;
  createdAt: Date | null;
  status: "sent";
};

export type ChatParticipantState = {
  lastReadSeq: number;
  lastReadAt: Date | null;
  typingUntil: Date | null;
  isTyping: boolean;
};

export type SendChatMessageInput = {
  conversationId?: string | null;
  peerUid?: string | null;
  text: string;
};

export type MarkReadInput = {
  conversationId: string;
  lastReadSeq: number;
};
