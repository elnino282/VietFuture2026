import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { BuyerAiAssistantDrawer } from './BuyerAiAssistantDrawer';

vi.mock('@/features/ai', async (importOriginal) => ({
  ...(await importOriginal<typeof import('@/features/ai')>()),
  useBuyerAiChatSession: () => ({
    messages: [
      {
        id: 'assistant-1',
        role: 'assistant',
        content: 'Nen kiem tra truy xuat truoc khi mua.',
        createdAt: '2026-06-23T00:00:00.000Z',
        sources: [
          {
            file_name: 'faq-nguoi-mua.md',
            heading: 'Truy xuat',
            page: 1,
            snippet: 'Quet ma QR de xem nguon goc.',
          },
        ],
      },
    ],
    isSending: false,
    sendMessage: vi.fn(),
    reset: vi.fn(),
  }),
}));

describe('BuyerAiAssistantDrawer RAG sources', () => {
  it('renders clean source details under assistant answers', () => {
    render(
      <BuyerAiAssistantDrawer
        open
        onOpenChange={vi.fn()}
        requestId={1}
        buyerContext="black beans"
      />,
    );

    expect(screen.getByText('Nguồn tham khảo')).toBeInTheDocument();
    expect(screen.getByText('Truy xuat')).toBeInTheDocument();
    expect(screen.getByText('faq-nguoi-mua.md')).toBeInTheDocument();
    expect(screen.getByText('Trang 1')).toBeInTheDocument();
    expect(screen.getByText('Quet ma QR de xem nguon goc.')).toBeInTheDocument();
  });
});
