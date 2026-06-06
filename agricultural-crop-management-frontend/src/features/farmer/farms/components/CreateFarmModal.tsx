import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/shared/ui/dialog';
import { Button } from '@/shared/ui/button';
import { Input } from '@/shared/ui/input';
import { Label } from '@/shared/ui/label';
import { Switch } from '@/shared/ui/switch';
import { AddressSelector } from '@/shared/ui/address-selector';
import { CreateFarmRequest } from '../types';
import { useTranslation } from 'react-i18next';

interface CreateFarmModalProps {
    open: boolean;
    onOpenChange: (open: boolean) => void;
    onSubmit: (data: CreateFarmRequest) => Promise<void>;
}

export function CreateFarmModal({ open, onOpenChange, onSubmit }: CreateFarmModalProps) {
    const { t } = useTranslation();
    const { register, handleSubmit, reset, setValue, watch, formState: { errors, isSubmitting } } = useForm<CreateFarmRequest>({
        defaultValues: {
            farmName: '',
            active: true,
            area: 0
        }
    });

    // We track address separately because AddressSelector uses a complex object
    const [addressError, setAddressError] = useState<string>('');

    const handleFormSubmit = async (data: CreateFarmRequest) => {
        setAddressError('');
        if (!data.provinceId || !data.wardId) {
            setAddressError(t('farms.validation.locationRequired'));
            return;
        }
        
        try {
            await onSubmit(data);
            reset();
            onOpenChange(false);
        } catch (error) {
            console.error(error);
        }
    };

    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent className="sm:max-w-[500px]">
                <DialogHeader>
                    <DialogTitle>{t('farms.dialog.createFarmTitle')}</DialogTitle>
                    <DialogDescription>
                        {t('farms.dialog.createFarmDescription')}
                    </DialogDescription>
                </DialogHeader>

                <form onSubmit={handleSubmit(handleFormSubmit)} className="space-y-4 py-4">
                    <div className="space-y-2">
                        <Label htmlFor="farmName" required>{t('farms.form.farmName')}</Label>
                        <Input 
                            id="farmName" 
                            {...register('farmName', { required: t('farms.validation.farmNameRequired') })}
                        />
                        {errors.farmName && <p className="text-sm text-red-500">{errors.farmName.message}</p>}
                    </div>

                    <div className="space-y-2">
                        <Label htmlFor="area" required>{t('farms.form.totalAreaHectares')}</Label>
                        <Input 
                            id="area" 
                            type="number" 
                            step="0.01" 
                            {...register('area', { 
                                required: t('farms.validation.areaRequired'),
                                min: { value: 0.01, message: t('farms.validation.areaPositive') },
                                valueAsNumber: true
                            })} 
                        />
                        {errors.area && <p className="text-sm text-red-500">{errors.area.message}</p>}
                    </div>

                    <div className="space-y-2">
                         <AddressSelector
                            required
                            label={t('farms.form.location')}
                            error={addressError}
                            onChange={(val) => {
                                setValue('provinceId', val.provinceId || 0);
                                setValue('wardId', val.wardId || 0);
                                setAddressError('');
                            }}
                         />
                    </div>

                    <div className="flex items-center justify-between space-x-2 border p-3 rounded-lg">
                        <Label htmlFor="active" className="cursor-pointer">{t('farms.form.activeStatus')}</Label>
                        <Switch 
                            id="active" 
                            checked={watch('active')} 
                            onCheckedChange={(checked) => setValue('active', checked)} 
                        />
                    </div>

                    <DialogFooter>
                        <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>{t('common.cancel')}</Button>
                        <Button type="submit" disabled={isSubmitting}>
                            {isSubmitting ? t('common.creating') : t('farms.dialog.createFarmTitle')}
                        </Button>
                    </DialogFooter>
                </form>
            </DialogContent>
        </Dialog>
    );
}



