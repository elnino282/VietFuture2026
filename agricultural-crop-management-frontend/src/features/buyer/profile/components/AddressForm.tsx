import { useState } from 'react';
import { BackButton, Button, Input, Label, RadioGroup, RadioGroupItem, Checkbox } from '@/shared/ui';
import { useI18n } from '@/hooks/useI18n';
import { Loader2 } from 'lucide-react';

interface AddressFormData {
  name: string;
  phone: string;
  province: string;
  district: string;
  ward: string;
  street: string;
  detail?: string;
  label: 'HOME' | 'OFFICE';
  isDefault: boolean;
}

interface AddressFormProps {
  mode: 'add' | 'edit';
  initialData?: Partial<AddressFormData>;
  onSave: (data: AddressFormData) => Promise<void>;
  onCancel: () => void;
}

export function AddressForm({ mode, initialData, onSave, onCancel }: AddressFormProps) {
  const { t } = useI18n();
  const [formData, setFormData] = useState<Partial<AddressFormData>>({
    name: initialData?.name || '',
    phone: initialData?.phone || '',
    province: initialData?.province || '',
    district: initialData?.district || '',
    ward: initialData?.ward || '',
    street: initialData?.street || '',
    detail: initialData?.detail || '',
    label: initialData?.label || 'HOME',
    isDefault: initialData?.isDefault || false,
  });
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSubmitting(true);
    try {
      await onSave(formData as AddressFormData);
    } finally {
      setIsSubmitting(false);
    }
  };
  const initialSnapshot = JSON.stringify({
    name: initialData?.name || '',
    phone: initialData?.phone || '',
    province: initialData?.province || '',
    district: initialData?.district || '',
    ward: initialData?.ward || '',
    street: initialData?.street || '',
    detail: initialData?.detail || '',
    label: initialData?.label || 'HOME',
    isDefault: initialData?.isDefault || false,
  });
  const isDirty = JSON.stringify(formData) !== initialSnapshot;
  const handleCancel = () => {
    if (
      isDirty &&
      !window.confirm(t('common.unsavedChangesConfirm', 'You have unsaved changes. Leave this page?'))
    ) {
      return;
    }

    onCancel();
  };

  return (
    <div className="rounded-lg bg-gray-50 p-4">
      <BackButton onClick={handleCancel} className="mb-3 w-fit" />
      <h3 className="mb-4 font-bold">
        {mode === 'add' ? 'Thêm địa chỉ mới' : 'Chỉnh sửa địa chỉ'}
      </h3>

      <form onSubmit={handleSubmit} className="space-y-4">
        <div className="grid gap-4 md:grid-cols-2">
          <div>
            <Label htmlFor="name">Họ và tên</Label>
            <Input
              id="name"
              value={formData.name}
              onChange={(e) => setFormData({ ...formData, name: e.target.value })}
              required
              disabled={isSubmitting}
            />
          </div>

          <div>
            <Label htmlFor="phone">Số điện thoại</Label>
            <Input
              id="phone"
              value={formData.phone}
              onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
              required
              disabled={isSubmitting}
            />
          </div>

          <div>
            <Label htmlFor="province">Tỉnh/Thành phố</Label>
            <Input
              id="province"
              value={formData.province}
              onChange={(e) => setFormData({ ...formData, province: e.target.value })}
              required
              disabled={isSubmitting}
              placeholder="Ví dụ: Hồ Chí Minh"
            />
          </div>

          <div>
            <Label htmlFor="district">Quận/Huyện</Label>
            <Input
              id="district"
              value={formData.district}
              onChange={(e) => setFormData({ ...formData, district: e.target.value })}
              required
              disabled={isSubmitting}
              placeholder="Ví dụ: Quận 1"
            />
          </div>

          <div>
            <Label htmlFor="ward">Phường/Xã</Label>
            <Input
              id="ward"
              value={formData.ward}
              onChange={(e) => setFormData({ ...formData, ward: e.target.value })}
              required
              disabled={isSubmitting}
              placeholder="Ví dụ: Phường Bến Nghé"
            />
          </div>

          <div>
            <Label htmlFor="street">Tên đường, Tòa nhà</Label>
            <Input
              id="street"
              value={formData.street}
              onChange={(e) => setFormData({ ...formData, street: e.target.value })}
              required
              disabled={isSubmitting}
            />
          </div>

          <div className="md:col-span-2">
            <Label htmlFor="detail">Chi tiết thêm (Tùy chọn)</Label>
            <Input
              id="detail"
              placeholder="Số nhà, tầng, phòng..."
              value={formData.detail}
              onChange={(e) => setFormData({ ...formData, detail: e.target.value })}
              disabled={isSubmitting}
            />
          </div>
        </div>

        <div>
          <Label>Loại địa chỉ</Label>
          <RadioGroup
            value={formData.label}
            onValueChange={(value) => setFormData({ ...formData, label: value as 'HOME' | 'OFFICE' })}
            disabled={isSubmitting}
            className="mt-2 flex gap-4"
          >
            <div className="flex items-center space-x-2">
              <RadioGroupItem value="HOME" id="home" />
              <Label htmlFor="home" className="cursor-pointer">Nhà riêng</Label>
            </div>
            <div className="flex items-center space-x-2">
              <RadioGroupItem value="OFFICE" id="office" />
              <Label htmlFor="office" className="cursor-pointer">Văn phòng</Label>
            </div>
          </RadioGroup>
        </div>

        <label className="flex items-center space-x-2 cursor-pointer">
          <Checkbox
            checked={formData.isDefault}
            onCheckedChange={(checked) => setFormData({ ...formData, isDefault: !!checked })}
            disabled={isSubmitting}
          />
          <span className="text-sm font-medium">Đặt làm địa chỉ mặc định</span>
        </label>

        <div className="flex justify-end gap-2">
          <Button type="button" variant="outline" onClick={handleCancel} disabled={isSubmitting}>
            Hủy
          </Button>
          <Button type="submit" disabled={isSubmitting}>
            {isSubmitting && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
            Lưu địa chỉ
          </Button>
        </div>
      </form>
    </div>
  );
}
