import { type ChangePasswordPayload, useProfileChangePassword } from '@/entities/user';
import { BackButton, Card, CardContent, CardHeader } from '@/shared/ui';
import axios from 'axios';
import { useState } from 'react';
import { toast } from 'sonner';
import { PasswordChangeForm } from '../components/PasswordChangeForm';

function getErrorMessage(error: unknown): string {
  if (axios.isAxiosError(error)) {
    const apiMessage = (error.response?.data as { message?: string } | undefined)?.message;
    if (apiMessage) {
      return apiMessage;
    }
  }

  if (error instanceof Error && error.message) {
    return error.message;
  }

  return 'Có lỗi xảy ra khi đổi mật khẩu';
}

export function SecurityPage() {
  const changePassword = useProfileChangePassword();
  const [isPasswordFormDirty, setIsPasswordFormDirty] = useState(false);

  const handlePasswordChange = async (data: ChangePasswordPayload) => {
    try {
      await changePassword.mutateAsync({
        currentPassword: data.currentPassword,
        newPassword: data.newPassword,
      });
      toast.success('Đổi mật khẩu thành công');
    } catch (error: unknown) {
      const message = getErrorMessage(error);
      toast.error(message);
      throw new Error(message);
    }
  };

  return (
    <div className="mx-auto max-w-4xl space-y-4">
      <BackButton
        to="/marketplace"
        confirmOnLeave={isPasswordFormDirty}
        className="w-fit"
      />
      <Card>
        <CardHeader>
          <h2 className="text-xl font-bold">Đổi mật khẩu</h2>
        </CardHeader>

        <CardContent>
          <PasswordChangeForm
            onSave={handlePasswordChange}
            onDirtyChange={setIsPasswordFormDirty}
          />
        </CardContent>
      </Card>
    </div>
  );
}
