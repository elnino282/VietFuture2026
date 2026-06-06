import { Button, CardContent, Label, Switch } from '@/shared/ui';
import { useProfileMe } from '@/entities/user';
import { useAuth } from '@/features/auth';
import {
  ContactInfoCard,
  ProfileHeroCard,
  SectionCardHeader,
} from '@/features/shared/profile';
import { useI18n } from '@/shared/lib/hooks/useI18n';
import {
  AdminContentCard,
  AdminHeaderCard,
  AdminPageContainer,
} from '@/features/admin/shared/ui';
import { Bell, Clock, Loader2, Shield, User } from 'lucide-react';
import { useMemo, useState } from 'react';
import type {
  AdminProfileData,
  NotificationPreferences,
  RecentActivity,
} from '../types';
import { EditProfileDialog } from './EditProfileDialog';

export function AdminProfile() {
  const { user } = useAuth();
  const { t, locale } = useI18n();
  const [editDialogOpen, setEditDialogOpen] = useState(false);
  const [notifications, setNotifications] = useState<NotificationPreferences>({
    systemAlerts: true,
    incidentReports: true,
  });

  const {
    data: profile,
    isLoading: profileLoading,
    isFetching,
  } = useProfileMe();

  const hasSessionProfile = !!user?.profile;

  const profileData: AdminProfileData = useMemo(() => {
    const rawUsername = profile?.username || user?.username || 'admin';
    const username = rawUsername.includes('@') ? rawUsername.split('@')[0] : rawUsername;
    const apiFullName = profile?.fullName?.trim();
    const sessionFullName = user?.profile?.fullName?.trim();
    const fullName = apiFullName || sessionFullName || username;

    const addressParts = [profile?.wardName, profile?.provinceName].filter(Boolean);
    const address = addressParts.length > 0 ? addressParts.join(', ') : t('profile.notAvailable');

    const rawJoinedDate = profile?.joinedDate || user?.profile?.joinedDate;
    const joinedDate = rawJoinedDate
      ? new Date(rawJoinedDate).toLocaleDateString(locale, {
          day: '2-digit', month: 'short', year: 'numeric',
        })
      : t('profile.notAvailable');

    return {
      id: Number(profile?.id ?? user?.profile?.id ?? user?.id ?? 0),
      username,
      displayName: fullName,
      email:
        profile?.email ||
        user?.profile?.email ||
        user?.email ||
        t('profile.notAvailable'),
      phone: profile?.phone || user?.profile?.phone || t('profile.notAvailable'),
      address,
      bio: undefined,
      role: 'admin',
      status:
        (profile?.status || user?.profile?.status) === 'ACTIVE' ? 'active' : 'inactive',
      joinedDate,
      lastLogin: t('profile.notAvailable'),
      provinceId: profile?.provinceId ?? user?.profile?.provinceId ?? undefined,
      wardId: profile?.wardId ?? user?.profile?.wardId ?? undefined,
    };
  }, [profile, user, t, locale]);

  const recentActivities: RecentActivity[] = [];

  const getInitials = (name: string) =>
    name
      .split(' ')
      .map((n) => n[0])
      .join('')
      .toUpperCase();

  if (profileLoading && !hasSessionProfile) {
    return (
      <AdminPageContainer className="flex items-center justify-center text-muted-foreground">
        <Loader2 className="w-6 h-6 animate-spin mr-2" />
        {t('profile.loading')}
      </AdminPageContainer>
    );
  }

  return (
    <AdminPageContainer>
        <AdminHeaderCard
          title={t('admin.profile.title', 'Admin Profile')}
          metadata={
            <>
            {isFetching && !profileLoading && (
              <Loader2 className="w-3 h-3 animate-spin text-primary" />
            )}
            </>
          }
          actions={
            <Button
              onClick={() => setEditDialogOpen(true)}
              className="w-full rounded-[14px] sm:w-auto"
            >
              <User className="w-4 h-4 mr-2" />
              {t('profile.editProfile')}
            </Button>
          }
        />

        <ProfileHeroCard
          displayName={profileData.displayName}
          username={profileData.username}
          initials={getInitials(profileData.displayName)}
          roleIcon={Shield}
          roleLabel={t('admin.profile.administrator', 'Administrator')}
          isActive={profileData.status === 'active'}
          userId={profileData.id}
          joinedDate={profileData.joinedDate}
          lastLogin={profileData.lastLogin}
          labels={{
            userId: t('profile.userId'),
            joinedDate: t('profile.joinedDate'),
            lastLogin: t('profile.lastLogin'),
            status: t('profile.active'),
          }}
        />

        <ContactInfoCard
          email={profileData.email}
          phone={profileData.phone}
          address={profileData.address}
          wardCode={profileData.wardId}
          labels={{
            title: t('profile.contactInfo.title'),
            email: t('profile.contactInfo.email'),
            phone: t('profile.contactInfo.phone'),
            address: t('profile.contactInfo.address'),
          }}
        />

        {/* Recent Activity Card */}
        <AdminContentCard>
          <SectionCardHeader icon={Clock} title={t('profile.recentActivity.title')} />
          <CardContent className="px-6 pb-6 sm:px-8 sm:pb-8">
            {recentActivities.length === 0 ? (
              <p className="text-sm text-muted-foreground">
                {t('profile.recentActivity.empty')}
              </p>
            ) : (
              <div className="space-y-0">
                {recentActivities.map((activity, index) => (
                  <div
                    key={activity.id}
                    className={`flex items-start gap-4 py-4 ${
                      index !== recentActivities.length - 1 ? 'border-b border-border' : ''
                    }`}
                  >
                    <div className="bg-primary/10 rounded-2xl p-2 mt-0.5">
                      <User className="w-4 h-4 text-primary" />
                    </div>
                    <div className="flex-1 space-y-1">
                      <p className="text-sm text-muted-foreground">{activity.date}</p>
                      <p className="text-base text-foreground">{activity.description}</p>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </CardContent>
        </AdminContentCard>

        {/* Notifications Card */}
        <AdminContentCard>
          <SectionCardHeader icon={Bell} title={t('profile.notifications.title')} />
          <CardContent className="space-y-4 px-6 pb-6 sm:px-8 sm:pb-8">
            <div className="bg-muted border border-border rounded-2xl p-4 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
              <div className="space-y-1">
                <Label htmlFor="system-alerts" className="text-sm font-medium text-foreground">
                  {t('admin.profile.notifications.systemAlerts.label', 'Receive System Alerts')}
                </Label>
                <p className="text-xs text-muted-foreground">
                  {t(
                    'admin.profile.notifications.systemAlerts.description',
                    'Get notified about system events and updates'
                  )}
                </p>
              </div>
              <Switch
                id="system-alerts"
                checked={notifications.systemAlerts}
                onCheckedChange={(checked) =>
                  setNotifications((prev) => ({ ...prev, systemAlerts: checked }))
                }
                className="data-[state=checked]:bg-primary"
              />
            </div>

            <div className="bg-muted border border-border rounded-2xl p-4 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
              <div className="space-y-1">
                <Label htmlFor="incident-reports" className="text-sm font-medium text-foreground">
                  {t('admin.profile.notifications.incidentReports.label', 'Receive Incident Reports')}
                </Label>
                <p className="text-xs text-muted-foreground">
                  {t(
                    'admin.profile.notifications.incidentReports.description',
                    'Get immediate alerts for new incidents and issues'
                  )}
                </p>
              </div>
              <Switch
                id="incident-reports"
                checked={notifications.incidentReports}
                onCheckedChange={(checked) =>
                  setNotifications((prev) => ({ ...prev, incidentReports: checked }))
                }
                className="data-[state=checked]:bg-primary"
              />
            </div>
          </CardContent>
        </AdminContentCard>

        <EditProfileDialog
          open={editDialogOpen}
          onOpenChange={setEditDialogOpen}
          profileData={profileData}
        />
    </AdminPageContainer>
  );
}
