import { Search, X } from 'lucide-react';
import { Button } from '@/shared/ui/button';
import { Card, CardContent } from '@/shared/ui/card';
import { Input } from '@/shared/ui/input';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/shared/ui/select';
import { useI18n } from '@/shared/lib/hooks/useI18n';
import { FilterState } from '../types';

interface SeasonFiltersProps {
  filters: FilterState;
  setFilters: (filters: FilterState) => void;
  searchQuery: string;
  setSearchQuery: (query: string) => void;
  uniqueCrops: string[];
  uniqueYears: string[];
}

export function SeasonFilters({
  filters,
  setFilters,
  searchQuery,
  setSearchQuery,
  uniqueCrops,
  uniqueYears,
}: SeasonFiltersProps) {
  const { t } = useI18n();

  return (
    <Card variant="filter">
      <CardContent className="px-6 py-4">
        <div className="flex flex-wrap items-center justify-start gap-4">
          {/* Crop Filter */}
          <Select value={filters.crop} onValueChange={(value) => setFilters({ ...filters, crop: value })}>
            <SelectTrigger className="w-[180px] border-border acm-rounded-sm h-9 text-sm">
              <SelectValue placeholder={t("seasonFilters.allCrops")} />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">{t("seasonFilters.allCrops")}</SelectItem>
              {uniqueCrops.map((crop) => (
                <SelectItem key={crop} value={crop}>
                  {crop}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>

          {/* Status Filter */}
          <Select
            value={filters.status}
            onValueChange={(value) =>
              setFilters({ ...filters, status: value as FilterState['status'] })
            }
          >
            <SelectTrigger className="w-[180px] border-border acm-rounded-sm h-9 text-sm">
              <SelectValue placeholder={t("seasonFilters.allStatus")} />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">{t("seasonFilters.allStatus")}</SelectItem>
              <SelectItem value="PLANNED">{t("seasonFilters.planned")}</SelectItem>
              <SelectItem value="ACTIVE">{t("seasonFilters.active")}</SelectItem>
              <SelectItem value="COMPLETED">{t("seasonFilters.completed")}</SelectItem>
              <SelectItem value="CANCELLED">{t("seasonFilters.cancelled")}</SelectItem>
              <SelectItem value="ARCHIVED">{t("seasonFilters.archived")}</SelectItem>
            </SelectContent>
          </Select>

          {/* Year Filter */}
          <Select value={filters.year} onValueChange={(value) => setFilters({ ...filters, year: value })}>
            <SelectTrigger className="w-[180px] border-border acm-rounded-sm h-9 text-sm">
              <SelectValue placeholder={t("seasonFilters.allYears")} />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">{t("seasonFilters.allYears")}</SelectItem>
              {uniqueYears.map((year) => (
                <SelectItem key={year} value={year}>
                  {year}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>

          {/* Search Input */}
          <div className="relative w-[320px]">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
            <Input
              placeholder={t("seasonFilters.searchPlaceholder")}
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="pl-10 border-border acm-rounded-sm h-9 text-sm"
            />
          </div>

          {/* Clear Filters */}
          {(filters.crop !== 'all' || filters.status !== 'all' || filters.year !== 'all') && (
            <Button
              variant="ghost"
              size="sm"
              onClick={() => setFilters({ crop: 'all', status: 'all', year: 'all' })}
              className="h-9 text-sm text-muted-foreground hover:text-foreground"
            >
              <X className="w-3 h-3 mr-1" />
              {t("seasonFilters.clear")}
            </Button>
          )}
        </div>
      </CardContent>
    </Card>
  );
}



