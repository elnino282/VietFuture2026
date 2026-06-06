import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useUpdateFarm as useUpdateFarmEntity, FarmUpdateRequestSchema, type FarmUpdateRequest, type Farm, type FarmDetailResponse } from '@/entities/farm';
import { toast } from 'sonner';
import { useEffect } from 'react';
import { useTranslation } from 'react-i18next';

type FarmFormInitialData = Pick<FarmDetailResponse, 'name' | 'provinceId' | 'wardId' | 'area' | 'active'> | Farm;

/**
 * Feature hook for updating a farm with form validation and toast handling
 */
export function useUpdateFarm(farmId: number, initialData?: FarmFormInitialData) {
    const { t } = useTranslation();
    const form = useForm<FarmUpdateRequest>({
        resolver: zodResolver(FarmUpdateRequestSchema),
        defaultValues: {
            name: '',
            provinceId: null,
            wardId: null,
            area: null,
        },
    });

    // Populate form when initialData changes
    useEffect(() => {
        if (initialData) {
            form.reset({
                name: initialData.name,
                provinceId: initialData.provinceId ?? null,
                wardId: initialData.wardId ?? null,
                area: initialData.area,
                active: initialData.active,
            });
        }
    }, [initialData, form]);

    const mutation = useUpdateFarmEntity({
        onSuccess: () => {
            toast.success(t('farms.toast.updateFarmSuccess'));
        },
        onError: (error: any) => {
            const message = error?.response?.data?.message || error?.message || t('farms.toast.updateFarmError');
            toast.error(message);
        },
    });

    const handleSubmit = form.handleSubmit((data) => {
        mutation.mutate({ id: farmId, data });
    });

    return {
        form,
        handleSubmit,
        isSubmitting: mutation.isPending,
        isSuccess: mutation.isSuccess,
        error: mutation.error,
    };
}



