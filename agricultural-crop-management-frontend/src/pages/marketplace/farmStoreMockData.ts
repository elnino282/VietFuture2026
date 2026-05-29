/**
 * Supplementary mock data for the Farm Store page.
 *
 * These fields are NOT yet available from the backend API.
 * Once the API supports them, replace this file's usage with real API calls.
 */

export type FarmStoreCertification = {
  id: string;
  name: string;
  icon: string;
};

export type FarmStoreDeliveryInfo = {
  areas: string[];
  estimatedDays: string;
  freeShippingMinimum: number | null;
  note: string;
};

export type FarmStoreEnrichedData = {
  followers: number;
  rating: number;
  ratingCount: number;
  yearsActive: number;
  ordersCompleted: number;
  chatResponseRate: number;
  certifications: FarmStoreCertification[];
  seasonalHighlights: string[];
  deliveryInfo: FarmStoreDeliveryInfo;
  introduction: string;
};

/**
 * Mock product shape for supplementing the product grid
 * when the API returns fewer than 4 products for a farm.
 */
export type FarmStoreMockProduct = {
  id: number;
  slug: string;
  name: string;
  imageUrl: string;
  category: string;
  farmName: string | null;
  farmId: number;
  region: string | null;
  price: number;
  unit: string;
  availableQuantity: number;
  traceable: boolean;
};

/**
 * Returns enriched farm store data for a given farmId.
 * Falls back to sensible defaults for unknown IDs.
 */
export function getFarmStoreEnrichedData(farmId: number): FarmStoreEnrichedData {
  const dataByFarmId: Record<number, Partial<FarmStoreEnrichedData>> = {
    1: {
      followers: 1_240,
      rating: 4.8,
      ratingCount: 156,
      yearsActive: 5,
      ordersCompleted: 890,
      chatResponseRate: 100,
      certifications: [
        { id: "vietgap", name: "VietGAP", icon: "🛡️" },
        { id: "organic", name: "Hữu cơ", icon: "🌿" },
      ],
      seasonalHighlights: [
        "Rau cải ngọt thu hoạch tháng 5–7",
        "Dưa lưới Đà Lạt vụ hè",
        "Cà chua bi hữu cơ quanh năm",
      ],
      introduction:
        "Canh tác rau hữu cơ tại Đà Lạt với hơn 5 năm kinh nghiệm.",
    },
    2: {
      followers: 860,
      rating: 4.6,
      ratingCount: 98,
      yearsActive: 3,
      ordersCompleted: 520,
      chatResponseRate: 98,
      certifications: [
        { id: "organic", name: "Hữu cơ", icon: "🌿" },
        { id: "globalg.a.p", name: "GlobalG.A.P", icon: "🌍" },
      ],
      seasonalHighlights: [
        "Rau mùa mưa Cần Thơ",
        "Giỏ rau theo mùa giao tận nơi",
      ],
      introduction:
        "Sản phẩm nông nghiệp hữu cơ từ vùng đồng bằng sông Cửu Long.",
    },
    3: {
      followers: 430,
      rating: 4.5,
      ratingCount: 67,
      yearsActive: 2,
      ordersCompleted: 310,
      chatResponseRate: 95,
      certifications: [{ id: "vietgap", name: "VietGAP", icon: "🛡️" }],
      seasonalHighlights: ["Cà chua beef Đà Lạt vụ xuân", "Ớt chuông hữu cơ"],
      introduction:
        "Rau củ cao cấp từ vùng Lâm Đồng, chú trọng chất lượng.",
    },
  };

  const overrides = dataByFarmId[farmId] ?? {};

  return {
    followers: overrides.followers ?? 100,
    rating: overrides.rating ?? 4.0,
    ratingCount: overrides.ratingCount ?? 10,
    yearsActive: overrides.yearsActive ?? 1,
    ordersCompleted: overrides.ordersCompleted ?? 50,
    chatResponseRate: overrides.chatResponseRate ?? 95,
    certifications: overrides.certifications ?? [],
    seasonalHighlights: overrides.seasonalHighlights ?? [],
    deliveryInfo: overrides.deliveryInfo ?? {
      areas: ["TP. Hồ Chí Minh", "Hà Nội", "Đà Nẵng", "Cần Thơ"],
      estimatedDays: "2–4 ngày",
      freeShippingMinimum: 500_000,
      note: "Giao hàng bằng xe lạnh, đảm bảo tươi ngon.",
    },
    introduction:
      overrides.introduction ??
      "Nông trại cam kết cung cấp nông sản sạch, an toàn.",
  };
}

/**
 * Returns supplementary mock products for display testing.
 * These fill out the 4-column grid when the API returns < 4 items.
 * IDs use negative values to avoid collision with real API products.
 */
export function getFarmStoreMockProducts(farmId: number): FarmStoreMockProduct[] {
  return [
    {
      id: -1001,
      slug: "bap-ngot-da-lat",
      name: "Bắp ngọt Đà Lạt",
      imageUrl: "https://images.unsplash.com/photo-1551754655-cd27e38d2076?w=400&h=300&fit=crop",
      category: "CORN",
      farmName: null,
      farmId,
      region: "Đà Lạt",
      price: 22_000,
      unit: "kg",
      availableQuantity: 150,
      traceable: true,
    },
    {
      id: -1002,
      slug: "bap-nep-non",
      name: "Bắp nếp non",
      imageUrl: "https://images.unsplash.com/photo-1606567595334-d39972c85dbe?w=400&h=300&fit=crop",
      category: "CORN",
      farmName: null,
      farmId,
      region: "Đà Lạt",
      price: 28_000,
      unit: "kg",
      availableQuantity: 80,
      traceable: true,
    },
    {
      id: -1003,
      slug: "gao-st25",
      name: "Gạo ST25",
      imageUrl: "https://images.unsplash.com/photo-1586201375761-83865001e31c?w=400&h=300&fit=crop",
      category: "RICE",
      farmName: null,
      farmId,
      region: "Sóc Trăng",
      price: 45_000,
      unit: "kg",
      availableQuantity: 200,
      traceable: true,
    },
    {
      id: -1004,
      slug: "dau-nanh-huu-co",
      name: "Đậu nành hữu cơ",
      imageUrl: "https://images.unsplash.com/photo-1615485290382-441e4d049cb5?w=400&h=300&fit=crop",
      category: "SOYBEAN",
      farmName: null,
      farmId,
      region: "Đồng Tháp",
      price: 32_000,
      unit: "kg",
      availableQuantity: 120,
      traceable: true,
    },
  ];
}
