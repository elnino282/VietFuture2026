import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useCreateFarm as useCreateFarmEntity, FarmCreateRequestSchema, type FarmCreateRequest, type FarmDetailResponse } from '@/entities/farm';
import { toast } from 'sonner';
import { useTranslation } from 'react-i18next';

/**
 * Feature hook for creating a farm with form validation and toast handling
 */
export function useCreateFarm(onSuccessCallback?: (farm: FarmDetailResponse) => void) {
    const { t } = useTranslation();
    const form = useForm<FarmCreateRequest>({
        resolver: zodResolver(FarmCreateRequestSchema),
        defaultValues: {
            name: '',
            provinceId: undefined,
            wardId: undefined,
            area: null,
        },
    });

    const mutation = useCreateFarmEntity({
        onSuccess: (data) => {
            console.log('[useCreateFarm] Success! Created farm:', data);
            toast.success(t('farms.toast.createFarmSuccess'));
            form.reset({
                name: '',
                provinceId: undefined,
                wardId: undefined,
                area: null,
            }, {
                keepErrors: false,
                keepDirty: false,
                keepIsSubmitted: false,
                keepTouched: false,
                keepIsValid: false,
                keepSubmitCount: false,
            });
            // Call the dialog close callback after form reset
            onSuccessCallback?.(data);
        },
        onError: (error: any) => {
            console.error('[useCreateFarm] Error occurred:', error);
            console.error('[useCreateFarm] Error response:', error?.response);
            console.error('[useCreateFarm] Error response data:', error?.response?.data);
            console.error('[useCreateFarm] Error status:', error?.response?.status);

            const errorData = error?.response?.data;
            const errorCode = errorData?.code || errorData?.result?.code;
            let message = t('farms.toast.createFarmError');

            // Handle specific error codes from backend
            if (errorCode === 'ERR_FARM_NAME_EXISTS') {
                message = t('farms.errors.nameExists', { name: form.getValues('name') });
            } else if (errorCode === 'ERR_KEY_INVALID') {
                message = t('farms.errors.invalidInput');
            } else if (errorCode === 'ERR_UNAUTHORIZED' || error?.response?.status === 401) {
                message = t('farms.errors.unauthorized');
            } else if (errorCode === 'ERR_FORBIDDEN' || error?.response?.status === 403) {
                message = t('farms.errors.forbiddenCreate');
            } else if (errorData?.message) {
                message = errorData.message;
            } else if (error?.message) {
                message = error.message;
            }

            toast.error(message, {
                description: errorCode ? t('farms.errors.errorCode', { code: errorCode }) : undefined,
            });
        },
    });

    const handleSubmit = form.handleSubmit((data: FarmCreateRequest) => {
        console.log('[useCreateFarm] Submitting farm data:', data);
        mutation.mutate(data as FarmCreateRequest);
    });

    return {
        form,
        handleSubmit,
        isSubmitting: mutation.isPending,
        isSuccess: mutation.isSuccess,
        error: mutation.error,
    };
}



