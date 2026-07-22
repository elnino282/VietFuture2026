import { beforeEach, describe, expect, it, vi } from 'vitest';
import httpClient from '@/shared/api/http';
import { changePassword } from './auth';

vi.mock('@/shared/api/http', () => ({
  default: {
    put: vi.fn(),
  },
}));

const mockedHttpClient = httpClient as unknown as {
  put: ReturnType<typeof vi.fn>;
};

describe('auth api', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockedHttpClient.put.mockResolvedValue({ data: { code: 'SUCCESS' } });
  });

  it('changes the current user password through the shared user endpoint', async () => {
    await changePassword({
      currentPassword: 'old-password',
      newPassword: 'new-password',
    });

    expect(httpClient.put).toHaveBeenCalledWith('/api/v1/user/change-password', {
      currentPassword: 'old-password',
      newPassword: 'new-password',
    });
  });
});
