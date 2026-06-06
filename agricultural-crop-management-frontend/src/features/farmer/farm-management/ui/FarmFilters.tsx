import { Input, Select, SelectTrigger, SelectValue, SelectContent, SelectItem } from '@/shared/ui';
import { useTranslation } from 'react-i18next';

interface FarmFiltersProps {
    keyword: string;
    onKeywordChange: (value: string) => void;
    activeFilter: boolean | null;
    onActiveFilterChange: (value: boolean | null) => void;
}

/**
 * Farm filters component for search and active status filtering
 */
export function FarmFilters({
    keyword,
    onKeywordChange,
    activeFilter,
    onActiveFilterChange,
}: FarmFiltersProps) {
    const { t } = useTranslation();

    const handleFilterChange = (value: string) => {
        if (value === 'all') {
            onActiveFilterChange(null);
        } else if (value === 'active') {
            onActiveFilterChange(true);
        } else if (value === 'inactive') {
            onActiveFilterChange(false);
        }
    };

    const currentFilterValue =
        activeFilter === null ? 'all' : activeFilter ? 'active' : 'inactive';

    return (
        <div className="flex flex-wrap items-center justify-start gap-4 mb-6">
            <div className="w-[320px]">
                <Input
                    type="text"
                    placeholder={t('farmManagement.searchPlaceholder')}
                    value={keyword}
                    onChange={(e) => onKeywordChange(e.target.value)}
                />
            </div>
            <Select value={currentFilterValue} onValueChange={handleFilterChange}>
                <SelectTrigger className="w-[180px]">
                    <SelectValue placeholder={t('farmManagement.filters.allStatuses')} />
                </SelectTrigger>
                <SelectContent>
                    <SelectItem value="all">{t('farmManagement.filters.allFarms')}</SelectItem>
                    <SelectItem value="active">{t('farmManagement.filters.activeOnly')}</SelectItem>
                    <SelectItem value="inactive">{t('farmManagement.filters.inactiveOnly')}</SelectItem>
                </SelectContent>
            </Select>
        </div>
    );
}



