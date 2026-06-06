import { Button, Badge } from '@/shared/ui';
import { Home, Building2, Pencil, Trash2, Star } from 'lucide-react';
import { cn } from '@/shared/lib';

interface Address {
  id: number;
  name: string;
  phone: string;
  province: string;
  ward: string;
  street: string;
  detail?: string;
  label: 'HOME' | 'OFFICE';
  isDefault: boolean;
}

interface AddressCardProps {
  address: Address;
  onEdit: (address: Address) => void;
  onDelete: (id: number) => void;
  onSetDefault: (id: number) => void;
}

const LABEL_CONFIG = {
  HOME: { text: 'Nhà riêng', icon: Home, color: 'text-emerald-600 bg-emerald-50 border-emerald-100' },
  OFFICE: { text: 'Văn phòng', icon: Building2, color: 'text-blue-600 bg-blue-50 border-blue-100' },
};

export function AddressCard({ address, onEdit, onDelete, onSetDefault }: AddressCardProps) {
  const fullAddress = [
    address.street,
    address.ward,
    address.province,
  ]
    .filter(Boolean)
    .join(', ');

  const labelConfig = LABEL_CONFIG[address.label];
  const LabelIcon = labelConfig.icon;

  return (
    <div
      className={cn(
        'group rounded-2xl border bg-white p-5 transition-all duration-200 hover:shadow-sm',
        address.isDefault
          ? 'border-emerald-200 ring-1 ring-emerald-100'
          : 'border-gray-100 hover:border-gray-200'
      )}
    >
      {/* Top row: Name, phone, badges */}
      <div className="flex flex-wrap items-center gap-2">
        <span className="text-base font-semibold text-gray-900">{address.name}</span>
        <span className="text-sm text-gray-400">|</span>
        <span className="text-sm text-gray-600">{address.phone}</span>

        <div className="flex items-center gap-2 ml-auto">
          {address.isDefault && (
            <Badge className="gap-1 border-emerald-200 bg-emerald-50 text-emerald-700 hover:bg-emerald-50">
              <Star className="h-3 w-3 fill-emerald-500 text-emerald-500" />
              Mặc định
            </Badge>
          )}
          <Badge
            variant="outline"
            className={cn('gap-1 border', labelConfig.color)}
          >
            <LabelIcon className="h-3 w-3" />
            {labelConfig.text}
          </Badge>
        </div>
      </div>

      {/* Address text */}
      <p className="mt-2.5 text-sm leading-relaxed text-gray-600">{fullAddress}</p>
      {address.detail && (
        <p className="mt-1 text-sm text-gray-400">{address.detail}</p>
      )}

      {/* Actions */}
      <div className="mt-4 flex items-center gap-2 border-t border-gray-50 pt-3">
        <Button
          variant="ghost"
          size="sm"
          onClick={() => onEdit(address)}
          className="gap-1.5 rounded-lg text-gray-600 hover:bg-emerald-50 hover:text-emerald-700"
        >
          <Pencil className="h-3.5 w-3.5" />
          Sửa
        </Button>
        {!address.isDefault && (
          <>
            <Button
              variant="ghost"
              size="sm"
              onClick={() => onSetDefault(address.id)}
              className="gap-1.5 rounded-lg text-gray-600 hover:bg-emerald-50 hover:text-emerald-700"
            >
              <Star className="h-3.5 w-3.5" />
              Đặt mặc định
            </Button>
            <Button
              variant="ghost"
              size="sm"
              onClick={() => onDelete(address.id)}
              className="ml-auto gap-1.5 rounded-lg text-gray-400 hover:bg-red-50 hover:text-red-600"
              aria-label="Xóa địa chỉ"
            >
              <Trash2 className="h-3.5 w-3.5" />
            </Button>
          </>
        )}
      </div>
    </div>
  );
}
