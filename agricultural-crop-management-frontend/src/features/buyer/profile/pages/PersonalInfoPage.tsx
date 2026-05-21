import { useState } from 'react';
import { useAuth } from '@/features/auth';
import { useProfileMe, useProfileUpdate } from '@/entities/user';
import { Button, Card, CardContent, CardHeader, Dialog, Input, Label } from '@/shared/ui';
import axios from 'axios';
import { toast } from 'sonner';
import { PersonalInfoForm } from '../components/PersonalInfoForm';

type PersonalInfoFormData = {
  name: string;
  phone: string;
};

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

  return 'Có lỗi xảy ra khi cập nhật thông tin';
}

export function PersonalInfoPage() {
  const { user } = useAuth();
  const profileQuery = useProfileMe();
  const updateProfile = useProfileUpdate();
  const [isEditing, setIsEditing] = useState(false);

  const userData = {
    name: profileQuery.data?.fullName || user?.profile?.fullName || user?.username || '',
    email: profileQuery.data?.email || user?.email || user?.profile?.email || '',
    phone: profileQuery.data?.phone || user?.profile?.phone || '',
  };

  const handleSave = async (data: PersonalInfoFormData) => {
    try {
      await updateProfile.mutateAsync({
        fullName: data.name,
        phone: data.phone,
      });

      toast.success('Cập nhật thông tin thành công');
      setIsEditing(false);
    } catch (error) {
      const message = getErrorMessage(error);
      toast.error(message);
      throw new Error(message);
    }
  };

  return (
    <div className="mx-auto max-w-4xl">
      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
          <h2 className="text-xl font-bold">Thông tin cá nhân</h2>
          <Button
            variant="outline"
            onClick={() => setIsEditing(true)}
            disabled={updateProfile.isPending}
          >
            Chỉnh sửa
          </Button>
        </CardHeader>

        <CardContent>
          <div className="grid gap-4 md:grid-cols-2">
            <div>
              <Label>Họ và tên</Label>
              <Input value={userData.name} disabled className="mt-1" />
            </div>

            <div>
              <Label>Email</Label>
              <Input value={userData.email} disabled className="mt-1" />
            </div>

            <div>
              <Label>Số điện thoại</Label>
              <Input value={userData.phone} disabled className="mt-1" />
            </div>
          </div>
        </CardContent>
      </Card>

      <Dialog open={isEditing} onOpenChange={setIsEditing}>
        <PersonalInfoForm
          initialData={{ name: userData.name, phone: userData.phone }}
          onSave={handleSave}
          onCancel={() => setIsEditing(false)}
        />
      </Dialog>
    </div>
  );
}
