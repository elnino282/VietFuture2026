import { SoilTypeArrayResponseSchema } from '../model/schemas';
import type { SoilTypeArrayResponse } from '../model/types';

// ═══════════════════════════════════════════════════════════════
// SOIL TYPE API CLIENT
// Pure Axios calls with Zod validation (no React dependencies)
// ═══════════════════════════════════════════════════════════════

export const soilTypeApi = {
    /**
     * List all soil types
     */
    listAll: async (): Promise<SoilTypeArrayResponse> => {
        const data: SoilTypeArrayResponse = [
            { id: 1, soilName: 'Ferralsols', description: 'Highly weathered tropical soils rich in iron and aluminum oxides' },
            { id: 2, soilName: 'Chernozems', description: 'Dark, humus-rich grassland soils with high natural fertility' },
            { id: 3, soilName: 'Fluvisols', description: 'Young alluvial soils found on floodplains and river deposits' },
            { id: 4, soilName: 'Podzol', description: 'Acidic, leached soils with organic and iron-rich subsurface layers' },
            { id: 5, soilName: 'Peat', description: 'Organic soils formed from partially decomposed plant material' },
            { id: 6, soilName: 'Arenosols', description: 'Coarse-textured soils with low water retention' },
        ];
        return SoilTypeArrayResponseSchema.parse(data);
    },
};
