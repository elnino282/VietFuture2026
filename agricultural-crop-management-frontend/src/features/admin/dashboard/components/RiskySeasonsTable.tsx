import { useNavigate } from 'react-router-dom';
import {
  AlertTriangle,
  ExternalLink,
  Loader2,
} from 'lucide-react';
import { AdminContentCard } from '@/features/admin/shared/ui';
import {
  Alert,
  AlertDescription,
  AlertTitle,
  Badge,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/shared/ui';
import type {
  RiskDataCoverage,
  TransformedRiskySeason,
  RiskLevel,
} from '../types';
import { useI18n } from '@/shared/lib/hooks/useI18n';

interface RiskySeasonsTableProps {
  seasons: TransformedRiskySeason[];
  isLoading?: boolean;
  error?: Error | null;
  riskDataLimited?: boolean;
  dataCoverage?: RiskDataCoverage;
}

const getRiskBadgeVariant = (level: RiskLevel): 'destructive' | 'default' | 'secondary' => {
  switch (level) {
    case 'high':
      return 'destructive';
    case 'medium':
      return 'default';
    case 'low':
      return 'secondary';
  }
};

const buildCoverageMessage = (
  dataCoverage: RiskDataCoverage | undefined,
  t: (key: string, optionsOrDefault?: Record<string, unknown> | string) => string,
) => {
  if (!dataCoverage) {
    return t('admin.dashboard.riskySeasons.coverage.someUnavailable');
  }

  const missingSources: string[] = [];
  if (!dataCoverage.incidentDataAvailable) {
    missingSources.push(t('admin.dashboard.riskySeasons.coverage.incidentData'));
  }
  if (!dataCoverage.taskDataAvailable) {
    missingSources.push(t('admin.dashboard.riskySeasons.coverage.taskData'));
  }

  if (missingSources.length === 0) {
    return t('admin.dashboard.riskySeasons.coverage.someUnavailable');
  }

  return t('admin.dashboard.riskySeasons.coverage.missing', {
    sources: missingSources.join(` ${t('common.and')} `),
  });
};

export function RiskySeasonsTable({
  seasons,
  isLoading = false,
  error = null,
  riskDataLimited = false,
  dataCoverage,
}: RiskySeasonsTableProps) {
  const navigate = useNavigate();
  const { t } = useI18n();

  const handleRowClick = (seasonId: number) => {
    navigate(`/admin/seasons/${seasonId}`);
  };

  const coverageMessage = buildCoverageMessage(dataCoverage, t);

  return (
    <AdminContentCard>
      <CardHeader>
        <div className="flex items-center gap-2">
          <AlertTriangle className="h-5 w-5 text-amber-500" />
          <CardTitle className="text-lg">{t('admin.dashboard.riskySeasons.title')}</CardTitle>
        </div>
        <CardDescription>
          {t('admin.dashboard.riskySeasons.description')}
        </CardDescription>
      </CardHeader>
      <CardContent>
        {isLoading ? (
          <div className="flex items-center justify-center py-8 text-muted-foreground">
            <Loader2 className="h-4 w-4 mr-2 animate-spin" />
            {t('admin.dashboard.riskySeasons.loading')}
          </div>
        ) : error ? (
          <Alert variant="destructive">
            <AlertTriangle className="h-4 w-4" />
            <AlertTitle>{t('admin.dashboard.riskySeasons.errorTitle')}</AlertTitle>
            <AlertDescription>
              {error.message || t('admin.dashboard.riskySeasons.errorDescription')}
            </AlertDescription>
          </Alert>
        ) : seasons.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-8 text-center">
            <div className={`rounded-full p-3 mb-3 ${riskDataLimited ? 'bg-amber-50' : 'bg-emerald-50'}`}>
              <AlertTriangle className={`h-6 w-6 ${riskDataLimited ? 'text-amber-600' : 'text-emerald-500'}`} />
            </div>
            <p className="text-sm font-medium text-muted-foreground">
              {riskDataLimited
                ? t('admin.dashboard.riskySeasons.limited')
                : t('admin.dashboard.riskySeasons.emptyTitle')}
            </p>
            <p className="text-xs text-muted-foreground mt-1">
              {riskDataLimited ? coverageMessage : t('admin.dashboard.riskySeasons.emptyDescription')}
            </p>
          </div>
        ) : (
          <div className="space-y-4">
            {riskDataLimited && (
              <Alert>
                <AlertTriangle className="h-4 w-4" />
                <AlertTitle>{t('admin.dashboard.riskySeasons.limited')}</AlertTitle>
                <AlertDescription>{coverageMessage}</AlertDescription>
              </Alert>
            )}
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>{t('admin.dashboard.riskySeasons.table.season')}</TableHead>
                  <TableHead>{t('admin.dashboard.riskySeasons.table.farmPlot')}</TableHead>
                  <TableHead className="text-center">{t('admin.dashboard.riskySeasons.table.incidents')}</TableHead>
                  <TableHead className="text-center">{t('admin.dashboard.riskySeasons.table.overdue')}</TableHead>
                  <TableHead>{t('admin.dashboard.riskySeasons.table.riskBasis')}</TableHead>
                  <TableHead className="text-center">{t('admin.dashboard.riskySeasons.table.risk')}</TableHead>
                  <TableHead className="w-8"></TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {seasons.map((season) => (
                  <TableRow
                    key={season.seasonId}
                    className="cursor-pointer hover:bg-muted/50 transition-colors"
                    onClick={() => handleRowClick(season.seasonId)}
                  >
                    <TableCell className="font-medium">
                      {season.seasonName}
                    </TableCell>
                    <TableCell className="text-muted-foreground">
                      {season.farmName ?? '-'} / {season.plotName ?? '-'}
                    </TableCell>
                    <TableCell className="text-center">
                      <Badge variant={season.incidentCount > 0 ? 'destructive' : 'secondary'}>
                        {season.incidentCount}
                      </Badge>
                    </TableCell>
                    <TableCell className="text-center">
                      <Badge variant={season.overdueTaskCount > 0 ? 'destructive' : 'secondary'}>
                        {season.overdueTaskCount}
                      </Badge>
                    </TableCell>
                    <TableCell>
                      <div className="flex flex-wrap gap-1">
                        {season.riskBasis.map((basis) => (
                          <Badge key={`${season.seasonId}-${basis}`} variant="outline">
                            {t(`admin.dashboard.riskySeasons.riskBasis.${basis}`)}
                          </Badge>
                        ))}
                      </div>
                    </TableCell>
                    <TableCell className="text-center">
                      <Badge variant={getRiskBadgeVariant(season.riskLevel)}>
                        {t(`admin.dashboard.riskySeasons.riskLevel.${season.riskLevel}`)}
                      </Badge>
                    </TableCell>
                    <TableCell>
                      <ExternalLink className="h-4 w-4 text-muted-foreground" />
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
        )}
      </CardContent>
    </AdminContentCard>
  );
}
