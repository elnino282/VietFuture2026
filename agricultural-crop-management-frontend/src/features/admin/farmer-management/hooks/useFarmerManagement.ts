import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useMemo, useState, type ChangeEvent } from 'react';
// eslint-disable-next-line no-restricted-imports -- Admin user management APIs are currently provided from legacy service module.
import {
    adminFarmerApi,
    adminKeys,
    adminUsersApi,
    type AdminUser,
    type AdminUserCreateRequest,
    type AdminUserStatusUpdate,
    type AdminUserUpdateRequest,
} from '@/services/api.admin';
import { toast } from 'sonner';
import type {
    CSVPreviewRow,
    Farmer,
    FarmerFormData,
    FarmerRole,
    FarmerStatus,
    ValidationError,
} from '../types';

const CSV_REQUIRED_COLUMNS = ['name', 'email', 'phone', 'role', 'status'] as const;
const FARMER_STATUS_MAP: Record<FarmerStatus, string> = {
    active: 'ACTIVE',
    locked: 'LOCKED',
    inactive: 'INACTIVE',
};

const API_STATUS_TO_FARMER_STATUS: Record<string, FarmerStatus> = {
    ACTIVE: 'active',
    LOCKED: 'locked',
    INACTIVE: 'inactive',
    PENDING: 'inactive',
};

const parseCsvLine = (line: string): string[] => {
    const values: string[] = [];
    let current = '';
    let inQuotes = false;

    for (let i = 0; i < line.length; i += 1) {
        const char = line[i];
        if (char === '"') {
            if (inQuotes && line[i + 1] === '"') {
                current += '"';
                i += 1;
            } else {
                inQuotes = !inQuotes;
            }
            continue;
        }
        if (char === ',' && !inQuotes) {
            values.push(current.trim());
            current = '';
            continue;
        }
        current += char;
    }

    values.push(current.trim());
    return values;
};

const normalizeText = (value: string) =>
    value
        .trim()
        .toLowerCase()
        .replace(/\s+/g, '')
        .replace(/[^a-z0-9_-]/g, '');

const safeUsername = (name: string, email: string, fallbackId: string) => {
    const emailPrefix = email.split('@')[0] ?? '';
    const seed = normalizeText(emailPrefix || name || `farmer${fallbackId}`);
    const base = seed.length >= 3 ? seed : `${seed}${'farmer'.slice(0, 3 - seed.length)}`;
    return base.slice(0, 32);
};

const generateTempPassword = () => `Tmp${Math.random().toString(36).slice(-8)}A1!`;

const mapFarmerRoleToApiRoles = (role: FarmerRole): string[] => {
    switch (role) {
        case 'owner':
            return ['OWNER'];
        case 'manager':
            return ['MANAGER'];
        default:
            return ['FARMER'];
    }
};

const mapApiRolesToFarmerRole = (roles: string[] | undefined): FarmerRole => {
    const normalizedRoles = (roles ?? []).map((role) => role.toUpperCase());
    if (normalizedRoles.includes('OWNER')) return 'owner';
    if (normalizedRoles.includes('MANAGER')) return 'manager';
    return 'farmer';
};

const mapAdminUserToFarmer = (user: AdminUser): Farmer => {
    const apiStatus = user.status?.toUpperCase() ?? 'ACTIVE';
    return {
        id: String(user.id),
        name: user.fullName?.trim() || user.username,
        email: user.email ?? '',
        phone: user.phone ?? '',
        role: mapApiRolesToFarmerRole(user.roles),
        status: API_STATUS_TO_FARMER_STATUS[apiStatus] ?? 'active',
        lastLogin: 'N/A',
        createdAt: '-',
        plotsCount: undefined,
    };
};

export function useFarmerManagement() {
    const queryClient = useQueryClient();

    // State Management
    const [searchQuery, setSearchQuery] = useState('');
    const [selectedFarmers, setSelectedFarmers] = useState<string[]>([]);
    const [filterOpen, setFilterOpen] = useState(false);
    const [createEditOpen, setCreateEditOpen] = useState(false);
    const [resetPasswordOpen, setResetPasswordOpen] = useState(false);
    const [historyOpen, setHistoryOpen] = useState(false);
    const [importWizardOpen, setImportWizardOpen] = useState(false);
    const [editingFarmer, setEditingFarmer] = useState<Farmer | null>(null);
    const [currentPage, setCurrentPage] = useState(1);
    const [itemsPerPage, setItemsPerPage] = useState(10);
    const [sortColumn, setSortColumn] = useState<keyof Farmer>('createdAt');
    const [sortDirection, setSortDirection] = useState<'asc' | 'desc'>('desc');

    // Import wizard state
    const [importStep, setImportStep] = useState(1);
    const [csvFile, setCsvFile] = useState<File | null>(null);
    const [csvPreview, setCsvPreview] = useState<CSVPreviewRow[]>([]);
    const [validationErrors, setValidationErrors] = useState<ValidationError[]>([]);

    // Filter state
    const [roleFilter, setRoleFilter] = useState<FarmerRole | 'all'>('all');
    const [statusFilter, setStatusFilter] = useState<FarmerStatus | 'all'>('all');

    // Form state
    const [formData, setFormData] = useState<FarmerFormData>({
        name: '',
        email: '',
        phone: '',
        role: 'farmer',
        status: 'active',
        tempPassword: '',
        sendEmail: true,
    });

    const farmerListQuery = useQuery({
        queryKey: adminKeys.farmerList({
            keyword: searchQuery.trim() || undefined,
            status: statusFilter === 'all' ? undefined : FARMER_STATUS_MAP[statusFilter],
            page: currentPage - 1,
            size: itemsPerPage,
        }),
        queryFn: () =>
            adminFarmerApi.list({
                keyword: searchQuery.trim() || undefined,
                status: statusFilter === 'all' ? undefined : FARMER_STATUS_MAP[statusFilter],
                page: currentPage - 1,
                size: itemsPerPage,
            }),
        staleTime: 30 * 1000,
    });

    const createFarmerMutation = useMutation({
        mutationFn: (payload: AdminUserCreateRequest) => adminUsersApi.create(payload),
        onSuccess: async () => {
            await queryClient.invalidateQueries({ queryKey: adminKeys.farmers });
            await queryClient.invalidateQueries({ queryKey: adminKeys.users });
        },
    });

    const updateFarmerMutation = useMutation({
        mutationFn: ({ id, payload }: { id: number; payload: AdminUserUpdateRequest }) =>
            adminUsersApi.update(id, payload),
        onSuccess: async () => {
            await queryClient.invalidateQueries({ queryKey: adminKeys.farmers });
            await queryClient.invalidateQueries({ queryKey: adminKeys.users });
        },
    });

    const updateFarmerStatusMutation = useMutation({
        mutationFn: ({ id, payload }: { id: number; payload: AdminUserStatusUpdate }) =>
            adminUsersApi.updateStatus(id, payload),
        onSuccess: async () => {
            await queryClient.invalidateQueries({ queryKey: adminKeys.farmers });
            await queryClient.invalidateQueries({ queryKey: adminKeys.users });
        },
    });

    const deleteFarmerMutation = useMutation({
        mutationFn: (id: number) => adminUsersApi.delete(id),
        onSuccess: async () => {
            await queryClient.invalidateQueries({ queryKey: adminKeys.farmers });
            await queryClient.invalidateQueries({ queryKey: adminKeys.users });
        },
    });

    const resetPasswordMutation = useMutation({
        mutationFn: ({ id, password }: { id: number; password: string }) =>
            adminUsersApi.resetPassword(id, password),
    });

    const farmers = useMemo(
        () => (farmerListQuery.data?.items ?? []).map(mapAdminUserToFarmer),
        [farmerListQuery.data?.items]
    );

    // Handlers
    const handleSort = (column: keyof Farmer) => {
        if (sortColumn === column) {
            setSortDirection(sortDirection === 'asc' ? 'desc' : 'asc');
        } else {
            setSortColumn(column);
            setSortDirection('asc');
        }
    };

    const handleSelectAll = (filteredFarmers: Farmer[]) => {
        if (selectedFarmers.length === filteredFarmers.length) {
            setSelectedFarmers([]);
        } else {
            setSelectedFarmers(filteredFarmers.map(f => f.id));
        }
    };

    const handleSelectFarmer = (id: string) => {
        setSelectedFarmers(prev =>
            prev.includes(id) ? prev.filter(fid => fid !== id) : [...prev, id]
        );
    };

    const handleEdit = (farmer: Farmer) => {
        setEditingFarmer(farmer);
        setFormData({
            name: farmer.name,
            email: farmer.email,
            phone: farmer.phone,
            role: farmer.role,
            status: farmer.status,
            tempPassword: '',
            sendEmail: true,
        });
        setCreateEditOpen(true);
    };

    const handleCreate = () => {
        setEditingFarmer(null);
        setFormData({
            name: '',
            email: '',
            phone: '',
            role: 'farmer',
            status: 'active',
            tempPassword: '',
            sendEmail: true,
        });
        setCreateEditOpen(true);
    };

    const handleSave = () => {
        void (async () => {
            const username = safeUsername(formData.name, formData.email, editingFarmer?.id ?? String(Date.now()));
            const payloadRoles = mapFarmerRoleToApiRoles(formData.role);
            const payloadStatus = FARMER_STATUS_MAP[formData.status];

            if (editingFarmer) {
                try {
                    await updateFarmerMutation.mutateAsync({
                        id: Number(editingFarmer.id),
                        payload: {
                            username,
                            fullName: formData.name.trim(),
                            email: formData.email.trim() || undefined,
                            phone: formData.phone.trim() || undefined,
                            roles: payloadRoles,
                            status: payloadStatus,
                        },
                    });
                    toast.success('Farmer account updated successfully.');
                } catch (error) {
                    console.error(error);
                    toast.error('Failed to update farmer account.');
                    return;
                }
            } else {
                const temporaryPassword = formData.tempPassword.trim() || generateTempPassword();
                try {
                    const createdUser = await createFarmerMutation.mutateAsync({
                        username,
                        password: temporaryPassword,
                        fullName: formData.name.trim(),
                        email: formData.email.trim() || undefined,
                        phone: formData.phone.trim() || undefined,
                        roles: payloadRoles,
                    });
                    if (payloadStatus !== 'ACTIVE') {
                        await updateFarmerStatusMutation.mutateAsync({
                            id: Number(createdUser.id),
                            payload: { status: payloadStatus },
                        });
                    }

                    if (formData.sendEmail) {
                        toast.warning('Welcome email flow is not available in backend. Credentials were created only.');
                    }
                    toast.success('Farmer account created successfully.');
                } catch (error) {
                    console.error(error);
                    toast.error('Failed to create farmer account.');
                    return;
                }
            }
            setCreateEditOpen(false);
            setEditingFarmer(null);
        })();
    };

    const handleDelete = async (id: string) => {
        const farmer = farmers.find((item) => item.id === id);
        try {
            await deleteFarmerMutation.mutateAsync(Number(id));
            toast.success('Farmer account deleted.', {
                description: farmer ? `${farmer.name} has been removed.` : undefined,
            });
            setSelectedFarmers((prev) => prev.filter((farmerId) => farmerId !== id));
        } catch (error) {
            console.error(error);
            toast.error('Failed to delete farmer account.');
        }
    };

    const handleLock = async (id: string) => {
        const farmer = farmers.find((item) => item.id === id);
        if (!farmer) return;

        const nextStatus: FarmerStatus = farmer.status === 'locked' ? 'active' : 'locked';
        try {
            await updateFarmerStatusMutation.mutateAsync({
                id: Number(id),
                payload: { status: FARMER_STATUS_MAP[nextStatus] },
            });
            toast.success(nextStatus === 'active' ? 'Account unlocked.' : 'Account locked.');
        } catch (error) {
            console.error(error);
            toast.error('Failed to update farmer status.');
        }
    };

    const handleBulkAction = async (action: string) => {
        if (selectedFarmers.length === 0) {
            toast.error('No farmers selected', {
                description: 'Please select at least one farmer to perform bulk action.',
            });
            return;
        }

        try {
            if (action === 'activate' || action === 'lock') {
                const status = action === 'activate' ? 'active' : 'locked';
                await Promise.all(
                    selectedFarmers.map((farmerId) =>
                        updateFarmerStatusMutation.mutateAsync({
                            id: Number(farmerId),
                            payload: { status: FARMER_STATUS_MAP[status] },
                        })
                    )
                );
                toast.success(
                    action === 'activate'
                        ? `${selectedFarmers.length} farmers activated.`
                        : `${selectedFarmers.length} farmers locked.`
                );
            }

            if (action === 'delete') {
                await Promise.all(
                    selectedFarmers.map((farmerId) => deleteFarmerMutation.mutateAsync(Number(farmerId)))
                );
                toast.success(`${selectedFarmers.length} farmers deleted.`);
            }
            setSelectedFarmers([]);
        } catch (error) {
            console.error(error);
            toast.error('Bulk action failed. Please retry.');
        }
    };

    const handleCSVUpload = async (e: ChangeEvent<HTMLInputElement>) => {
        const file = e.target.files?.[0];
        if (!file) return;

        setCsvFile(file);
        const content = await file.text();
        const rows = content
            .split(/\r?\n/)
            .map((row) => row.trim())
            .filter((row) => row.length > 0);

        if (rows.length < 2) {
            toast.error('CSV file does not contain data rows.');
            return;
        }

        const headers = parseCsvLine(rows[0]).map((header) => header.toLowerCase().trim());
        const hasRequiredColumns = CSV_REQUIRED_COLUMNS.every((column) => headers.includes(column));
        if (!hasRequiredColumns) {
            toast.error('CSV format is invalid. Required columns: name, email, phone, role, status.');
            return;
        }

        const previewRows: CSVPreviewRow[] = [];
        const errors: ValidationError[] = [];

        for (let index = 1; index < rows.length; index += 1) {
            const values = parseCsvLine(rows[index]);
            const row: Record<string, string> = {};
            headers.forEach((header, headerIndex) => {
                row[header] = values[headerIndex]?.trim() ?? '';
            });

            const normalizedRole = row.role?.toLowerCase();
            const normalizedStatus = row.status?.toLowerCase();

            if (!row.name) {
                errors.push({ row: index + 1, field: 'name', message: 'Name is required.' });
            }

            if (!row.email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(row.email)) {
                errors.push({ row: index + 1, field: 'email', message: 'Invalid email format.' });
            }

            if (!row.phone) {
                errors.push({ row: index + 1, field: 'phone', message: 'Phone number is required.' });
            }

            if (!['farmer', 'manager', 'owner'].includes(normalizedRole)) {
                errors.push({
                    row: index + 1,
                    field: 'role',
                    message: `Unsupported role "${row.role}".`,
                });
            }

            if (!['active', 'inactive', 'locked'].includes(normalizedStatus)) {
                errors.push({
                    row: index + 1,
                    field: 'status',
                    message: `Unsupported status "${row.status}".`,
                });
            }

            previewRows.push({
                name: row.name ?? '',
                email: row.email ?? '',
                phone: row.phone ?? '',
                role: (normalizedRole as FarmerRole) || 'farmer',
                status: (normalizedStatus as FarmerStatus) || 'active',
            });
        }

        setCsvPreview(previewRows);
        setValidationErrors(errors);
        setImportStep(2);

        if (errors.length > 0) {
            toast.warning(`${errors.length} validation issue(s) found in CSV preview.`);
        }
    };

    const handleImportConfirm = () => {
        toast.error('Farmer CSV import endpoint is not available yet.');
    };

    const handleResetPassword = (id: string, method: 'email' | 'temp') => {
        void (async () => {
            if (method === 'email') {
                toast.error('Email reset flow is not available. Use temporary password reset.');
                return;
            }

            try {
                const temporaryPassword = generateTempPassword();
                await resetPasswordMutation.mutateAsync({
                    id: Number(id),
                    password: temporaryPassword,
                });
                if (typeof navigator !== 'undefined' && navigator.clipboard?.writeText) {
                    await navigator.clipboard.writeText(temporaryPassword);
                }
                toast.success('Temporary password generated and applied.', {
                    description: 'Password copied to clipboard.',
                });
                setResetPasswordOpen(false);
            } catch (error) {
                console.error(error);
                toast.error('Failed to reset password.');
            }
        })();
    };

    const clearFilters = () => {
        setRoleFilter('all');
        setStatusFilter('all');
    };

    useEffect(() => {
        setCurrentPage(1);
    }, [searchQuery, roleFilter, statusFilter, itemsPerPage]);

    // Filter and sort farmers
    const filteredFarmers = farmers.filter(farmer => {
        const matchesSearch =
            farmer.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
            farmer.email.toLowerCase().includes(searchQuery.toLowerCase()) ||
            farmer.phone.includes(searchQuery);

        const matchesRole = roleFilter === 'all' || farmer.role === roleFilter;
        const matchesStatus = statusFilter === 'all' || farmer.status === statusFilter;

        return matchesSearch && matchesRole && matchesStatus;
    }).sort((a, b) => {
        const aValue = a[sortColumn];
        const bValue = b[sortColumn];
        const direction = sortDirection === 'asc' ? 1 : -1;
        if (aValue == null || bValue == null) return 0;
        return aValue > bValue ? direction : -direction;
    });

    const totalPages = Math.max(1, farmerListQuery.data?.totalPages ?? 1);
    const totalElements = farmerListQuery.data?.totalElements ?? filteredFarmers.length;
    const paginatedFarmers = filteredFarmers;

    useEffect(() => {
        if (currentPage > totalPages) {
            setCurrentPage(totalPages);
        }
    }, [currentPage, totalPages]);

    // Return all state and handlers needed by components
    return {
        // State
        farmers: paginatedFarmers,
        filteredFarmers,
        totalFarmers: totalElements,
        searchQuery,
        selectedFarmers,
        filterOpen,
        createEditOpen,
        resetPasswordOpen,
        historyOpen,
        importWizardOpen,
        editingFarmer,
        currentPage,
        itemsPerPage,
        sortColumn,
        sortDirection,
        roleFilter,
        statusFilter,
        formData,
        importStep,
        csvFile,
        csvPreview,
        validationErrors,
        totalPages,

        // Setters
        setSearchQuery,
        setFilterOpen,
        setCreateEditOpen,
        setResetPasswordOpen,
        setHistoryOpen,
        setImportWizardOpen,
        setCurrentPage,
        setItemsPerPage,
        setRoleFilter,
        setStatusFilter,
        setFormData,
        setImportStep,

        // Handlers
        handleSort,
        handleSelectAll: () => handleSelectAll(filteredFarmers),
        handleSelectFarmer,
        handleEdit,
        handleCreate,
        handleSave,
        handleDelete,
        handleLock,
        handleBulkAction,
        handleCSVUpload,
        handleImportConfirm,
        handleResetPassword,
        clearFilters,

        // API status
        isLoading: farmerListQuery.isLoading,
        isFetching: farmerListQuery.isFetching,
        error: farmerListQuery.error instanceof Error ? farmerListQuery.error.message : null,
    };
}
