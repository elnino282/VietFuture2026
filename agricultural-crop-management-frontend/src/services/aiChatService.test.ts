import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import {
  AI_CHAT_TIMEOUT_MS,
  EMPTY_AI_RESPONSE_MESSAGE,
  OFFLINE_AI_SERVICE_MESSAGE,
  sendAiChatMessage,
} from './aiChatService';

describe('sendAiChatMessage', () => {
  const fetchMock = vi.fn();

  beforeEach(() => {
    fetchMock.mockReset();
    vi.stubGlobal('fetch', fetchMock);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('posts a trimmed message with default top_k to the AI proxy', async () => {
    fetchMock.mockResolvedValue(
      new Response(
        JSON.stringify({
          answer: 'Nguon nuoc tuoi can duoc kiem soat.',
          sources: [
            {
              file_name: 'vietgap.md',
              heading: 'Nguon nuoc',
              page: 3,
              snippet: 'Nguon nuoc khong bi o nhiem.',
              score: 0.12,
              file_path: '/internal/vietgap.md',
            },
          ],
        }),
        { status: 200 },
      ),
    );

    const response = await sendAiChatMessage('  VietGAP yeu cau nguon nuoc ra sao?  ');

    expect(fetchMock).toHaveBeenCalledWith(
      '/ai-api/v1/ai/chat',
      expect.objectContaining({
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          message: 'VietGAP yeu cau nguon nuoc ra sao?',
          top_k: 4,
        }),
      }),
    );
    expect(response).toEqual({
      answer: 'Nguon nuoc tuoi can duoc kiem soat.',
      sources: [
        {
          file_name: 'vietgap.md',
          heading: 'Nguon nuoc',
          page: 3,
          snippet: 'Nguon nuoc khong bi o nhiem.',
        },
      ],
    });
  });

  it('normalizes malformed source fields and honors custom topK', async () => {
    fetchMock.mockResolvedValue(
      new Response(
        JSON.stringify({
          answer: 'Hay tao mua vu truoc khi phan cong cong viec.',
          sources: [
            { file_name: 'workflow.md', heading: 'Mua vu', page: '5', snippet: 'Buoc tiep theo.' },
            { file_name: 42, heading: null, page: -1, snippet: {} },
          ],
        }),
        { status: 200 },
      ),
    );

    const response = await sendAiChatMessage('Sau khi tao mua vu can lam gi?', 2);

    expect(JSON.parse(fetchMock.mock.calls[0][1].body as string)).toEqual({
      message: 'Sau khi tao mua vu can lam gi?',
      top_k: 2,
    });
    expect(response.sources).toEqual([
      {
        file_name: 'workflow.md',
        heading: 'Mua vu',
        page: 5,
        snippet: 'Buoc tiep theo.',
      },
    ]);
  });

  it('uses a chat timeout between 45 and 60 seconds', () => {
    expect(AI_CHAT_TIMEOUT_MS).toBeGreaterThanOrEqual(45000);
    expect(AI_CHAT_TIMEOUT_MS).toBeLessThanOrEqual(60000);
  });

  it('uses the empty-response fallback when the answer is blank', async () => {
    fetchMock.mockResolvedValue(
      new Response(JSON.stringify({ answer: '   ', sources: [] }), { status: 200 }),
    );

    await expect(sendAiChatMessage('Hoi lai')).resolves.toEqual({
      answer: EMPTY_AI_RESPONSE_MESSAGE,
      sources: [],
    });
  });

  it('throws the offline message for failed requests', async () => {
    fetchMock.mockResolvedValue(new Response('Service unavailable', { status: 503 }));

    await expect(sendAiChatMessage('Kiem tra AI')).rejects.toThrow(OFFLINE_AI_SERVICE_MESSAGE);
  });
});
