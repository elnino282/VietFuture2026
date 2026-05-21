import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook } from '@testing-library/react';
import type { ReactNode } from 'react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { taskKeys } from '../model/keys';
import { useUpdateTask } from './hooks';

const httpMocks = vi.hoisted(() => ({
  put: vi.fn(),
}));

vi.mock('@/shared/api/http', () => ({
  default: {
    put: httpMocks.put,
  },
}));

function createQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: { retry: false, staleTime: 0, gcTime: 0 },
      mutations: { retry: false },
    },
  });
}

describe('task hooks flow', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('invalidates season/workspace task queries after update', async () => {
    httpMocks.put.mockResolvedValueOnce({
      data: {
        status: 200,
        code: 'SUCCESS',
        message: 'OK',
        result: {
          taskId: 101,
          userId: 7,
          userName: 'Worker One',
          seasonId: 33,
          seasonName: 'Rice - Plot A',
          title: 'Spray Field A',
          description: 'Pesticide run',
          plannedDate: '2026-06-01',
          dueDate: '2026-06-20',
          status: 'PENDING',
          actualStartDate: null,
          actualEndDate: null,
          notes: 'note-101',
          createdAt: '2026-05-10T10:00:00',
        },
      },
    });

    const queryClient = createQueryClient();
    const invalidateSpy = vi.spyOn(queryClient, 'invalidateQueries');
    const wrapper = ({ children }: { children: ReactNode }) => (
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    );

    const { result } = renderHook(() => useUpdateTask(33), { wrapper });

    await result.current.mutateAsync({
      id: 101,
      data: {
        title: 'Spray Field A',
        description: 'Pesticide run',
        seasonId: 33,
        plannedDate: '2026-06-01',
        dueDate: '2026-06-20',
        notes: 'note-101',
        assigneeUserId: 501,
      },
    });

    expect(invalidateSpy).toHaveBeenCalledWith({
      queryKey: taskKeys.detail(101),
    });
    expect(invalidateSpy).toHaveBeenCalledWith({
      queryKey: taskKeys.listBySeason(33),
    });
    expect(invalidateSpy).toHaveBeenCalledWith({
      queryKey: taskKeys.listWorkspace(),
    });
  });
});
