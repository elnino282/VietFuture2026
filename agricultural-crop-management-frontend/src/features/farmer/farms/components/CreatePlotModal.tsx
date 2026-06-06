import { useForm } from 'react-hook-form';
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/shared/ui/dialog';
import { Button } from '@/shared/ui/button';
import { Input } from '@/shared/ui/input';
import { Label } from '@/shared/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/shared/ui/select';
import { CreatePlotRequest } from '../types';
import { useTranslation } from 'react-i18next';
import { PLOT_STATUS_OPTIONS, SOIL_TYPE_OPTIONS } from '@/features/farmer/shared/plotOptions';

interface CreatePlotModalProps {
    open: boolean;
    onOpenChange: (open: boolean) => void;
    onSubmit: (data: CreatePlotRequest) => Promise<void>;
}

export function CreatePlotModal({ open, onOpenChange, onSubmit }: CreatePlotModalProps) {
    const { t } = useTranslation();
    const { register, handleSubmit, reset, setValue, watch, formState: { errors, isSubmitting } } = useForm<CreatePlotRequest>({
        defaultValues: {
            status: 'IN_USE',
            area: 0
        }
    });
    const selectedSoilType = watch('soilType');
    const selectedStatus = watch('status');

    const handleFormSubmit = async (data: CreatePlotRequest) => {
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
            <DialogContent className="sm:max-w-[425px]">
                <DialogHeader>
                    <DialogTitle>{t('farms.dialog.createPlotTitle')}</DialogTitle>
                    <DialogDescription>
                        {t('farms.dialog.createPlotDescription')}
                    </DialogDescription>
                </DialogHeader>

                <form onSubmit={handleSubmit(handleFormSubmit)} className="space-y-4 py-4">
                    <div className="space-y-2">
                        <Label htmlFor="plotName" required>{t('farms.form.plotName')}</Label>
                        <Input 
                            id="plotName" 
                            {...register('plotName', { required: t('farms.validation.plotNameRequired') })}
                        />
                        {errors.plotName && <p className="text-sm text-red-500">{errors.plotName.message}</p>}
                    </div>

                    <div className="space-y-2">
                        <Label htmlFor="area" required>{t('plots.table.area')}</Label>
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
                        <Label htmlFor="soilType" required>{t('farms.form.soilType')}</Label>
                        <input
                            type="hidden"
                            {...register('soilType', { required: t('farms.validation.soilTypeRequired') })}
                        />
                        <Select
                            value={selectedSoilType || ''}
                            onValueChange={(value) => setValue('soilType', value, { shouldValidate: true })}
                        >
                            <SelectTrigger id="soilType">
                                <SelectValue placeholder={t('farms.form.selectSoilType')} />
                            </SelectTrigger>
                            <SelectContent>
                                {SOIL_TYPE_OPTIONS.map((soil) => (
                                    <SelectItem key={soil.value} value={soil.value}>
                                        {t(soil.labelKey)}
                                    </SelectItem>
                                ))}
                            </SelectContent>
                        </Select>
                        {errors.soilType && <p className="text-sm text-red-500">{errors.soilType.message}</p>}
                    </div>

                    <div className="space-y-2">
                        <Label htmlFor="status" required>{t('farms.form.initialStatus')}</Label>
                        <input
                            type="hidden"
                            {...register('status', { required: t('farms.validation.statusRequired') })}
                        />
                        <Select
                            value={selectedStatus || 'IN_USE'}
                            onValueChange={(value) => setValue('status', value as CreatePlotRequest['status'], { shouldValidate: true })}
                        >
                            <SelectTrigger id="status">
                                <SelectValue placeholder={t('farms.form.selectStatus')} />
                            </SelectTrigger>
                            <SelectContent>
                                {PLOT_STATUS_OPTIONS.map((status) => (
                                    <SelectItem key={status.value} value={status.value}>
                                        {t(status.labelKey)}
                                    </SelectItem>
                                ))}
                            </SelectContent>
                        </Select>
                        {errors.status && <p className="text-sm text-red-500">{errors.status.message}</p>}
                    </div>

                    <DialogFooter>
                        <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>{t('common.cancel')}</Button>
                        <Button type="submit" disabled={isSubmitting}>
                            {isSubmitting ? t('common.creating') : t('farms.dialog.createPlotTitle')}
                        </Button>
                    </DialogFooter>
                </form>
            </DialogContent>
        </Dialog>
    );
}



