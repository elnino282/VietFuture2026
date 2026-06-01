import { act, renderHook, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { useBuyerAiChatSession } from './useBuyerAiChatSession';

const mocks = vi.hoisted(() => ({
    mutateAsync: vi.fn(),
}));

vi.mock('@/entities/ai', () => ({
    useBuyerAiChat: () => ({
        mutateAsync: mocks.mutateAsync,
        isPending: false,
    }),
}));

describe('useBuyerAiChatSession', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('stores user and assistant messages when buyer chat succeeds', async () => {
        mocks.mutateAsync.mockResolvedValue({
            userMessage: 'Should I buy this lot?',
            buyerContext: 'black beans',
            assistantMessage: 'Check traceability first.',
        });

        const { result } = renderHook(() =>
            useBuyerAiChatSession({ welcomeMessage: 'Welcome buyer' }),
        );

        await act(async () => {
            await result.current.sendMessage('  Should I buy this lot?  ', ' black beans ');
        });

        expect(mocks.mutateAsync).toHaveBeenCalledWith({
            userMessage: 'Should I buy this lot?',
            buyerContext: 'black beans',
        });

        await waitFor(() => {
            expect(result.current.messages).toHaveLength(3);
        });
        expect(result.current.messages.map((message) => message.content)).toEqual([
            'Welcome buyer',
            'Should I buy this lot?',
            'Check traceability first.',
        ]);
    });

    it('adds the configured fallback message when buyer chat fails', async () => {
        mocks.mutateAsync.mockRejectedValue(new Error('network'));

        const { result } = renderHook(() =>
            useBuyerAiChatSession({
                welcomeMessage: 'Welcome buyer',
                fallbackMessage: 'Try again later.',
            }),
        );

        await act(async () => {
            await result.current.sendMessage('Should I buy this lot?', 'black beans');
        });

        await waitFor(() => {
            expect(result.current.messages[result.current.messages.length - 1]?.content).toBe('Try again later.');
        });
    });
});
