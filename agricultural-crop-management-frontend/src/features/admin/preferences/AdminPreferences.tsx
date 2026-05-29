import { Settings } from 'lucide-react';
import { PreferencesForm } from '@/features/shared/preferences';
import { useI18n } from '@/shared/lib/hooks/useI18n';
import { AdminHeaderCard, AdminPageContainer } from '@/features/admin/shared/ui';

export function AdminPreferences() {
  const { t } = useI18n();
  
  return (
    <AdminPageContainer>
      <AdminHeaderCard
        title={t('preferences.title')}
        metadata={<Settings className="w-5 h-5 text-primary" />}
      />
      <PreferencesForm />
    </AdminPageContainer>
  );
}

