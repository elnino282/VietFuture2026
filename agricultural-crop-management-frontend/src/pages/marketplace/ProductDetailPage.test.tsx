import { fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ProductDetailPage } from './ProductDetailPage';

const mocks = vi.hoisted(() => ({
  openAssistant: vi.fn(),
  navigate: vi.fn(),
}));

vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string) => key,
    i18n: { language: 'vi' },
    ready: true,
  }),
  initReactI18next: { type: '3rdParty', init: vi.fn() },
}));

vi.mock('react-router-dom', async (importOriginal) => {
  const actual = await importOriginal<typeof import('react-router-dom')>();
  return {
    ...actual,
    useNavigate: () => mocks.navigate,
  };
});

vi.mock('@/features/auth', () => ({
  useAuth: () => ({
    isAuthenticated: true,
    user: { id: 1, role: 'buyer' },
  }),
}));

vi.mock('@/features/marketplace/ai/BuyerAiAssistantContext', () => ({
  useBuyerAiAssistant: () => ({
    openAssistant: mocks.openAssistant,
  }),
}));

vi.mock('@/features/marketplace/hooks', () => ({
  useMarketplaceAddToCart: () => ({
    addToCart: vi.fn(),
    isAdding: false,
  }),
  useMarketplaceProductDetail: () => ({
    isLoading: false,
    isError: false,
    data: {
      id: 10,
      slug: 'black-beans',
      name: 'Đậu đen hữu cơ',
      category: 'Beans',
      shortDescription: 'Đậu đen loại A',
      description: 'Đậu đen thu hoạch mới.',
      price: 55000,
      unit: 'kg',
      stockQuantity: 100,
      availableQuantity: 80,
      imageUrl: '/black-beans.jpg',
      imageUrls: ['/black-beans.jpg'],
      farmerUserId: 2,
      farmerDisplayName: 'Farm ACM',
      farmId: 4,
      farmName: 'Nông trại xanh',
      seasonId: 9,
      seasonName: 'Vụ xuân',
      lotId: 11,
      region: 'Lâm Đồng',
      traceable: true,
      traceabilityCode: 'TRACE-10',
      ratingAverage: 4.7,
      ratingCount: 12,
      status: 'ACTIVE',
      createdAt: '2026-05-01T00:00:00Z',
      updatedAt: '2026-05-01T00:00:00Z',
    },
  }),
  useMarketplaceProductReviews: () => ({
    isLoading: false,
    isError: false,
    data: { items: [] },
  }),
  useMarketplaceTraceability: () => ({
    data: {
      lot: {
        lotCode: 'LOT-10',
        harvestedAt: '2026-04-28',
      },
      farm: {
        region: 'Đà Lạt',
      },
    },
  }),
}));

function renderPage() {
  return render(
    <MemoryRouter initialEntries={['/marketplace/products/black-beans']}>
      <Routes>
        <Route path="/marketplace/products/:slug" element={<ProductDetailPage />} />
      </Routes>
    </MemoryRouter>,
  );
}

describe('ProductDetailPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('opens buyer AI with product context', async () => {
    renderPage();

    fireEvent.click(screen.getByRole('button', { name: /AI về sản phẩm/i }));

    expect(mocks.openAssistant).toHaveBeenCalledWith({
      context: expect.stringContaining('Sản phẩm: Đậu đen hữu cơ'),
      prompt: expect.stringContaining('Có nên mua sản phẩm này không?'),
    });
    expect(mocks.openAssistant.mock.calls[0][0].context).toContain('Mã lô: LOT-10');
  });

  it('navigates to checkout with buyNowItem in state when Mua ngay is clicked', async () => {
    renderPage();

    const buyButtons = screen.getAllByRole('button', { name: /Mua ngay/i });
    expect(buyButtons.length).toBeGreaterThan(0);
    fireEvent.click(buyButtons[0]);

    expect(mocks.navigate).toHaveBeenCalledWith('/marketplace/checkout', {
      state: {
        buyNowItem: {
          productId: 10,
          name: 'Đậu đen hữu cơ',
          quantity: 1,
          unitPrice: 55000,
          farmerUserId: 2,
          farmerName: 'Nông trại xanh',
          imageUrl: '/black-beans.jpg',
          slug: 'black-beans',
          traceable: true,
        },
      },
    });
  });
});
