import { useEffect, useMemo } from 'react';
import { useSearchParams } from 'react-router-dom';
import { DndProvider } from 'react-dnd';
import { HTML5Backend } from 'react-dnd-html5-backend';
import { useTaskWorkspace } from './hooks/useTaskWorkspace';
import { TaskHeader } from './components/TaskHeader';
import { SearchFilterBar } from './components/SearchFilterBar';
import { BoardView } from './components/BoardView';
import { ListView } from './components/ListView';
import { CalendarView } from './components/CalendarView';
import { CreateTaskDialog } from './components/CreateTaskDialog';
import { ReassignDialog } from './components/ReassignDialog';
import { BulkActionToolbar } from './components/BulkActionToolbar';
import { DueDateDialog } from './components/DueDateDialog';
import { TaskProgressReportsPanel } from './components/TaskProgressReportsPanel';
import { PageContainer } from '@/shared/ui';

export function TaskWorkspace() {
  const [searchParams] = useSearchParams();
  const {
    viewMode,
    setViewMode,
    calendarMode,
    setCalendarMode,
    currentDate,
    setCurrentDate,
    searchQuery,
    setSearchQuery,
    filters,
    setFilters,
    selectedTasks,
    setSelectedTasks,
    createTaskOpen,
    setCreateTaskOpen,
    reassignOpen,
    setReassignOpen,
    dueDateOpen,
    setDueDateOpen,
    filteredTasks,
    uniqueAssignees,
    uniquePlots,
    assigneeOptions,
    handleTaskMove,
    handleBulkComplete,
    handleDeleteTask,
    handleSelectAll,
    handleSelectTask,
    handleReassign,
    handleBulkDueDateChange,
    handleCreateTask,
    seasonId,
    isSeasonWriteLocked,
    seasonWriteLockReason,
  } = useTaskWorkspace();

  const qParam = searchParams.get('q') ?? '';
  const seasonFilter = seasonId > 0 ? seasonId : null;

  useEffect(() => {
    if (qParam !== searchQuery) {
      setSearchQuery(qParam);
    }
  }, [qParam, searchQuery, setSearchQuery]);

  const scopedTasks = useMemo(() => {
    if (!seasonFilter) return filteredTasks;
    return filteredTasks.filter((task) => task.seasonId === seasonFilter);
  }, [filteredTasks, seasonFilter]);

  return (
    <DndProvider backend={HTML5Backend}>
      <PageContainer variant="wide" className="pb-20">
        <div className="space-y-6">
          <TaskHeader
            viewMode={viewMode}
            onViewModeChange={setViewMode}
            onCreateTask={() => setCreateTaskOpen(true)}
            disableMutations={isSeasonWriteLocked}
            lockMessage={seasonWriteLockReason}
          />

          <SearchFilterBar
            searchQuery={searchQuery}
            onSearchChange={setSearchQuery}
            filters={filters}
            onFiltersChange={setFilters}
            uniqueAssignees={uniqueAssignees}
            uniquePlots={uniquePlots}
          />

          {viewMode === 'board' && (
            <BoardView
              tasks={scopedTasks}
              onTaskMove={handleTaskMove}
              onDelete={handleDeleteTask}
              disableMutations={isSeasonWriteLocked}
            />
          )}
          {viewMode === 'list' && (
            <ListView
              tasks={scopedTasks}
              selectedTasks={selectedTasks}
              onSelectAll={handleSelectAll}
              onSelectTask={handleSelectTask}
              onDelete={handleDeleteTask}
              disableMutations={isSeasonWriteLocked}
            />
          )}
          {viewMode === 'calendar' && (
            <CalendarView
              tasks={scopedTasks}
              mode={calendarMode}
              currentDate={currentDate}
              onModeChange={setCalendarMode}
              onDateChange={setCurrentDate}
            />
          )}
          {seasonFilter && <TaskProgressReportsPanel seasonId={seasonFilter} />}
        </div>

        {selectedTasks.length > 0 && !isSeasonWriteLocked && (
          <BulkActionToolbar
            selectedCount={selectedTasks.length}
            onComplete={handleBulkComplete}
            onReassign={() => setReassignOpen(true)}
            onChangeDueDate={() => setDueDateOpen(true)}
            onClose={() => setSelectedTasks([])}
          />
        )}

        <CreateTaskDialog
          open={createTaskOpen}
          onOpenChange={setCreateTaskOpen}
          onCreateTask={handleCreateTask}
          seasonId={seasonFilter ?? undefined}
          hideSeasonSelector={true}
          uniquePlots={uniquePlots}
          assigneeOptions={assigneeOptions}
          isFormDisabled={isSeasonWriteLocked}
          disabledReason={seasonWriteLockReason}
        />

        <ReassignDialog
          open={reassignOpen}
          onOpenChange={setReassignOpen}
          selectedCount={selectedTasks.length}
          onReassign={handleReassign}
          assigneeOptions={assigneeOptions}
          disabled={isSeasonWriteLocked}
          disabledReason={seasonWriteLockReason}
        />

        <DueDateDialog
          open={dueDateOpen}
          onOpenChange={setDueDateOpen}
          selectedCount={selectedTasks.length}
          onChangeDueDate={handleBulkDueDateChange}
          disabled={isSeasonWriteLocked}
          disabledReason={seasonWriteLockReason}
        />
      </PageContainer>
    </DndProvider>
  );
}




