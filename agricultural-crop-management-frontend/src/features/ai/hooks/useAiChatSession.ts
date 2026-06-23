import { useCallback, useState } from 'react';
import { sendAiChatMessage, type AiChatSource } from '@/services/aiChatService';

export type AiChatRole = 'assistant' | 'user';

export type AiChatMessage = {
    id: string;
    role: AiChatRole;
    content: string;
    createdAt: string;
    sources?: AiChatSource[];
};

type AiChatSessionOptions = {
    welcomeMessage?: string;
    fallbackMessage?: string;
};

const DEFAULT_WELCOME_MESSAGE =
    'Xin chào! Tôi là trợ lý nông nghiệp. Hãy hỏi về cây trồng, sâu bệnh, đất, tưới tiêu hoặc lịch mùa vụ.';

const DEFAULT_FALLBACK_MESSAGE =
    'Hiện tại tôi chưa thể trả lời. Vui lòng thử lại hoặc đặt câu hỏi khác liên quan đến nông nghiệp.';

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

function buildContextualMessage(userMessage: string, cropContext?: string | null) {
    const trimmedContext = cropContext?.trim();

    if (!trimmedContext) {
        return userMessage;
    }

    return [
        'Boi canh mua vu/cay trong:',
        trimmedContext,
        '',
        'Cau hoi:',
        userMessage,
    ].join('\n');
}

export function useAiChatSession(options: AiChatSessionOptions = {}) {
    const welcomeMessage = options.welcomeMessage ?? DEFAULT_WELCOME_MESSAGE;
    const fallbackMessage = options.fallbackMessage ?? DEFAULT_FALLBACK_MESSAGE;

    const [messages, setMessages] = useState<AiChatMessage[]>(() => [
        createMessage('assistant', welcomeMessage),
    ]);
    const [isSending, setIsSending] = useState(false);

    const reset = useCallback(() => {
        setMessages([createMessage('assistant', welcomeMessage)]);
    }, [welcomeMessage]);

    const sendMessage = useCallback(async (userMessage: string, cropContext?: string | null) => {
        const trimmedMessage = userMessage.trim();
        if (!trimmedMessage || isSending) {
            return null;
        }

        setMessages((prev) => [...prev, createMessage('user', trimmedMessage)]);
        setIsSending(true);

        try {
            const response = await sendAiChatMessage(buildContextualMessage(trimmedMessage, cropContext));
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
