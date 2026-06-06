import { useState } from 'react';
import { useDeleteFarm as useDeleteFarmEntity } from '@/entities/farm';
import { toast } from 'sonner';
import { useTranslation } from 'react-i18next';

/**
 * Feature hook for deleting a farm with conflict detection
 * Handles backend errors when farm has plots or seasons
 */
export function useDeleteFarm() {
    const { t } = useTranslation();
    const [isConfirmOpen, setIsConfirmOpen] = useState(false);
    const [farmToDelete, setFarmToDelete] = useState<{ id: number; name: string } | null>(null);

    const mutation = useDeleteFarmEntity({
        onSuccess: (data, variables) => {
            // Use variables.name instead of farmToDelete state to avoid race conditions
            toast.success(t('farms.toast.deleteFarmNamedSuccess', { name: variables.name }));
            setIsConfirmOpen(false);
            setFarmToDelete(null);
        },
        onError: (error: any) => {
            const statusCode = error?.response?.status;
            const message = error?.response?.data?.message || error?.message;

            // Check for conflict errors (400/409) indicating plots/seasons exist
            if (statusCode === 400 || statusCode === 409) {
                toast.error(
                    message || t('farms.toast.deleteFarmConflict')
                );
            } else {
                toast.error(message || t('farms.toast.deleteError'));
            }
        },
    });

    const handleDeleteRequest = (farmId: number, farmName: string) => {
        setFarmToDelete({ id: farmId, name: farmName });
        setIsConfirmOpen(true);
    };

    const handleDeleteConfirm = () => {
        console.log('[useDeleteFarm Feature] handleDeleteConfirm called');
        console.log('[useDeleteFarm Feature] farmToDelete:', farmToDelete);
        if (farmToDelete) {
            // Pass the entire farm object { id, name } to mutation
            mutation.mutate(farmToDelete);
        }
    };

    const handleDeleteCancel = () => {
        setIsConfirmOpen(false);
        setFarmToDelete(null);
    };

    return {
        isConfirmOpen,
        farmToDelete,
        handleDeleteRequest,
        handleDeleteConfirm,
        handleDeleteCancel,
        isDeleting: mutation.isPending,
    };
}



