import { useCallback, useState } from 'react';
import { useBuyerAiChat } from '@/entities/ai';
import type { AiChatMessage, AiChatRole } from './useAiChatSession';

type BuyerAiChatSessionOptions = {
    welcomeMessage?: string;
    fallbackMessage?: string;
};

const DEFAULT_WELCOME_MESSAGE =
    'Xin chào! Tôi là trợ lý mua nông sản. Tôi có thể giúp bạn kiểm tra chất lượng, giá, truy xuất nguồn gốc, vận chuyển và điều kiện đổi trả.';

const DEFAULT_FALLBACK_MESSAGE =
    'Hiện tại tôi chưa thể trả lời. Vui lòng thử lại hoặc đặt câu hỏi khác liên quan đến việc chọn mua nông sản.';

const createMessage = (role: AiChatRole, content: string): AiChatMessage => ({
    id: `${Date.now()}-${Math.random().toString(16).slice(2)}`,
    role,
    content,
    createdAt: new Date().toISOString(),
});

export function useBuyerAiChatSession(options: BuyerAiChatSessionOptions = {}) {
    const { mutateAsync, isPending } = useBuyerAiChat();
    const welcomeMessage = options.welcomeMessage ?? DEFAULT_WELCOME_MESSAGE;
    const fallbackMessage = options.fallbackMessage ?? DEFAULT_FALLBACK_MESSAGE;

    const [messages, setMessages] = useState<AiChatMessage[]>(() => [
        createMessage('assistant', welcomeMessage),
    ]);

    const reset = useCallback(() => {
        setMessages([createMessage('assistant', welcomeMessage)]);
    }, [welcomeMessage]);

    const sendMessage = useCallback(async (userMessage: string, buyerContext?: string | null) => {
        const trimmedMessage = userMessage.trim();
        if (!trimmedMessage || isPending) {
            return null;
        }

        setMessages((prev) => [...prev, createMessage('user', trimmedMessage)]);

        try {
            const response = await mutateAsync({
                userMessage: trimmedMessage,
                buyerContext: buyerContext?.trim() || undefined,
            });
            const assistantText = response.assistantMessage?.trim() || fallbackMessage;
            const assistantMessage = createMessage('assistant', assistantText);
            setMessages((prev) => [...prev, assistantMessage]);
            return assistantMessage;
        } catch {
            const assistantMessage = createMessage('assistant', fallbackMessage);
            setMessages((prev) => [...prev, assistantMessage]);
            return assistantMessage;
        }
    }, [fallbackMessage, isPending, mutateAsync]);

    return {
        messages,
        isSending: isPending,
        sendMessage,
        reset,
    };
}
