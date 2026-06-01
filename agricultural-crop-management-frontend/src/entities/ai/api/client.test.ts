import { beforeEach, describe, expect, it, vi } from 'vitest';
import httpClient from '@/shared/api/http';
import { aiApi } from './client';

vi.mock('@/shared/api/http', () => ({
    default: {
        get: vi.fn(),
        post: vi.fn(),
    },
}));

describe('aiApi buyer chat', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('posts buyer chat requests to the buyer AI endpoint', async () => {
        vi.mocked(httpClient.post).mockResolvedValue({
            data: {
                status: 200,
                code: 'SUCCESS',
                message: 'OK',
                result: {
                    userMessage: 'Should I buy this lot?',
                    buyerContext: 'black beans',
                    assistantMessage: 'Check traceability first.',
                },
            },
        });

        const result = await aiApi.buyerChat({
            userMessage: 'Should I buy this lot?',
            buyerContext: 'black beans',
        });

        expect(httpClient.post).toHaveBeenCalledWith('/api/v1/buyer/ai/chat', {
            userMessage: 'Should I buy this lot?',
            buyerContext: 'black beans',
        });
        expect(result.assistantMessage).toBe('Check traceability first.');
    });
});
