import type { Activity, Season, SeasonStatus } from "../types";
import { ActivityFeed } from "./ActivityFeed";
import { SeasonHeader } from "./SeasonHeader";
import { SeasonTabs } from "./SeasonTabs";

interface SeasonDetailViewProps {
  season: Season;
  activities: Activity[];
  activeTab: string;
  setActiveTab: (tab: string) => void;
  onBack: () => void;
  onEditSeason: (season: Season) => void;
  onStartSeason: (season: Season) => void;
  onCompleteSeason: (season: Season) => void;
  onCancelSeason: (season: Season) => void;
  onArchiveSeason: (season: Season) => void;
  handleExportCSV: () => void;
  getStatusColor: (status: SeasonStatus) => string;
  getStatusLabel: (status: SeasonStatus) => string;
  formatDateRange: (startDate: string, endDate: string) => string;
}

export function SeasonDetailView({
  season,
  activities,
  activeTab,
  setActiveTab,
  onBack,
  onEditSeason,
  onStartSeason,
  onCompleteSeason,
  onCancelSeason,
  onArchiveSeason,
  handleExportCSV,
  getStatusColor,
  getStatusLabel,
  formatDateRange,
}: SeasonDetailViewProps) {
  return (
    <>
      <SeasonHeader
        viewMode="detail"
        selectedSeason={season}
        onNewSeason={() => {}}
        onExport={handleExportCSV}
        onBack={onBack}
        onEdit={() => onEditSeason(season)}
        onStartSeason={onStartSeason}
        onCompleteSeason={onCompleteSeason}
        onCancelSeason={onCancelSeason}
        onArchiveSeason={onArchiveSeason}
        getStatusColor={getStatusColor}
        getStatusLabel={getStatusLabel}
        formatDateRange={formatDateRange}
      />

      <div className="max-w-[1800px] mx-auto p-6 space-y-6">
        <SeasonTabs
          season={season}
          activeTab={activeTab}
          setActiveTab={setActiveTab}
          activities={activities}
        />
        <ActivityFeed activities={activities} />
      </div>
    </>
  );
}

