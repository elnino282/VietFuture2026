import { MessageCircle, Monitor, Smile, Sprout } from "lucide-react";

export function ChatWidgetEmptyState() {
  return (
    <div className="chat-widget-empty">
      <div className="chat-widget-empty__illustration" aria-hidden="true">
        <div className="chat-widget-empty__laptop">
          <div className="chat-widget-empty__laptop-screen">
            <Monitor className="h-6 w-6" />
          </div>
        </div>

        <div className="chat-widget-empty__bubble chat-widget-empty__bubble--1">
          <MessageCircle className="h-3.5 w-3.5" />
        </div>
        <div className="chat-widget-empty__bubble chat-widget-empty__bubble--2">
          <Smile className="h-3 w-3" />
        </div>
        <div className="chat-widget-empty__bubble chat-widget-empty__bubble--3">
          <Sprout className="h-3 w-3" />
        </div>
      </div>

      <h3>Chào mừng bạn đến với FarmACM Chat</h3>
      <p>Bắt đầu trò chuyện với nông trại và nhà cung cấp ngay!</p>
    </div>
  );
}
