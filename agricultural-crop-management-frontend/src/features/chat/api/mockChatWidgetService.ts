import type {
  ChatWidgetConversation,
  ChatWidgetMessage,
  ChatWidgetService,
} from "../model/widgetTypes";

const now = new Date("2026-05-25T09:00:00.000Z");

const initialConversations: ChatWidgetConversation[] = [
  {
    id: "conv-an-phu",
    farmName: "Nông trại An Phú",
    sellerName: "Nguyễn An",
    avatarUrl: null,
    region: "Đà Lạt",
    lastMessage: "Đơn hàng #FT12345 đã sẵn sàng giao.",
    lastMessageAt: new Date("2026-05-25T08:00:00.000Z"),
    unreadCount: 2,
    status: "online",
    context: {
      title: "Đơn hàng #FT12345",
      subtitle: "Rau hữu cơ theo lô thu hoạch 05/2026",
      imageUrl: null,
      traceCode: "FT-ANPHU-0526",
      transactionStatus: "delivered",
      price: "2.500.000₫",
      quantity: "50kg",
    },
  },
  {
    id: "conv-greenfarm",
    farmName: "GreenFarm Organic",
    sellerName: "Trần Bình",
    avatarUrl: null,
    region: "Cần Thơ",
    lastMessage: "Cảm ơn bạn đã đặt mua!",
    lastMessageAt: new Date("2026-05-24T14:20:00.000Z"),
    unreadCount: 1,
    status: "online",
    context: {
      title: "Giỏ rau theo mùa",
      subtitle: "Giao hàng dự kiến ngày 26/05",
      imageUrl: null,
      traceCode: "FT-GREEN-1805",
      transactionStatus: "trading",
      price: "850.000₫",
      quantity: "10kg",
    },
  },
  {
    id: "conv-dalat-fresh",
    farmName: "Dalat Farm Fresh",
    sellerName: "Lê Minh",
    avatarUrl: null,
    region: "Lâm Đồng",
    lastMessage: "Sản phẩm sắp được giao đến kho.",
    lastMessageAt: new Date("2026-05-23T15:10:00.000Z"),
    unreadCount: 0,
    status: "offline",
    context: {
      title: "Cà chua beef",
      subtitle: "Lô thu hoạch DL-0526",
      imageUrl: null,
      traceCode: "FT-DALAT-0526",
      transactionStatus: "pending_confirmation",
      price: "1.200.000₫",
      quantity: "25kg",
    },
  },
  {
    id: "conv-biogarden",
    farmName: "BioGarden",
    sellerName: "Phạm Hoa",
    avatarUrl: null,
    region: "Đồng Nai",
    lastMessage: "Chương trình ưu đãi mới đã sẵn sàng.",
    lastMessageAt: new Date("2026-05-22T10:45:00.000Z"),
    unreadCount: 0,
    status: "offline",
    context: {
      title: "Gói rau an toàn",
      subtitle: "Đặt trực tiếp từ nông trại BioGarden",
      imageUrl: null,
      traceCode: "FT-BIO-0522",
      transactionStatus: "completed",
      price: "650.000₫",
      quantity: "5kg",
    },
  },
  {
    id: "conv-mekong",
    farmName: "Mekong Delta Farm",
    sellerName: "Võ Thanh",
    avatarUrl: null,
    region: "An Giang",
    lastMessage: "Đơn hàng đã bị hủy theo yêu cầu.",
    lastMessageAt: new Date("2026-05-21T09:30:00.000Z"),
    unreadCount: 0,
    status: "offline",
    context: {
      title: "Gạo ST25 hữu cơ",
      subtitle: "Lô thu hoạch MK-0521",
      imageUrl: null,
      traceCode: "FT-MEKONG-0521",
      transactionStatus: "cancelled",
      price: "3.000.000₫",
      quantity: "100kg",
    },
  },
];

const initialMessages: Record<string, ChatWidgetMessage[]> = {
  "conv-an-phu": [
    {
      id: "anphu-1",
      conversationId: "conv-an-phu",
      sender: "seller",
      content: "Chào bạn, đơn hàng #FT12345 đã được đóng gói.",
      sentAt: new Date("2026-05-25T07:30:00.000Z"),
      status: "sent",
    },
    {
      id: "anphu-2",
      conversationId: "conv-an-phu",
      sender: "buyer",
      content: "Mình có thể nhận hàng trong chiều nay không?",
      sentAt: new Date("2026-05-25T07:45:00.000Z"),
      status: "sent",
    },
    {
      id: "anphu-3",
      conversationId: "conv-an-phu",
      sender: "seller",
      content: "Được ạ. Đơn hàng sẽ được bàn giao cho đơn vị vận chuyển lúc 14:00.",
      sentAt: new Date("2026-05-25T07:50:00.000Z"),
      status: "sent",
    },
    {
      id: "anphu-4",
      conversationId: "conv-an-phu",
      sender: "buyer",
      content: "Tốt quá, cảm ơn anh nhé!",
      sentAt: new Date("2026-05-25T07:55:00.000Z"),
      status: "sent",
    },
    {
      id: "anphu-5",
      conversationId: "conv-an-phu",
      sender: "seller",
      content: "Đơn hàng #FT12345 đã sẵn sàng giao. Bạn kiểm tra mã truy xuất FT-ANPHU-0526 để theo dõi nhé.",
      sentAt: new Date("2026-05-25T08:00:00.000Z"),
      status: "sent",
    },
  ],
  "conv-greenfarm": [
    {
      id: "green-1",
      conversationId: "conv-greenfarm",
      sender: "seller",
      content: "Chào bạn! Giỏ rau theo mùa đang được chuẩn bị.",
      sentAt: new Date("2026-05-24T14:00:00.000Z"),
      status: "sent",
    },
    {
      id: "green-2",
      conversationId: "conv-greenfarm",
      sender: "buyer",
      content: "Dự kiến khi nào giao hàng vậy ạ?",
      sentAt: new Date("2026-05-24T14:10:00.000Z"),
      status: "sent",
    },
    {
      id: "green-3",
      conversationId: "conv-greenfarm",
      sender: "seller",
      content: "Cảm ơn bạn đã đặt mua! Dự kiến giao ngày 26/05 nhé.",
      sentAt: new Date("2026-05-24T14:20:00.000Z"),
      status: "sent",
    },
  ],
  "conv-dalat-fresh": [
    {
      id: "dalat-1",
      conversationId: "conv-dalat-fresh",
      sender: "seller",
      content: "Lô cà chua beef có thể truy xuất bằng mã FT-DALAT-0526.",
      sentAt: new Date("2026-05-23T14:50:00.000Z"),
      status: "sent",
    },
    {
      id: "dalat-2",
      conversationId: "conv-dalat-fresh",
      sender: "buyer",
      content: "Cho mình hỏi giá sỉ cho 50kg được không ạ?",
      sentAt: new Date("2026-05-23T15:00:00.000Z"),
      status: "sent",
    },
    {
      id: "dalat-3",
      conversationId: "conv-dalat-fresh",
      sender: "seller",
      content: "Sản phẩm sắp được giao đến kho. Mình sẽ gửi báo giá sỉ trong ngày nhé.",
      sentAt: new Date("2026-05-23T15:10:00.000Z"),
      status: "sent",
    },
  ],
  "conv-biogarden": [
    {
      id: "bio-1",
      conversationId: "conv-biogarden",
      sender: "seller",
      content: "BioGarden đang có gói rau an toàn giao trong 24h.",
      sentAt: new Date("2026-05-22T10:30:00.000Z"),
      status: "sent",
    },
    {
      id: "bio-2",
      conversationId: "conv-biogarden",
      sender: "buyer",
      content: "Mình đã nhận hàng rồi, chất lượng rất tốt!",
      sentAt: new Date("2026-05-22T10:40:00.000Z"),
      status: "sent",
    },
    {
      id: "bio-3",
      conversationId: "conv-biogarden",
      sender: "seller",
      content: "Chương trình ưu đãi mới đã sẵn sàng. Đặt hàng lần sau sẽ được giảm 10%!",
      sentAt: new Date("2026-05-22T10:45:00.000Z"),
      status: "sent",
    },
  ],
  "conv-mekong": [
    {
      id: "mekong-1",
      conversationId: "conv-mekong",
      sender: "buyer",
      content: "Mình muốn hủy đơn hàng gạo ST25 được không ạ?",
      sentAt: new Date("2026-05-21T09:00:00.000Z"),
      status: "sent",
    },
    {
      id: "mekong-2",
      conversationId: "conv-mekong",
      sender: "seller",
      content: "Dạ được bạn. Mình đã xử lý hủy đơn hàng.",
      sentAt: new Date("2026-05-21T09:20:00.000Z"),
      status: "sent",
    },
    {
      id: "mekong-3",
      conversationId: "conv-mekong",
      sender: "seller",
      content: "Đơn hàng đã bị hủy theo yêu cầu. Tiền sẽ được hoàn trong 3-5 ngày làm việc.",
      sentAt: new Date("2026-05-21T09:30:00.000Z"),
      status: "sent",
    },
  ],
};

function cloneConversation(conversation: ChatWidgetConversation): ChatWidgetConversation {
  return {
    ...conversation,
    lastMessageAt: new Date(conversation.lastMessageAt),
    context: { ...conversation.context },
  };
}

function cloneMessage(message: ChatWidgetMessage): ChatWidgetMessage {
  return {
    ...message,
    sentAt: new Date(message.sentAt),
  };
}

function createMockChatWidgetService(): ChatWidgetService {
  let conversations = initialConversations.map(cloneConversation);
  const messagesByConversation = Object.fromEntries(
    Object.entries(initialMessages).map(([conversationId, messages]) => [
      conversationId,
      messages.map(cloneMessage),
    ]),
  );

  return {
    async getConversations() {
      return conversations.map(cloneConversation);
    },
    async getConversationMessages(conversationId) {
      return (messagesByConversation[conversationId] ?? []).map(cloneMessage);
    },
    async sendMessage(conversationId, content) {
      const nextMessage: ChatWidgetMessage = {
        id: `${conversationId}-local-${Date.now()}`,
        conversationId,
        sender: "buyer",
        content,
        sentAt: new Date(Math.max(Date.now(), now.getTime())),
        status: "sent",
      };

      messagesByConversation[conversationId] = [
        ...(messagesByConversation[conversationId] ?? []),
        nextMessage,
      ];
      conversations = conversations.map((conversation) =>
        conversation.id === conversationId
          ? {
              ...conversation,
              lastMessage: content,
              lastMessageAt: nextMessage.sentAt,
              unreadCount: 0,
            }
          : conversation,
      );

      return cloneMessage(nextMessage);
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

export const mockChatWidgetService = createMockChatWidgetService();
