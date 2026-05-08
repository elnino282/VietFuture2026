import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import type { MarketplaceFarmerProductFormOptions, MarketplaceProductDetail } from '@/shared/api';
import { marketplaceApi } from '@/shared/api';
import { SellerProductFormPage } from './SellerProductFormPage';

// Polyfills for JSDOM limitations with Radix UI
if (!Element.prototype.hasPointerCapture) {
  Element.prototype.hasPointerCapture = () => false;
}
if (!Element.prototype.scrollIntoView) {
  Element.prototype.scrollIntoView = () => {};
}

const navigateMock = vi.fn();

vi.mock('react-router-dom', async (importOriginal) => {
  const actual = await importOriginal<typeof import('react-router-dom')>();
  return {
    ...actual,
    useNavigate: () => navigateMock,
  };
});

vi.mock('@/shared/api', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/shared/api')>();
  return {
    ...actual,
    marketplaceApi: {
      ...actual.marketplaceApi,
      getFarmerProductFormOptions: vi.fn(),
      getFarmerProductDetail: vi.fn(),
      createFarmerProduct: vi.fn(),
      updateFarmerProduct: vi.fn(),
      updateFarmerProductStatus: vi.fn(),
    },
  };
});

function formOptionsFixture(): MarketplaceFarmerProductFormOptions {
  return {
    farms: [{ id: 10, name: 'Green Valley Farm' }],
    seasons: [{ id: 20, seasonName: 'Spring 2026', farmId: 10 }],
    lots: [
      {
        id: 30,
        lotCode: 'LOT-30',
        farmId: 10,
        farmName: 'Green Valley Farm',
        seasonId: 20,
        seasonName: 'Spring 2026',
        availableQuantity: 12,
        harvestedAt: '2026-04-01T08:00:00Z',
        unit: 'kg',
        productName: 'Jasmine rice',
        productVariant: null,
        linkedProductId: null,
        linkedProductStatus: null,
      },
    ],
  };
}

function productFixture(): MarketplaceProductDetail {
  return {
    id: 99,
    slug: 'jasmine-rice',
    name: 'Jasmine rice listing',
    category: 'Grain',
    shortDescription: 'Fresh rice',
    description: 'Freshly harvested rice',
    price: 45000,
    unit: 'kg',
    stockQuantity: 6,
    availableQuantity: 6,
    imageUrl: 'https://example.com/rice.jpg',
    imageUrls: [],
    farmerUserId: 7,
    farmerDisplayName: 'Farmer A',
    farmId: 10,
    farmName: 'Green Valley Farm',
    seasonId: 20,
    seasonName: 'Spring 2026',
    lotId: 30,
    region: 'Lam Dong',
    traceable: true,
    ratingAverage: 0,
    ratingCount: 0,
    status: 'HIDDEN',
    statusReason: 'Missing harvest certificate',
    rejectionReason: 'Photo quality is too low',
    createdAt: '2026-04-02T08:00:00Z',
    updatedAt: '2026-04-03T08:00:00Z',
    traceabilityCode: 'TRACE-99',
  };
}

function renderPage(route = '/farmer/marketplace-products/new') {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[route]}>
        <Routes>
          <Route path="/farmer/marketplace-products/new" element={<SellerProductFormPage />} />
          <Route path="/farmer/marketplace-products/:id/edit" element={<SellerProductFormPage />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

async function chooseOption(label: string, option: string) {
  const user = userEvent.setup();
  const trigger = screen.getByRole('combobox', { name: label });
  await user.click(trigger);
  const optionElement = await screen.findByRole('option', { name: option }, { timeout: 3000 });
  await user.click(optionElement);
}

describe('SellerProductFormPage', () => {
  beforeEach(() => {
    navigateMock.mockReset();
    vi.mocked(marketplaceApi.getFarmerProductFormOptions).mockResolvedValue({ result: formOptionsFixture() } as never);
    vi.mocked(marketplaceApi.getFarmerProductDetail).mockResolvedValue({ result: productFixture() } as never);
    vi.mocked(marketplaceApi.createFarmerProduct).mockResolvedValue({ result: productFixture() } as never);
    vi.mocked(marketplaceApi.updateFarmerProduct).mockResolvedValue({ result: productFixture() } as never);
    vi.mocked(marketplaceApi.updateFarmerProductStatus).mockResolvedValue({ result: productFixture() } as never);
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it('creates a harvest-backed listing end-to-end', async () => {
    renderPage();
    const user = userEvent.setup();

    await screen.findByRole('heading', { name: 'Create marketplace listing' });

    await user.type(screen.getByLabelText(/Listing name/i), 'Premium jasmine rice');
    await user.type(screen.getByLabelText(/Category/i), 'Grain');
    await user.clear(screen.getByLabelText(/Price/i));
    await user.type(screen.getByLabelText(/Price/i), '55000');
    await user.type(screen.getByLabelText(/Main image URL/i), 'https://example.com/premium.jpg');
    await user.type(screen.getByLabelText(/Short description/i), 'Fragrant rice from spring harvest');
    await user.type(screen.getByLabelText(/Full description/i), 'Packed after harvest with traceability enabled.');

    await chooseOption('Farm', 'Green Valley Farm');
    await chooseOption('Season', 'Spring 2026');
    await chooseOption('Harvested lot', 'LOT-30 - Jasmine rice');

    await user.clear(screen.getByLabelText(/Quantity to sell/i));
    await user.type(screen.getByLabelText(/Quantity to sell/i), '8');
    await user.click(screen.getByRole('button', { name: 'Create draft' }));

    await waitFor(() => {
      expect(marketplaceApi.createFarmerProduct).toHaveBeenCalledWith({
        name: 'Premium jasmine rice',
        category: 'Grain',
        shortDescription: 'Fragrant rice from spring harvest',
        description: 'Packed after harvest with traceability enabled.',
        price: 55000,
        stockQuantity: 8,
        imageUrl: 'https://example.com/premium.jpg',
        lotId: 30,
      });
    });
    expect(navigateMock).toHaveBeenCalledWith('/farmer/marketplace-products');
  }, 10000);

  it('blocks listing quantity that exceeds selected lot availability', async () => {
    renderPage();
    const user = userEvent.setup();

    await screen.findByRole('heading', { name: 'Create marketplace listing' });

    await user.type(screen.getByLabelText(/Listing name/i), 'Premium jasmine rice');
    await user.clear(screen.getByLabelText(/Price/i));
    await user.type(screen.getByLabelText(/Price/i), '55000');
    await chooseOption('Farm', 'Green Valley Farm');
    await chooseOption('Season', 'Spring 2026');
    await chooseOption('Harvested lot', 'LOT-30 - Jasmine rice');

    await user.clear(screen.getByLabelText(/Quantity to sell/i));
    await user.type(screen.getByLabelText(/Quantity to sell/i), '13');

    expect(screen.getByRole('button', { name: 'Create draft' })).toBeDisabled();
    expect(marketplaceApi.createFarmerProduct).not.toHaveBeenCalled();
  }, 10000);

  it('shows admin rejection reason when editing a rejected listing', async () => {
    renderPage('/farmer/marketplace-products/99/edit');

    expect(await screen.findByText('Admin reason: Photo quality is too low')).toBeInTheDocument();
  });
});

