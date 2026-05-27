export type ChatWidgetParticipant = "buyer" | "seller";

export type ChatWidgetFilter = "all" | "unread";

export type ChatWidgetConversationStatus = "online" | "offline";

/**
 * Stable English keys for transaction status.
 * Map to Vietnamese display labels via `getTransactionStatusLabel()`.
 */
export type ChatWidgetTransactionStatus =
  | "trading"
  | "pending_confirmation"
  | "delivered"
  | "completed"
  | "cancelled";

export type ChatWidgetContext = {
  title: string;
  subtitle: string;
  imageUrl: string | null;
  traceCode: string;
  transactionStatus: ChatWidgetTransactionStatus;
  price?: string;
  quantity?: string;
};

export type ChatWidgetConversation = {
  id: string;
  farmName: string;
  sellerName: string;
  avatarUrl: string | null;
  region: string;
  lastMessage: string;
  lastMessageAt: Date;
  unreadCount: number;
  status: ChatWidgetConversationStatus;
  context: ChatWidgetContext;
};

export type ChatWidgetMessage = {
  id: string;
  conversationId: string;
  sender: ChatWidgetParticipant;
  content: string;
  sentAt: Date;
  status: "sent";
};

export type ChatWidgetService = {
  getConversations: () => Promise<ChatWidgetConversation[]>;
  getConversationMessages: (conversationId: string) => Promise<ChatWidgetMessage[]>;
  sendMessage: (conversationId: string, content: string) => Promise<ChatWidgetMessage>;
  markConversationAsRead: (conversationId: string) => Promise<void>;
};
