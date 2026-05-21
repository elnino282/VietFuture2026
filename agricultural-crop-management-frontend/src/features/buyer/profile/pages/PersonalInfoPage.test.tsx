import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { PersonalInfoPage } from './PersonalInfoPage';

const {
  mockMutateAsync,
  mockToastSuccess,
  mockToastError,
} = vi.hoisted(() => ({
  mockMutateAsync: vi.fn(),
  mockToastSuccess: vi.fn(),
  mockToastError: vi.fn(),
}));

vi.mock('@/features/auth', () => ({
  useAuth: () => ({
    user: {
      id: 10,
      username: 'buyer-user',
      email: 'buyer@example.com',
      role: 'buyer' as const,
      profile: {
        id: 10,
        fullName: 'Buyer User',
        email: 'buyer@example.com',
        phone: '0900000000',
      },
    },
  }),
}));

vi.mock('@/entities/user', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/entities/user')>();
  return {
    ...actual,
    useProfileMe: () => ({
      data: {
        id: 10,
        username: 'buyer-user',
        email: 'buyer@example.com',
        fullName: 'Buyer User',
        phone: '0900000000',
      },
    }),
    useProfileUpdate: () => ({
      mutateAsync: mockMutateAsync,
      isPending: false,
    }),
  };
});

vi.mock('../components/PersonalInfoForm', () => ({
  PersonalInfoForm: ({
    onSave,
    onCancel,
  }: {
    onSave: (data: { name: string; phone: string }) => Promise<void>;
    onCancel: () => void;
  }) => (
    <div>
      <button
        type="button"
        onClick={() => {
          void onSave({ name: 'Updated Buyer', phone: '0911111111' }).catch(() => {
            // no-op in test double
          });
        }}
      >
        Trigger Save
      </button>
      <button type="button" onClick={onCancel}>
        Trigger Cancel
      </button>
    </div>
  ),
}));

vi.mock('sonner', () => ({
  toast: {
    success: mockToastSuccess,
    error: mockToastError,
  },
}));

describe('PersonalInfoPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockMutateAsync.mockResolvedValue({
      id: 10,
      username: 'buyer-user',
      email: 'buyer@example.com',
      fullName: 'Updated Buyer',
      phone: '0911111111',
    });
  });

  it('calls profile update mutation with real payload when saving personal info', async () => {
    render(<PersonalInfoPage />);
    const user = userEvent.setup();

    await user.click(screen.getByRole('button', { name: 'Chỉnh sửa' }));
    await user.click(screen.getByRole('button', { name: 'Trigger Save' }));

    await waitFor(() => {
      expect(mockMutateAsync).toHaveBeenCalledWith({
        fullName: 'Updated Buyer',
        phone: '0911111111',
      });
    });
    expect(mockToastSuccess).toHaveBeenCalledWith('Cập nhật thông tin thành công');
  });

  it('shows backend error message and does not emit success toast when update fails', async () => {
    const apiError = {
      isAxiosError: true,
      response: { data: { message: 'Số điện thoại đã được sử dụng' } },
    };
    mockMutateAsync.mockRejectedValue(apiError);

    render(<PersonalInfoPage />);
    const user = userEvent.setup();

    await user.click(screen.getByRole('button', { name: 'Chỉnh sửa' }));
    await user.click(screen.getByRole('button', { name: 'Trigger Save' }));

    await waitFor(() => {
      expect(mockToastError).toHaveBeenCalledWith('Số điện thoại đã được sử dụng');
    });
    expect(mockToastSuccess).not.toHaveBeenCalled();
  });
});
