import { Laptop, Monitor, Smartphone } from 'lucide-react';
import { useState } from 'react';
import { toast } from 'sonner';
import { useI18n } from '@/shared/lib/hooks/useI18n';
import {
    PLACEHOLDER_AUDIT_LOGS,
    PLACEHOLDER_BACKUP_POINTS,
    PLACEHOLDER_DEVICES,
    PLACEHOLDER_INTEGRATIONS,
    PLACEHOLDER_NOTIFICATION_SETTINGS,
    PLACEHOLDER_PERMISSIONS,
    PLACEHOLDER_ROLES,
} from '../constants';
import type {
    AuditLog,
    BackupPoint,
    Device,
    Integration,
    NotificationSetting,
    Permission,
    Role,
    SecuritySettings,
    SettingsSection,
    SystemPreferences,
} from '../types';

export function useSystemSettings() {
    const { t } = useI18n();
    const [activeSection, setActiveSection] = useState<SettingsSection>('system');
    const [addRoleModalOpen, setAddRoleModalOpen] = useState(false);
    const [restoreModalOpen, setRestoreModalOpen] = useState(false);
    const [showApiKey, setShowApiKey] = useState<Record<string, boolean>>({});

    // System Preferences State
    const [systemPrefs, setSystemPrefs] = useState<SystemPreferences>({
        language: 'en',
        timeZone: 'UTC+7',
        unitSystem: 'metric',
        dateFormat: 'dd/MM/yyyy',
        currency: 'VND',
        theme: 'light',
    });

    // Security State
    const [securitySettings, setSecuritySettings] = useState<SecuritySettings>({
        enable2FA: true,
        sessionTimeout: true,
        passwordPolicy: true,
    });

    // Data States
    // TODO: Replace all placeholder data with real API calls
    const [roles] = useState<Role[]>(PLACEHOLDER_ROLES);
    const [permissions, setPermissions] = useState<Record<string, Permission>>(PLACEHOLDER_PERMISSIONS);
    const [devices] = useState<Device[]>(PLACEHOLDER_DEVICES);
    const [notificationSettings, setNotificationSettings] = useState<NotificationSetting[]>(
        PLACEHOLDER_NOTIFICATION_SETTINGS
    );
    const [integrations, setIntegrations] = useState<Record<string, Integration>>(PLACEHOLDER_INTEGRATIONS);
    const [backupPoints] = useState<BackupPoint[]>(PLACEHOLDER_BACKUP_POINTS);
    const [auditLogs] = useState<AuditLog[]>(PLACEHOLDER_AUDIT_LOGS);

    // Handlers
    const handleSaveAll = (): void => {
        toast.success(t('admin.systemSettings.toast.settingsSaved'), {
            description: t('admin.systemSettings.toast.settingsSavedDescription'),
        });
    };

    const handleResetToDefault = (): void => {
        toast.success(t('admin.systemSettings.toast.settingsReset'), {
            description: t('admin.systemSettings.toast.settingsResetDescription'),
        });
    };

    const handleSaveSystemPrefs = (): void => {
        toast.success(t('admin.systemSettings.toast.systemPrefsSaved'), {
            description: t('admin.systemSettings.toast.changesApplied'),
        });
    };

    const handleApplyToAll = (): void => {
        toast.success(t('admin.systemSettings.toast.appliedToAll'), {
            description: t('admin.systemSettings.toast.appliedToAllDescription'),
        });
    };

    const handleTestConnection = (service: string): void => {
        toast.success(t('admin.systemSettings.toast.testingConnection', {
            service: t(`admin.systemSettings.integrations.services.${service}`, service),
        }), {
            description: t('admin.systemSettings.toast.connectionTestInitiated'),
        });
    };

    const handleTestNotification = (topic: string): void => {
        toast.success(t('admin.systemSettings.toast.testNotificationSent'), {
            description: t('admin.systemSettings.toast.testNotificationSentDescription', { topic }),
        });
    };

    const handleSignOutDevice = (_deviceId: string): void => {
        toast.success(t('admin.systemSettings.toast.deviceSignedOut'), {
            description: t('admin.systemSettings.toast.deviceSignedOutDescription'),
        });
    };

    const handleSignOutAll = (): void => {
        toast.success(t('admin.systemSettings.toast.allDevicesSignedOut'), {
            description: t('admin.systemSettings.toast.allDevicesSignedOutDescription'),
        });
    };

    const handleManualBackup = (): void => {
        toast.success(t('admin.systemSettings.toast.backupStarted'), {
            description: t('admin.systemSettings.toast.backupStartedDescription'),
        });
    };

    const handleExportConfig = (): void => {
        toast.success(t('admin.systemSettings.toast.configExported'), {
            description: t('admin.systemSettings.toast.configExportedDescription'),
        });
    };

    const handleImportConfig = (): void => {
        toast.success(t('admin.systemSettings.toast.configImported'), {
            description: t('admin.systemSettings.toast.configImportedDescription'),
        });
    };

    const handleCreateRole = (): void => {
        toast.success(t('admin.systemSettings.toast.roleCreated'));
        setAddRoleModalOpen(false);
    };

    const handleConfirmRestore = (): void => {
        toast.success(t('admin.systemSettings.toast.restoreInitiated'));
        setRestoreModalOpen(false);
    };

    const toggleApiKeyVisibility = (service: string): void => {
        setShowApiKey({ ...showApiKey, [service]: !showApiKey[service] });
    };

    const updateSystemPref = (key: keyof SystemPreferences, value: string): void => {
        setSystemPrefs({ ...systemPrefs, [key]: value });
    };

    const updateSecuritySetting = (key: keyof SecuritySettings, value: boolean): void => {
        setSecuritySettings({ ...securitySettings, [key]: value });
    };

    const updatePermission = (key: string, field: keyof Permission, value: boolean): void => {
        const perm = permissions[key];
        if (perm && field !== 'module') {
            setPermissions({
                ...permissions,
                [key]: { ...perm, [field]: value },
            });
        }
    };

    const updateNotificationSetting = (id: string, field: keyof NotificationSetting, value: boolean): void => {
        setNotificationSettings(
            notificationSettings.map((s) =>
                s.id === id && field !== 'id' && field !== 'topic' ? { ...s, [field]: value } : s
            )
        );
    };

    const updateIntegration = (service: string, apiKey: string): void => {
        setIntegrations({
            ...integrations,
            [service]: { ...integrations[service], apiKey },
        });
    };

    const getDeviceIcon = (type: Device['type']) => {
        switch (type) {
            case 'desktop':
                return Monitor;
            case 'mobile':
                return Smartphone;
            case 'tablet':
                return Laptop;
        }
    };

    const getStatusBadge = (status: string): string => {
        const colors: Record<string, string> = {
            connected: 'bg-emerald-100 text-emerald-700',
            disconnected: 'bg-gray-100 text-gray-700',
            error: 'bg-red-100 text-red-700',
            success: 'bg-emerald-100 text-emerald-700',
            failed: 'bg-red-100 text-red-700',
        };
        return colors[status] || 'bg-gray-100 text-gray-700';
    };

    return {
        // State
        activeSection,
        addRoleModalOpen,
        restoreModalOpen,
        showApiKey,
        systemPrefs,
        securitySettings,
        roles,
        permissions,
        devices,
        notificationSettings,
        integrations,
        backupPoints,
        auditLogs,

        // Setters
        setActiveSection,
        setAddRoleModalOpen,
        setRestoreModalOpen,

        // Handlers
        handleSaveAll,
        handleResetToDefault,
        handleSaveSystemPrefs,
        handleApplyToAll,
        handleTestConnection,
        handleTestNotification,
        handleSignOutDevice,
        handleSignOutAll,
        handleManualBackup,
        handleExportConfig,
        handleImportConfig,
        handleCreateRole,
        handleConfirmRestore,
        toggleApiKeyVisibility,
        updateSystemPref,
        updateSecuritySetting,
        updatePermission,
        updateNotificationSetting,
        updateIntegration,

        // Utilities
        getDeviceIcon,
        getStatusBadge,
    };
}
