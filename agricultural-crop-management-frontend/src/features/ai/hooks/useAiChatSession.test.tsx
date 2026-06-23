import { act, renderHook, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { sendAiChatMessage } from '@/services/aiChatService';
import { useAiChatSession } from './useAiChatSession';

vi.mock('@/services/aiChatService', () => ({
    sendAiChatMessage: vi.fn(),
}));

const sendAiChatMessageMock = vi.mocked(sendAiChatMessage);

describe('useAiChatSession', () => {
    beforeEach(() => {
        sendAiChatMessageMock.mockReset();
    });

    it('sends crop context to local RAG and stores sources on assistant messages', async () => {
        sendAiChatMessageMock.mockResolvedValue({
            answer: 'Use clean irrigation water.',
            sources: [{ file_name: 'vietgap.md', heading: 'Water', snippet: 'Check water.' }],
        });

        const { result } = renderHook(() =>
            useAiChatSession({ welcomeMessage: 'Welcome farmer' }),
        );

        await act(async () => {
            await result.current.sendMessage('  What water is allowed?  ', ' rice plot A ');
        });

        expect(sendAiChatMessageMock).toHaveBeenCalledWith(expect.stringContaining('rice plot A'));
        expect(sendAiChatMessageMock).toHaveBeenCalledWith(expect.stringContaining('What water is allowed?'));

        await waitFor(() => {
            expect(result.current.messages).toHaveLength(3);
        });
        expect(result.current.messages.map((message) => message.content)).toEqual([
            'Welcome farmer',
            'What water is allowed?',
            'Use clean irrigation water.',
        ]);
        expect(result.current.messages[2].sources).toEqual([
            { file_name: 'vietgap.md', heading: 'Water', snippet: 'Check water.' },
        ]);
    });
});
