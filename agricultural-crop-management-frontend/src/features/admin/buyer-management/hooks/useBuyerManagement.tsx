import {
    AlertCircle,
    Edit,
    FileCheck, Lock,
    Plus,
    Trash2,
} from 'lucide-react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useMemo, useState, type ChangeEvent } from 'react';
// eslint-disable-next-line no-restricted-imports -- Admin user management APIs are currently provided from legacy service module.
import {
    adminBuyerApi,
    adminKeys,
    adminUsersApi,
    type AdminUser,
    type AdminUserCreateRequest,
    type AdminUserStatusUpdate,
    type AdminUserUpdateRequest,
} from '@/services/api.admin';
import { toast } from 'sonner';
import {
    DEFAULT_ITEMS_PER_PAGE,
    KYC_BADGE_COLORS,
    ROLE_BADGE_COLORS,
    STATUS_BADGE_COLORS,
} from '../constants';
import type {
    AccountStatus,
    AuditLog,
    Buyer,
    BuyerFormData,
    BuyerRole,
    BuyerStats,
    KYCStatus,
} from '../types';

const CSV_REQUIRED_COLUMNS = ['companyname', 'taxid', 'contactname', 'email', 'phone', 'role'] as const;
const BUYER_STATUS_MAP: Record<AccountStatus, string> = {
    active: 'ACTIVE',
    suspended: 'LOCKED',
    closed: 'INACTIVE',
};

const API_STATUS_TO_BUYER_STATUS: Record<string, AccountStatus> = {
    ACTIVE: 'active',
    LOCKED: 'suspended',
    INACTIVE: 'closed',
    PENDING: 'active',
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

const safeUsername = (companyName: string, email: string, fallbackId: string) => {
    const emailPrefix = email.split('@')[0] ?? '';
    const seed = normalizeText(emailPrefix || companyName || `buyer${fallbackId}`);
    const base = seed.length >= 3 ? seed : `${seed}${'buyer'.slice(0, 3 - seed.length)}`;
    return base.slice(0, 32);
};

const generateTempPassword = () => `Tmp${Math.random().toString(36).slice(-8)}A1!`;

const mapBuyerRoleToApiRoles = (role: BuyerRole): string[] => {
    switch (role) {
        case 'enterprise':
            return ['BUYER', 'ENTERPRISE'];
        case 'distributor':
            return ['BUYER', 'DISTRIBUTOR'];
        default:
            return ['BUYER'];
    }
};

const mapApiRolesToBuyerRole = (roles: string[] | undefined): BuyerRole => {
    const upperRoles = (roles ?? []).map((role) => role.toUpperCase());
    if (upperRoles.includes('DISTRIBUTOR')) return 'distributor';
    if (upperRoles.includes('ENTERPRISE')) return 'enterprise';
    return 'buyer';
};

const mapAdminUserToBuyer = (user: AdminUser): Buyer => {
    const apiStatus = user.status?.toUpperCase() ?? 'ACTIVE';
    const accountStatus = API_STATUS_TO_BUYER_STATUS[apiStatus] ?? 'active';
    const contactName = user.fullName?.trim() || user.username;
    return {
        id: String(user.id),
        companyName: user.username,
        taxId: '',
        contactName,
        email: user.email ?? '',
        phone: user.phone ?? '',
        role: mapApiRolesToBuyerRole(user.roles),
        kycStatus: 'pending',
        accountStatus,
        createdAt: '-',
        address: '',
        paymentTerms: '',
    };
};

export function useBuyerManagement() {
    const queryClient = useQueryClient();

    // State Management
    const [searchQuery, setSearchQuery] = useState('');
    const [selectedBuyers, setSelectedBuyers] = useState<string[]>([]);
    const [filterOpen, setFilterOpen] = useState(false);
    const [detailDrawerOpen, setDetailDrawerOpen] = useState(false);
    const [resetPasswordOpen, setResetPasswordOpen] = useState(false);
    const [importWizardOpen, setImportWizardOpen] = useState(false);
    const [selectedBuyer, setSelectedBuyer] = useState<Buyer | null>(null);
    const [detailTab, setDetailTab] = useState('general');
    const [currentPage, setCurrentPage] = useState(1);
    const [itemsPerPage, setItemsPerPage] = useState(DEFAULT_ITEMS_PER_PAGE);
    const [sortColumn, setSortColumn] = useState<keyof Buyer>('createdAt');
    const [sortDirection, setSortDirection] = useState<'asc' | 'desc'>('desc');

    // Import wizard state
    const [importStep, setImportStep] = useState(1);
    const [csvFile, setCsvFile] = useState<File | null>(null);
    // CSV Preview types for type-safe imports
    const [csvPreview, setCsvPreview] = useState<Partial<Buyer>[]>([]);
    const [validationErrors, setValidationErrors] = useState<{ row: number; error: string }[]>([]);
    const [resetPasswordTargetId, setResetPasswordTargetId] = useState<string | null>(null);

    // Filter state
    const [roleFilter, setRoleFilter] = useState<BuyerRole | 'all'>('all');
    const [kycFilter, setKycFilter] = useState<KYCStatus | 'all'>('all');
    const [statusFilter, setStatusFilter] = useState<AccountStatus | 'all'>('all');

    // Form state
    const [formData, setFormData] = useState<BuyerFormData>({
        companyName: '',
        taxId: '',
        contactName: '',
        email: '',
        phone: '',
        address: '',
        role: 'buyer',
        accountStatus: 'active',
        paymentTerms: '',
    });

    const buyerListQuery = useQuery({
        queryKey: adminKeys.buyerList({
            keyword: searchQuery.trim() || undefined,
            status: statusFilter === 'all' ? undefined : BUYER_STATUS_MAP[statusFilter],
            page: currentPage - 1,
            size: itemsPerPage,
        }),
        queryFn: () =>
            adminBuyerApi.list({
                keyword: searchQuery.trim() || undefined,
                status: statusFilter === 'all' ? undefined : BUYER_STATUS_MAP[statusFilter],
                page: currentPage - 1,
                size: itemsPerPage,
            }),
        staleTime: 30 * 1000,
    });

    const createBuyerMutation = useMutation({
        mutationFn: (payload: AdminUserCreateRequest) => adminUsersApi.create(payload),
        onSuccess: async () => {
            await queryClient.invalidateQueries({ queryKey: adminKeys.buyers });
            await queryClient.invalidateQueries({ queryKey: adminKeys.users });
        },
    });

    const updateBuyerMutation = useMutation({
        mutationFn: ({ id, payload }: { id: number; payload: AdminUserUpdateRequest }) =>
            adminUsersApi.update(id, payload),
        onSuccess: async () => {
            await queryClient.invalidateQueries({ queryKey: adminKeys.buyers });
            await queryClient.invalidateQueries({ queryKey: adminKeys.users });
        },
    });

    const updateBuyerStatusMutation = useMutation({
        mutationFn: ({ id, payload }: { id: number; payload: AdminUserStatusUpdate }) =>
            adminUsersApi.updateStatus(id, payload),
        onSuccess: async () => {
            await queryClient.invalidateQueries({ queryKey: adminKeys.buyers });
            await queryClient.invalidateQueries({ queryKey: adminKeys.users });
        },
    });

    const deleteBuyerMutation = useMutation({
        mutationFn: (id: number) => adminUsersApi.delete(id),
        onSuccess: async () => {
            await queryClient.invalidateQueries({ queryKey: adminKeys.buyers });
            await queryClient.invalidateQueries({ queryKey: adminKeys.users });
        },
    });

    const resetPasswordMutation = useMutation({
        mutationFn: ({ id, password }: { id: number; password: string }) =>
            adminUsersApi.resetPassword(id, password),
    });

    const buyers = useMemo(
        () => (buyerListQuery.data?.items ?? []).map(mapAdminUserToBuyer),
        [buyerListQuery.data?.items]
    );

    // Filter and sort buyers
    const filteredBuyers = buyers.filter((buyer) => {
        const matchesSearch =
            buyer.companyName.toLowerCase().includes(searchQuery.toLowerCase()) ||
            buyer.taxId.toLowerCase().includes(searchQuery.toLowerCase()) ||
            buyer.contactName.toLowerCase().includes(searchQuery.toLowerCase()) ||
            buyer.email.toLowerCase().includes(searchQuery.toLowerCase());

        const matchesRole = roleFilter === 'all' || buyer.role === roleFilter;
        const matchesKYC = kycFilter === 'all' || buyer.kycStatus === kycFilter;
        const matchesStatus = statusFilter === 'all' || buyer.accountStatus === statusFilter;

        return matchesSearch && matchesRole && matchesKYC && matchesStatus;
    }).sort((a, b) => {
        const aValue = a[sortColumn];
        const bValue = b[sortColumn];
        const direction = sortDirection === 'asc' ? 1 : -1;

        // Safe comparison with type guards
        if (aValue == null && bValue == null) return 0;
        if (aValue == null) return 1;
        if (bValue == null) return -1;

        // Use localeCompare for strings, numeric comparison for others
        if (typeof aValue === 'string' && typeof bValue === 'string') {
            return aValue.localeCompare(bValue) * direction;
        }

        return (aValue > bValue ? 1 : -1) * direction;
    });

    // Pagination
    const totalPages = Math.max(1, buyerListQuery.data?.totalPages ?? 1);
    const totalResults = buyerListQuery.data?.totalElements ?? filteredBuyers.length;
    const paginatedBuyers = filteredBuyers;

    useEffect(() => {
        setCurrentPage(1);
    }, [searchQuery, roleFilter, kycFilter, statusFilter, itemsPerPage]);

    useEffect(() => {
        if (currentPage > totalPages) {
            setCurrentPage(totalPages);
        }
    }, [currentPage, totalPages]);

    // Calculate stats
    const stats: BuyerStats = {
        total: totalResults,
        active: buyers.filter((b) => b.accountStatus === 'active').length,
        pendingKYC: buyers.filter((b) => b.kycStatus === 'pending').length,
        locked: buyers.filter((b) => b.accountStatus === 'suspended' || b.accountStatus === 'closed').length,
    };

    // Handlers
    const handleSort = (column: keyof Buyer) => {
        if (sortColumn === column) {
            setSortDirection(sortDirection === 'asc' ? 'desc' : 'asc');
        } else {
            setSortColumn(column);
            setSortDirection('asc');
        }
    };

    const handleSelectAll = () => {
        if (selectedBuyers.length === paginatedBuyers.length) {
            setSelectedBuyers([]);
        } else {
            setSelectedBuyers(paginatedBuyers.map((b) => b.id));
        }
    };

    const handleSelectBuyer = (id: string) => {
        setSelectedBuyers((prev) =>
            prev.includes(id) ? prev.filter((bid) => bid !== id) : [...prev, id]
        );
    };

    const handleViewEdit = (buyer: Buyer) => {
        setSelectedBuyer(buyer);
        setFormData({
            companyName: buyer.companyName,
            taxId: buyer.taxId,
            contactName: buyer.contactName,
            email: buyer.email,
            phone: buyer.phone,
            address: buyer.address || '',
            role: buyer.role,
            accountStatus: buyer.accountStatus,
            paymentTerms: buyer.paymentTerms || '',
        });
        setDetailTab('general');
        setDetailDrawerOpen(true);
    };

    const handleAddBuyer = () => {
        setSelectedBuyer(null);
        setFormData({
            companyName: '',
            taxId: '',
            contactName: '',
            email: '',
            phone: '',
            address: '',
            role: 'buyer',
            accountStatus: 'active',
            paymentTerms: '',
        });
        setDetailDrawerOpen(true);
    };

    const handleSave = async () => {
        if (
            formData.taxId.trim().length > 0 ||
            formData.address.trim().length > 0 ||
            formData.paymentTerms.trim().length > 0
        ) {
            toast.error('Tax ID, address, and payment terms are not supported by the current backend user API.');
            return;
        }

        const username = safeUsername(
            formData.companyName,
            formData.email,
            selectedBuyer?.id ?? String(Date.now())
        );

        const payloadRoles = mapBuyerRoleToApiRoles(formData.role);
        const payloadStatus = BUYER_STATUS_MAP[formData.accountStatus];

        if (selectedBuyer) {
            try {
                await updateBuyerMutation.mutateAsync({
                    id: Number(selectedBuyer.id),
                    payload: {
                        username,
                        fullName: formData.contactName.trim() || formData.companyName.trim(),
                        email: formData.email.trim() || undefined,
                        phone: formData.phone.trim() || undefined,
                        roles: payloadRoles,
                        status: payloadStatus,
                    },
                });
                toast.success('Buyer account updated successfully.');
            } catch (error) {
                console.error(error);
                toast.error('Failed to update buyer account.');
                return;
            }
        } else {
            try {
                await createBuyerMutation.mutateAsync({
                    username,
                    password: generateTempPassword(),
                    fullName: formData.contactName.trim() || formData.companyName.trim(),
                    email: formData.email.trim() || undefined,
                    phone: formData.phone.trim() || undefined,
                    roles: payloadRoles,
                });
                if (payloadStatus !== 'ACTIVE') {
                    const latest = await adminBuyerApi.list({
                        keyword: username,
                        page: 0,
                        size: 1,
                    });
                    const created = latest.items[0];
                    if (created?.id != null) {
                        await updateBuyerStatusMutation.mutateAsync({
                            id: Number(created.id),
                            payload: { status: payloadStatus },
                        });
                    }
                }
                toast.success('Buyer account created successfully.');
            } catch (error) {
                console.error(error);
                toast.error('Failed to create buyer account.');
                return;
            }
        }
        setDetailDrawerOpen(false);
        setSelectedBuyer(null);
    };

    const handleDelete = async (id: string) => {
        const buyer = buyers.find((b) => b.id === id);
        try {
            await deleteBuyerMutation.mutateAsync(Number(id));
            toast.success('Buyer account deleted.', {
                description: buyer ? `${buyer.companyName} has been removed.` : undefined,
            });
            setSelectedBuyers((prev) => prev.filter((buyerId) => buyerId !== id));
        } catch (error) {
            console.error(error);
            toast.error('Failed to delete buyer account.');
        }
    };

    const handleToggleSuspend = async (id: string) => {
        const buyer = buyers.find((b) => b.id === id);
        if (!buyer) return;

        const nextStatus: AccountStatus = buyer.accountStatus === 'suspended' ? 'active' : 'suspended';
        try {
            await updateBuyerStatusMutation.mutateAsync({
                id: Number(id),
                payload: { status: BUYER_STATUS_MAP[nextStatus] },
            });
            toast.success(nextStatus === 'active' ? 'Buyer account activated.' : 'Buyer account suspended.');
        } catch (error) {
            console.error(error);
            toast.error('Failed to update buyer status.');
        }
    };

    const handleKYCAction = (_action: 'verify' | 'reject') => {
        toast.error('KYC verification API is not available yet.');
    };

    const handleBulkAction = async (action: string) => {
        if (selectedBuyers.length === 0) {
            toast.error('No buyers selected', {
                description: 'Please select at least one buyer to perform bulk action.',
            });
            return;
        }

        try {
            if (action === 'activate' || action === 'suspend') {
                const targetStatus = action === 'activate' ? 'active' : 'suspended';
                await Promise.all(
                    selectedBuyers.map((buyerId) =>
                        updateBuyerStatusMutation.mutateAsync({
                            id: Number(buyerId),
                            payload: { status: BUYER_STATUS_MAP[targetStatus] },
                        })
                    )
                );
                toast.success(
                    action === 'activate'
                        ? `${selectedBuyers.length} buyers activated.`
                        : `${selectedBuyers.length} buyers suspended.`
                );
            }

            if (action === 'delete') {
                await Promise.all(
                    selectedBuyers.map((buyerId) => deleteBuyerMutation.mutateAsync(Number(buyerId)))
                );
                toast.success(`${selectedBuyers.length} buyers deleted.`);
            }
            setSelectedBuyers([]);
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

        const headers = parseCsvLine(rows[0]).map((header) => header.toLowerCase().replace(/\s+/g, ''));
        const hasRequiredColumns = CSV_REQUIRED_COLUMNS.every((column) => headers.includes(column));
        if (!hasRequiredColumns) {
            toast.error('CSV format is invalid. Required columns: companyName, taxId, contactName, email, phone, role.');
            return;
        }

        const previewRows: Partial<Buyer>[] = [];
        const parseErrors: { row: number; error: string }[] = [];

        for (let index = 1; index < rows.length; index += 1) {
            const values = parseCsvLine(rows[index]);
            const rowObject: Record<string, string> = {};
            headers.forEach((header, headerIndex) => {
                rowObject[header] = values[headerIndex]?.trim() ?? '';
            });

            const roleValue = rowObject.role?.toLowerCase();
            if (!roleValue || !['buyer', 'enterprise', 'distributor'].includes(roleValue)) {
                parseErrors.push({ row: index + 1, error: `Invalid role "${rowObject.role}"` });
                continue;
            }

            if (!rowObject.companyname || !rowObject.contactname) {
                parseErrors.push({ row: index + 1, error: 'companyName and contactName are required' });
                continue;
            }

            previewRows.push({
                companyName: rowObject.companyname,
                taxId: rowObject.taxid ?? '',
                contactName: rowObject.contactname,
                email: rowObject.email ?? '',
                phone: rowObject.phone ?? '',
                role: roleValue as BuyerRole,
            });
        }

        setValidationErrors(parseErrors);
        setCsvPreview(previewRows);
        setImportStep(2);
        if (parseErrors.length > 0) {
            toast.warning(`Skipped ${parseErrors.length} invalid row(s).`);
        }
        if (previewRows.length === 0) {
            toast.error('No valid rows found in CSV file.');
        }
    };

    const handleImportConfirm = () => {
        toast.error('Buyer CSV import endpoint is not available yet.');
    };

    const handleResetPassword = (id: string) => {
        setResetPasswordTargetId(id);
        setResetPasswordOpen(true);
    };

    const handleResetPasswordModalOpenChange = (open: boolean) => {
        setResetPasswordOpen(open);
        if (!open) {
            setResetPasswordTargetId(null);
        }
    };

    const handleResetPasswordSubmit = async (method: 'email' | 'temp') => {
        if (!resetPasswordTargetId) return;

        if (method === 'email') {
            toast.error('Email reset flow is not available. Use temporary password reset.');
            return;
        }

        try {
            const temporaryPassword = generateTempPassword();
            await resetPasswordMutation.mutateAsync({
                id: Number(resetPasswordTargetId),
                password: temporaryPassword,
            });
            if (typeof navigator !== 'undefined' && navigator.clipboard?.writeText) {
                await navigator.clipboard.writeText(temporaryPassword);
            }
            toast.success('Temporary password generated and applied.', {
                description: 'Password copied to clipboard.',
            });
            setResetPasswordOpen(false);
            setResetPasswordTargetId(null);
        } catch (error) {
            console.error(error);
            toast.error('Failed to reset password.');
        }
    };

    // Helper functions for badge colors
    const getRoleBadge = (role: BuyerRole) => ROLE_BADGE_COLORS[role];
    const getKYCBadge = (status: KYCStatus) => KYC_BADGE_COLORS[status];
    const getStatusBadge = (status: AccountStatus) => STATUS_BADGE_COLORS[status];

    const getAuditIcon = (type: AuditLog['type']) => {
        switch (type) {
            case 'create':
                return <Plus className="w-4 h-4 text-green-600" />;
            case 'update':
                return <Edit className="w-4 h-4 text-blue-600" />;
            case 'delete':
                return <Trash2 className="w-4 h-4 text-red-600" />;
            case 'kyc':
                return <FileCheck className="w-4 h-4 text-purple-600" />;
            case 'lock':
                return <Lock className="w-4 h-4 text-orange-600" />;
            default:
                return <AlertCircle className="w-4 h-4 text-gray-600" />;
        }
    };

    // Return all necessary values and functions
    return {
        // State
        searchQuery,
        setSearchQuery,
        selectedBuyers,
        filterOpen,
        setFilterOpen,
        detailDrawerOpen,
        setDetailDrawerOpen,
        resetPasswordOpen,
        setResetPasswordOpen,
        importWizardOpen,
        setImportWizardOpen,
        selectedBuyer,
        detailTab,
        setDetailTab,
        currentPage,
        setCurrentPage,
        itemsPerPage,
        setItemsPerPage,
        sortColumn,
        sortDirection,
        importStep,
        setImportStep,
        csvFile,
        csvPreview,
        validationErrors,
        roleFilter,
        setRoleFilter,
        kycFilter,
        setKycFilter,
        statusFilter,
        setStatusFilter,
        formData,
        setFormData,

        // Data
        buyers,
        filteredBuyers,
        paginatedBuyers,
        totalPages,
        stats,

        // Handlers
        handleSort,
        handleSelectAll,
        handleSelectBuyer,
        handleViewEdit,
        handleAddBuyer,
        handleSave,
        handleDelete,
        handleToggleSuspend,
        handleKYCAction,
        handleBulkAction,
        handleCSVUpload,
        handleImportConfirm,
        handleResetPassword,
        handleResetPasswordSubmit,
        handleResetPasswordModalOpenChange,

        // Helper functions
        getRoleBadge,
        getKYCBadge,
        getStatusBadge,
        getAuditIcon,

        // API status
        isLoading: buyerListQuery.isLoading,
        isFetching: buyerListQuery.isFetching,
        error: buyerListQuery.error instanceof Error ? buyerListQuery.error.message : null,
        totalResults,
    };
}

