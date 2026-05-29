import { Trash2, Loader2 } from 'lucide-react';
import { toast } from 'sonner';
import {
    AlertDialog,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle,
    Button,
} from '@/shared/ui';
import { useDeleteFarm } from '@/entities/farm';
import { useTranslation } from 'react-i18next';

interface FarmDeleteDialogProps {
    open: boolean;
    onOpenChange: (open: boolean) => void;
    farmId: number;
    farmName: string;
    onDeleteSuccess?: () => void;
}

/**
 * Farm delete confirmation dialog with integrated deletion
 */
export function FarmDeleteDialog({
    open,
    onOpenChange,
    farmId,
    farmName,
    onDeleteSuccess,
}: FarmDeleteDialogProps) {
    const { t } = useTranslation();
    const deleteMutation = useDeleteFarm({
        onSuccess: () => {
            toast.success(t('farms.toast.deleteSuccess'));
            onOpenChange(false);
            onDeleteSuccess?.();
        },
        onError: (error: Error) => {
            const errorMessage = error.message || t('farms.toast.deleteError');
            toast.error(errorMessage);
        },
    });

    const handleConfirm = () => {
        deleteMutation.mutate({ id: farmId, name: farmName });
    };

    const handleOpenChange = (nextOpen: boolean) => {
        if (deleteMutation.isPending && !nextOpen) return;
        onOpenChange(nextOpen);
    };

    return (
        <AlertDialog open={open} onOpenChange={handleOpenChange}>
            <AlertDialogContent>
                <AlertDialogHeader>
                    <AlertDialogTitle>{t('farms.dialog.deleteFarmTitle')}</AlertDialogTitle>
                    <AlertDialogDescription>
                        {t('farms.dialog.deleteFarmConfirmDescription', { name: farmName })}
                    </AlertDialogDescription>
                </AlertDialogHeader>
                <AlertDialogFooter>
                    <AlertDialogCancel disabled={deleteMutation.isPending}>
                        {t('common.cancel')}
                    </AlertDialogCancel>
                    <Button
                        variant="destructive"
                        onClick={handleConfirm}
                        disabled={deleteMutation.isPending}
                    >
                        {deleteMutation.isPending ? (
                            <>
                                <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                                {t('common.deleting')}
                            </>
                        ) : (
                            <>
                                <Trash2 className="w-4 h-4 mr-2" />
                                {t('common.delete')}
                            </>
                        )}
                    </Button>
                </AlertDialogFooter>
            </AlertDialogContent>
        </AlertDialog>
    );
}



