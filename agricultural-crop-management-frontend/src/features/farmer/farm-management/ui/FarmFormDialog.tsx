import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    BackButton,
    Button,
    Input,
    Label,
    AddressSelector,
    type AddressValue,
} from '@/shared/ui';
import { useCreateFarm } from '../hooks/useCreateFarm';
import { useUpdateFarm } from '../hooks/useUpdateFarm';
import type { Farm, FarmDetailResponse } from '@/entities/farm';
import { Controller, useWatch } from 'react-hook-form';
import { useId } from 'react';
import { useTranslation } from 'react-i18next';

type FarmFormInitialData = Pick<FarmDetailResponse, 'name' | 'provinceId' | 'wardId' | 'area' | 'active'> | Farm;

interface FarmFormDialogProps {
    open: boolean;
    onOpenChange: (open: boolean) => void;
    mode: 'create' | 'edit';
    farm?: FarmFormInitialData;
    farmId?: number;
    onCreated?: (farm: FarmDetailResponse) => void;
}

/**
 * Farm form dialog for create and edit operations
 */
export function FarmFormDialog({
    open,
    onOpenChange,
    mode,
    farm,
    farmId,
    onCreated,
}: FarmFormDialogProps) {
    const { t } = useTranslation();
    const nameErrorId = useId();
    const areaErrorId = useId();
    const isCreate = mode === 'create';

    // Pass dialog close callback to create hook for immediate close after success
    const createHook = useCreateFarm((createdFarm) => {
        onCreated?.(createdFarm);
        onOpenChange(false);
    });
    const updateHook = useUpdateFarm(farmId ?? 0, farm);

    // Render separate forms to avoid type conflicts
    if (isCreate) {
        const { form, handleSubmit, isSubmitting } = createHook;
        const nameError = form.formState.errors.name?.message;
        const areaError = form.formState.errors.area?.message;
        const closeCreateWithConfirm = () => {
            if (isSubmitting) return;
            if (
                form.formState.isDirty &&
                !window.confirm(t('common.unsavedChangesConfirm', 'You have unsaved changes. Leave this page?'))
            ) {
                return;
            }
            onOpenChange(false);
        };
        const handleCreateOpenChange = (nextOpen: boolean) => {
            if (isSubmitting && !nextOpen) return;
            if (!nextOpen) {
                closeCreateWithConfirm();
                return;
            }
            onOpenChange(true);
        };

        return (
            <Dialog open={open} onOpenChange={handleCreateOpenChange}>
                <DialogContent className="sm:max-w-[500px]" closeDisabled={isSubmitting}>
                    <DialogHeader>
                        <BackButton onClick={closeCreateWithConfirm} className="w-fit" />
                        <DialogTitle>{t('farms.dialog.createFarmTitle')}</DialogTitle>
                        <DialogDescription>
                            {t('farms.dialog.createFarmDescription')}
                        </DialogDescription>
                    </DialogHeader>
                    <form onSubmit={handleSubmit} className="space-y-4">
                        <div className="space-y-2">
                            <Label htmlFor="name" required>
                                {t('farms.form.farmName')}
                            </Label>
                            <Input
                                id="name"
                                {...form.register('name')}
                                placeholder={t('farms.form.farmNamePlaceholder')}
                                maxLength={255}
                                aria-invalid={!!nameError}
                                aria-describedby={nameError ? nameErrorId : undefined}
                            />
                            {nameError && (
                                <p id={nameErrorId} className="text-sm text-destructive">
                                    {nameError}
                                </p>
                            )}
                        </div>

                        <Controller
                            name="provinceId"
                            control={form.control}
                            render={({ field: provinceField, fieldState: provinceFieldState }) => {
                                const wardId = useWatch({ control: form.control, name: 'wardId' });
                                const wardError = form.formState.errors.wardId?.message;
                                const combinedError = provinceFieldState.error?.message || wardError;

                                return (
                                    <AddressSelector
                                        value={{
                                            provinceId: provinceField.value ?? null,
                                            wardId: wardId ?? null,
                                        }}
                                        onChange={(address: AddressValue) => {
                                            provinceField.onChange(address.provinceId ?? undefined);
                                            if (address.wardId == null) {
                                                form.resetField('wardId');
                                            } else {
                                                form.setValue('wardId', address.wardId);
                                            }
                                        }}
                                        error={combinedError}
                                        disabled={isSubmitting}
                                        label={t('farms.form.farmAddress')}
                                        description={t('farms.form.farmAddressCreateDescription')}
                                        required
                                    />
                                );
                            }}
                        />

                        <div className="space-y-2">
                            <Label htmlFor="area">{t('farms.form.areaHectares')}</Label>
                            <Input
                                id="area"
                                type="number"
                                step="0.01"
                                {...form.register('area', {
                                    setValueAs: (v: string) => (v === '' || v === null ? null : Number(v)),
                                })}
                                placeholder={t('farms.form.areaHectaresPlaceholder')}
                                aria-invalid={!!areaError}
                                aria-describedby={areaError ? areaErrorId : undefined}
                            />
                            {areaError && (
                                <p id={areaErrorId} className="text-sm text-destructive">
                                    {areaError}
                                </p>
                            )}
                        </div>

                        <DialogFooter>
                            <Button
                                type="button"
                                variant="outline"
                                onClick={closeCreateWithConfirm}
                                disabled={isSubmitting}
                            >
                                {t('common.cancel')}
                            </Button>
                            <Button type="submit" disabled={isSubmitting}>
                                {isSubmitting ? t('common.creating') : t('common.create')}
                            </Button>
                        </DialogFooter>
                    </form>
                </DialogContent>
            </Dialog>
        );
    }

    // Edit mode
    const { form, handleSubmit, isSubmitting } = updateHook;
    const nameError = form.formState.errors.name?.message;
    const areaError = form.formState.errors.area?.message;
    const closeEditWithConfirm = () => {
        if (isSubmitting) return;
        if (
            form.formState.isDirty &&
            !window.confirm(t('common.unsavedChangesConfirm', 'You have unsaved changes. Leave this page?'))
        ) {
            return;
        }
        onOpenChange(false);
    };
    const handleEditOpenChange = (nextOpen: boolean) => {
        if (isSubmitting && !nextOpen) return;
        if (!nextOpen) {
            closeEditWithConfirm();
            return;
        }
        onOpenChange(true);
    };

    return (
        <Dialog open={open} onOpenChange={handleEditOpenChange}>
            <DialogContent className="sm:max-w-[500px]" closeDisabled={isSubmitting}>
                <DialogHeader>
                    <BackButton onClick={closeEditWithConfirm} className="w-fit" />
                    <DialogTitle>{t('farms.dialog.editFarmTitle')}</DialogTitle>
                    <DialogDescription>
                        {t('farms.dialog.editFarmDescription')}
                    </DialogDescription>
                </DialogHeader>
                <form onSubmit={handleSubmit} className="space-y-4">
                    <div className="space-y-2">
                        <Label htmlFor="name" required>
                            {t('farms.form.farmName')}
                        </Label>
                        <Input
                            id="name"
                            {...form.register('name')}
                            placeholder={t('farms.form.farmNamePlaceholder')}
                            maxLength={255}
                            aria-invalid={!!nameError}
                            aria-describedby={nameError ? nameErrorId : undefined}
                        />
                        {nameError && (
                            <p id={nameErrorId} className="text-sm text-destructive">
                                {nameError}
                            </p>
                        )}
                    </div>

                    <Controller
                        name="provinceId"
                        control={form.control}
                        render={({ field: provinceField, fieldState: provinceFieldState }) => {
                            const wardId = useWatch({ control: form.control, name: 'wardId' });
                            const wardError = form.formState.errors.wardId?.message;
                            const combinedError = provinceFieldState.error?.message || wardError;

                            return (
                                <AddressSelector
                                    value={{
                                        provinceId: provinceField.value ?? null,
                                        wardId: wardId ?? null,
                                    }}
                                    onChange={(address: AddressValue) => {
                                        provinceField.onChange(address.provinceId ?? null);
                                        form.setValue('wardId', address.wardId ?? null);
                                    }}
                                    error={combinedError}
                                    disabled={isSubmitting}
                                    label={t('farms.form.farmAddress')}
                                    description={t('farms.form.farmAddressEditDescription')}
                                />
                            );
                        }}
                    />

                    <div className="space-y-2">
                        <Label htmlFor="area">{t('farms.form.areaHectares')}</Label>
                        <Input
                            id="area"
                            type="number"
                            step="0.01"
                            {...form.register('area', {
                                setValueAs: (v: string) => (v === '' || v === null ? null : Number(v)),
                            })}
                            placeholder={t('farms.form.areaHectaresPlaceholder')}
                            aria-invalid={!!areaError}
                            aria-describedby={areaError ? areaErrorId : undefined}
                        />
                        {areaError && (
                            <p id={areaErrorId} className="text-sm text-destructive">
                                {areaError}
                            </p>
                        )}
                    </div>

                    <DialogFooter>
                        <Button
                            type="button"
                            variant="outline"
                            onClick={closeEditWithConfirm}
                        disabled={isSubmitting}
                    >
                        {t('common.cancel')}
                    </Button>
                    <Button type="submit" disabled={isSubmitting}>
                        {isSubmitting ? t('common.updating') : t('common.update')}
                    </Button>
                    </DialogFooter>
                </form>
            </DialogContent>
        </Dialog>
    );
}



