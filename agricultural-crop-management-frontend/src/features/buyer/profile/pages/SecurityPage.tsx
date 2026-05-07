import { changePassword, type ChangePasswordPayload } from '@/api/auth';
import { Card, CardContent, CardHeader } from '@/shared/ui';
import { toast } from 'sonner';
import { PasswordChangeForm } from '../components/PasswordChangeForm';

export function SecurityPage() {
  const handlePasswordChange = async (data: ChangePasswordPayload) => {
    try {
      await changePassword(data);
      toast.success('Đổi mật khẩu thành công');
    } catch (error: unknown) {
      const message = error instanceof Error ? error.message : 'Có lỗi xảy ra khi đổi mật khẩu';
      toast.error(message);
      throw error;
    }
  };

  return (
    <div className="mx-auto max-w-4xl">
      <Card>
        <CardHeader>
          <h2 className="text-xl font-bold">Đổi mật khẩu</h2>
        </CardHeader>

        <CardContent>
          <PasswordChangeForm onSave={handlePasswordChange} />
        </CardContent>
      </Card>
    </div>
  );
}
