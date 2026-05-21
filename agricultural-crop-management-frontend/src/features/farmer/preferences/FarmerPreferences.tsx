import { PageContainer, Separator } from '@/shared/ui';
import { ChangePasswordSection } from '@/features/farmer/account/components/ChangePasswordSection';
import { PreferencesForm } from '@/features/shared/preferences';
import { useI18n } from '@/hooks/useI18n';
import { Settings } from 'lucide-react';

export function FarmerPreferences() {
  const { t } = useI18n();
  
  return (
    <PageContainer variant="narrow" className="space-y-6">
        <div className="flex items-center gap-3">
          <Settings className="w-6 h-6 text-primary" />
          <h1 className="text-xl font-bold text-foreground">{t('preferences.title')}</h1>
        </div>

        <ChangePasswordSection />

        <Separator />

        <PreferencesForm />
    </PageContainer>
  );
}

