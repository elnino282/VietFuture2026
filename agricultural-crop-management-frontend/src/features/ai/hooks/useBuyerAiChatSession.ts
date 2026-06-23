import { useCallback, useState } from 'react';
import { sendAiChatMessage, type AiChatSource } from '@/services/aiChatService';
import type { AiChatMessage, AiChatRole } from './useAiChatSession';

type BuyerAiChatSessionOptions = {
    welcomeMessage?: string;
    fallbackMessage?: string;
};

const DEFAULT_WELCOME_MESSAGE =
    'Xin chào! Tôi là trợ lý mua nông sản. Tôi có thể giúp bạn kiểm tra chất lượng, giá, truy xuất nguồn gốc, vận chuyển và điều kiện đổi trả.';

const DEFAULT_FALLBACK_MESSAGE =
    'Hiện tại tôi chưa thể trả lời. Vui lòng thử lại hoặc đặt câu hỏi khác liên quan đến việc chọn mua nông sản.';

const createMessage = (
    role: AiChatRole,
    content: string,
    sources?: AiChatSource[],
): AiChatMessage => ({
    id: `${Date.now()}-${Math.random().toString(16).slice(2)}`,
    role,
    content,
    createdAt: new Date().toISOString(),
    ...(sources?.length ? { sources } : {}),
});

function buildContextualMessage(userMessage: string, buyerContext?: string | null) {
    const trimmedContext = buyerContext?.trim();

    if (!trimmedContext) {
        return userMessage;
    }

    return [
        'Boi canh san pham/nguoi mua:',
        trimmedContext,
        '',
        'Cau hoi:',
        userMessage,
    ].join('\n');
}

export function useBuyerAiChatSession(options: BuyerAiChatSessionOptions = {}) {
    const welcomeMessage = options.welcomeMessage ?? DEFAULT_WELCOME_MESSAGE;
    const fallbackMessage = options.fallbackMessage ?? DEFAULT_FALLBACK_MESSAGE;

    const [messages, setMessages] = useState<AiChatMessage[]>(() => [
        createMessage('assistant', welcomeMessage),
    ]);
    const [isSending, setIsSending] = useState(false);

    const reset = useCallback(() => {
        setMessages([createMessage('assistant', welcomeMessage)]);
    }, [welcomeMessage]);

    const sendMessage = useCallback(async (userMessage: string, buyerContext?: string | null) => {
        const trimmedMessage = userMessage.trim();
        if (!trimmedMessage || isSending) {
            return null;
        }

        setMessages((prev) => [...prev, createMessage('user', trimmedMessage)]);
        setIsSending(true);

        try {
            const response = await sendAiChatMessage(buildContextualMessage(trimmedMessage, buyerContext));
            const assistantText = response.answer?.trim() || fallbackMessage;
            const assistantMessage = createMessage('assistant', assistantText, response.sources);
            setMessages((prev) => [...prev, assistantMessage]);
            return assistantMessage;
        } catch {
            const assistantMessage = createMessage('assistant', fallbackMessage);
            setMessages((prev) => [...prev, assistantMessage]);
            return assistantMessage;
        } finally {
            setIsSending(false);
        }
    }, [fallbackMessage, isSending]);

    return {
        messages,
        isSending,
        sendMessage,
        reset,
    };
}
