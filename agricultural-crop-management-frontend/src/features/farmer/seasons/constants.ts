import type { Activity } from './types';

// Note: MOCK_SEASONS and MOCK_ACTIVITIES removed - now using entity API hooks

export const DEFAULT_PAGINATION = {
  rowsPerPage: 10,
  rowsPerPageOptions: [10, 25, 50],
};

// Type export for Activity to ensure it's still available
export type { Activity };



