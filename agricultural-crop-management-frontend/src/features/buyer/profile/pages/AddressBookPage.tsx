import { useState } from 'react';
import { Button, ConfirmDialog } from '@/shared/ui';
import { AddressCard } from '../components/AddressCard';
import { AddressForm } from '../components/AddressForm';
import {
  useMarketplaceAddresses,
  useMarketplaceCreateAddressMutation,
  useMarketplaceUpdateAddressMutation,
  useMarketplaceDeleteAddressMutation,
} from '@/features/marketplace/hooks';
import { toast } from 'sonner';
import { Loader2, MapPin, Plus } from 'lucide-react';
import type { MarketplaceAddress, MarketplaceAddressUpsertRequest } from '@/shared/api';

interface AddressFormData {
  name: string;
  phone: string;
  province: string;
  ward: string;
  street: string;
  detail?: string;
  label: 'HOME' | 'OFFICE';
  isDefault: boolean;
}

interface AddressCardData {
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

function toAddressCardData(address: MarketplaceAddress): AddressCardData {
  return {
    id: address.id,
    name: address.fullName,
    phone: address.phone,
    province: address.province,
    ward: address.ward,
    street: address.street,
    detail: address.detail || undefined,
    label: address.label === 'home' ? 'HOME' : address.label === 'office' ? 'OFFICE' : 'HOME',
    isDefault: address.isDefault,
  };
}

function toMarketplaceAddressUpsertRequest(address: MarketplaceAddress): MarketplaceAddressUpsertRequest {
  return {
    fullName: address.fullName,
    phone: address.phone,
    province: address.province,
    district: address.ward, // Backend requires district; auto-fill with ward
    ward: address.ward,
    street: address.street,
    detail: address.detail || undefined,
    label: address.label,
    isDefault: address.isDefault,
  };
}

export function AddressBookPage() {
  const [showAddForm, setShowAddForm] = useState(false);
  const [editingAddress, setEditingAddress] = useState<AddressCardData | null>(null);
  const [deleteConfirmOpen, setDeleteConfirmOpen] = useState(false);
  const [addressToDelete, setAddressToDelete] = useState<number | null>(null);

  const { data: addresses, isLoading, isError } = useMarketplaceAddresses();
  const createMutation = useMarketplaceCreateAddressMutation();
  const updateMutation = useMarketplaceUpdateAddressMutation();
  const deleteMutation = useMarketplaceDeleteAddressMutation();

  const handleSave = async (data: AddressFormData) => {
    try {
      const request: MarketplaceAddressUpsertRequest = {
        fullName: data.name,
        phone: data.phone,
        province: data.province,
        district: data.ward, // Backend requires district; auto-fill with ward
        ward: data.ward,
        street: data.street,
        detail: data.detail,
        label: data.label === 'HOME' ? 'home' : data.label === 'OFFICE' ? 'office' : 'other',
        isDefault: data.isDefault,
      };

      if (editingAddress) {
        await updateMutation.mutateAsync({
          addressId: editingAddress.id,
          request,
        });
        toast.success('Cập nhật địa chỉ thành công');
        setEditingAddress(null);
      } else {
        await createMutation.mutateAsync(request);
        toast.success('Thêm địa chỉ thành công');
        setShowAddForm(false);
      }
    } catch (error: unknown) {
      toast.error(error instanceof Error ? error.message : 'Có lỗi xảy ra');
      throw error;
    }
  };

  const handleDeleteClick = (id: number) => {
    setAddressToDelete(id);
    setDeleteConfirmOpen(true);
  };

  const handleDeleteConfirm = async () => {
    if (!addressToDelete) return;
    try {
      await deleteMutation.mutateAsync(addressToDelete);
      toast.success('Xóa địa chỉ thành công');
      setDeleteConfirmOpen(false);
      setAddressToDelete(null);
    } catch (error: unknown) {
      toast.error(error instanceof Error ? error.message : 'Có lỗi xảy ra');
      throw error;
    }
  };

  const handleSetDefault = async (id: number) => {
    const address = addresses?.find((a) => a.id === id);
    if (!address) return;
    try {
      const request = toMarketplaceAddressUpsertRequest(address);
      request.isDefault = true;
      await updateMutation.mutateAsync({
        addressId: address.id,
        request,
      });
      toast.success('Đã đặt làm địa chỉ mặc định');
    } catch (error: unknown) {
      toast.error(error instanceof Error ? error.message : 'Có lỗi xảy ra');
      throw error;
    }
  };

  const handleAddClick = () => {
    setEditingAddress(null);
    setShowAddForm(true);
  };

  const handleEditClick = (address: AddressCardData) => {
    setShowAddForm(false);
    setEditingAddress(address);
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-20">
        <Loader2 className="h-6 w-6 animate-spin text-emerald-600" />
      </div>
    );
  }

  if (isError) {
    return (
      <div className="rounded-2xl border border-red-100 bg-white p-8 text-center text-sm text-red-600 shadow-sm">
        Không thể tải sổ địa chỉ.
      </div>
    );
  }

  const addressCardData = addresses?.map(toAddressCardData) || [];

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-semibold text-gray-900">Sổ địa chỉ</h1>
          <p className="mt-1 text-sm text-gray-500">
            Quản lý địa chỉ giao hàng của bạn
          </p>
        </div>
        <Button
          onClick={handleAddClick}
          className="gap-2 rounded-xl bg-emerald-600 text-white shadow-sm hover:bg-emerald-700"
        >
          <Plus className="h-4 w-4" />
          Thêm địa chỉ
        </Button>
      </div>

      {/* Add Form */}
      {showAddForm && (
        <div className="rounded-2xl border border-emerald-100 bg-white p-6 shadow-sm">
          <AddressForm
            mode="add"
            onSave={handleSave}
            onCancel={() => setShowAddForm(false)}
          />
        </div>
      )}

      {/* Edit Form */}
      {editingAddress && (
        <div className="rounded-2xl border border-emerald-100 bg-white p-6 shadow-sm">
          <AddressForm
            mode="edit"
            initialData={editingAddress}
            onSave={handleSave}
            onCancel={() => setEditingAddress(null)}
          />
        </div>
      )}

      {/* Address List */}
      <div className="space-y-3">
        {addressCardData.map((address) => (
          <AddressCard
            key={address.id}
            address={address}
            onEdit={handleEditClick}
            onDelete={handleDeleteClick}
            onSetDefault={handleSetDefault}
          />
        ))}
      </div>

      {/* Empty State */}
      {!addressCardData.length && !showAddForm && (
        <div className="rounded-2xl border border-dashed border-gray-200 bg-white p-12 text-center">
          <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-gray-50">
            <MapPin className="h-7 w-7 text-gray-300" />
          </div>
          <h3 className="mt-4 text-sm font-medium text-gray-900">
            Chưa có địa chỉ nào
          </h3>
          <p className="mt-1 text-sm text-gray-500">
            Thêm địa chỉ đầu tiên để giao hàng nhanh hơn.
          </p>
          <Button
            onClick={handleAddClick}
            className="mt-5 gap-2 rounded-xl bg-emerald-600 text-white shadow-sm hover:bg-emerald-700"
          >
            <Plus className="h-4 w-4" />
            Thêm địa chỉ mới
          </Button>
        </div>
      )}

      <ConfirmDialog
        open={deleteConfirmOpen}
        onOpenChange={setDeleteConfirmOpen}
        title="Xóa địa chỉ"
        description="Bạn có chắc chắn muốn xóa địa chỉ này? Hành động này không thể hoàn tác."
        variant="destructive"
        confirmText="Xóa"
        cancelText="Hủy"
        onConfirm={handleDeleteConfirm}
        isLoading={deleteMutation.isPending}
      />
    </div>
  );
}
