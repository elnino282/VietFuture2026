/**
 * Farms List Page
 * 
 * Main orchestration component for farm management
 */

import type { FarmDetailResponse } from '@/entities/farm';
import { Button } from '@/shared/ui';
import { AlertCircle } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useFarmManagement } from '../hooks/useFarmManagement';
import { EmptyState } from '../ui/EmptyState';
import { FarmDeleteDialog } from '../ui/FarmDeleteDialog';
import { FarmFormDialog } from '../ui/FarmFormDialog';
import { FarmsListView } from '../ui/FarmsListView';
import { FarmToolbar } from '../ui/FarmToolbar';
import { LoadingState } from '../ui/LoadingState';

/**
 * FarmsListPage Component
 * 
 * Complete farm management interface with:
 * - Search and filtering
 * - Sortable table view
 * - Bulk selection and actions
 * - Create, edit, and delete operations
 * - Loading and error states
 */
export function FarmsListPage() {
    const navigate = useNavigate();
    const {
        // Data
        farms,
        filteredFarms,
        
        // Search and filter state
        searchQuery,
        setSearchQuery,
        activeFilter,
        setActiveFilter,
        
        // Loading & Error states
        isLoading,
        isError,
        error,
        refetch,
        
        // Selection state
        selectedFarms,
        handleToggleSelection,
        handleToggleAllSelection,
        handleClearSelection,
        
        // Dialog states
        showCreateDialog,
        setShowCreateDialog,
        editingFarmId,
        setEditingFarmId,
        
        // Delete state
        isConfirmOpen,
        farmToDelete,
        handleDeleteRequest,
        handleDeleteConfirm,
        handleDeleteCancel,
        isDeleting,
        
        // Handlers
        handleClearFilters,
        handleViewFarm,
        handleEditFarm,
        handleBulkDelete,
        handleBulkStatusChange,
        
        // Computed
        hasActiveFilters,
    } = useFarmManagement();

    const handleCreatedFarm = (farm: FarmDetailResponse) => {
        navigate(`/farmer/farms/${farm.id}`, { state: { openCreatePlot: true } });
    };
    
    // Error state
    if (isError) {
        return (
            <div className="min-h-screen acm-main-content pb-20 flex items-center justify-center">
                <div className="flex flex-col items-center gap-4 max-w-md text-center">
                    <AlertCircle className="w-16 h-16 text-red-500 mb-4" />
                    <h2 className="text-2xl font-bold text-foreground mb-2">
                        Failed to Load Farms
                    </h2>
                    <p className="text-muted-foreground mb-6">
                        {error?.message || 'An error occurred while loading your farms.'}
                    </p>
                    <Button onClick={refetch}>
                        Try Again
                    </Button>
                </div>
            </div>
        );
    }
    
    // Loading state
    if (isLoading) {
        return (
            <div className="min-h-screen acm-main-content pb-20 flex items-center justify-center">
                <LoadingState />
            </div>
        );
    }
    
    // Empty state (no farms at all)
    if (farms.length === 0 && !hasActiveFilters) {
        return (
            <div className="min-h-screen acm-main-content pb-20">
                <FarmToolbar
                    searchQuery={searchQuery}
                    setSearchQuery={setSearchQuery}
                    activeFilter={activeFilter}
                    setActiveFilter={setActiveFilter}
                    onCreateFarm={() => setShowCreateDialog(true)}
                    filteredCount={0}
                    totalCount={0}
                    selectedCount={0}
                    onClearFilters={handleClearFilters}
                />
                <div className="max-w-[1800px] mx-auto px-6">
                    <EmptyState onCreateFarm={() => setShowCreateDialog(true)} />
                </div>
                
                {/* Create Dialog */}
                <FarmFormDialog
                    open={showCreateDialog}
                    onOpenChange={setShowCreateDialog}
                    mode="create"
                    onCreated={handleCreatedFarm}
                />
            </div>
        );
    }
    
    // Get the farm being edited
    const editingFarm = editingFarmId
        ? farms.find(f => f.id === editingFarmId)
        : undefined;
    
    return (
        <div className="min-h-screen acm-main-content pb-20">
            {/* Page Header with Create Button */}
            <FarmToolbar
                searchQuery={searchQuery}
                setSearchQuery={setSearchQuery}
                activeFilter={activeFilter}
                setActiveFilter={setActiveFilter}
                onCreateFarm={() => setShowCreateDialog(true)}
                filteredCount={filteredFarms.length}
                totalCount={farms.length}
                selectedCount={selectedFarms.length}
                onClearFilters={handleClearFilters}
            />
            
            {/* Content */}
            <div className="max-w-[1800px] mx-auto px-6 space-y-6">
            {filteredFarms.length === 0 && hasActiveFilters ? (
                <div className="bg-card rounded-xl border border-border shadow-sm p-12">
                    <div className="flex flex-col items-center text-center">
                        <div className="text-4xl mb-4">🔍</div>
                        <h3 className="text-lg font-semibold text-foreground mb-2">
                            No farms found
                        </h3>
                        <p className="text-muted-foreground mb-4">
                            Try adjusting your search or filters.
                        </p>
                        <Button
                            variant="outline"
                            onClick={handleClearFilters}
                        >
                            Clear filters
                        </Button>
                    </div>
                </div>
            ) : (
                <FarmsListView
                    farms={filteredFarms}
                    selectedFarms={selectedFarms}
                    onToggleSelection={handleToggleSelection}
                    onToggleAllSelection={handleToggleAllSelection}
                    onView={handleViewFarm}
                    onEdit={handleEditFarm}
                    onDelete={handleDeleteRequest}
                    onBulkDelete={handleBulkDelete}
                    onBulkStatusChange={handleBulkStatusChange}
                    onClearSelection={handleClearSelection}
                />
            )}
            </div>
            
            {/* Create Dialog */}
            <FarmFormDialog
                open={showCreateDialog}
                onOpenChange={setShowCreateDialog}
                mode="create"
                onCreated={handleCreatedFarm}
            />
            
            {/* Edit Dialog */}
            {editingFarmId && editingFarm && (
                <FarmFormDialog
                    open={true}
                    onOpenChange={(open) => !open && setEditingFarmId(null)}
                    mode="edit"
                    farm={editingFarm}
                    farmId={editingFarmId}
                />
            )}
            
            {/* Delete Confirmation Dialog */}
            {farmToDelete && (
                <FarmDeleteDialog
                    open={isConfirmOpen}
                    onOpenChange={(open) => !open && handleDeleteCancel()}
                    farmId={farmToDelete.id}
                    farmName={farmToDelete.name}
                    onDeleteSuccess={handleDeleteConfirm}
                />
            )}
        </div>
    );
}




