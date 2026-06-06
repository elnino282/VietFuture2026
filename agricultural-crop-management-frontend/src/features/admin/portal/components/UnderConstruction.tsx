import { Calendar } from 'lucide-react';
import { BackButton, Card, CardContent } from '@/shared/ui';
import { useI18n } from '@/shared/lib/hooks/useI18n';

type UnderConstructionProps = {
  title: string;
};

export function UnderConstruction({ title }: UnderConstructionProps) {
  const { t } = useI18n();
  
  return (
    <div className="p-6 max-w-7xl mx-auto space-y-4">
      <BackButton to="/admin/dashboard" className="w-fit" />
      <Card className="border-dashed border-2">
        <CardContent className="flex flex-col items-center justify-center py-16">
          <div className="mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-muted">
            <Calendar className="h-8 w-8 text-muted-foreground" />
          </div>
          <h3 className="mb-2 font-semibold text-lg">{title}</h3>
          <p className="text-sm text-muted-foreground">
            {t('common.underConstruction', 'This view is under construction.')}
          </p>
        </CardContent>
      </Card>
    </div>
  );
}
