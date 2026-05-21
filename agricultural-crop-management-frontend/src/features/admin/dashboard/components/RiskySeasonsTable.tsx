import { useNavigate } from 'react-router-dom';
import {
  AlertTriangle,
  ExternalLink,
  Loader2,
} from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import type {
  RiskBasis,
  RiskDataCoverage,
  TransformedRiskySeason,
  RiskLevel,
} from '../types';

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

const getRiskLabel = (level: RiskLevel) => {
  switch (level) {
    case 'high':
      return 'High';
    case 'medium':
      return 'Medium';
    case 'low':
      return 'Low';
  }
};

const getRiskBasisLabel = (riskBasis: RiskBasis) => {
  switch (riskBasis) {
    case 'OPEN_INCIDENTS':
      return 'Open incidents';
    case 'OVERDUE_TASKS':
      return 'Overdue tasks';
    case 'HIGH_FDN_RISK':
      return 'High FDN risk';
    case 'INVENTORY_RISK':
      return 'Inventory risk';
  }
};

const buildCoverageMessage = (dataCoverage?: RiskDataCoverage) => {
  if (!dataCoverage) {
    return 'Some risk sources are unavailable.';
  }

  const missingSources: string[] = [];
  if (!dataCoverage.incidentDataAvailable) {
    missingSources.push('incident data');
  }
  if (!dataCoverage.taskDataAvailable) {
    missingSources.push('task data');
  }

  if (missingSources.length === 0) {
    return 'Some risk sources are unavailable.';
  }

  return `Missing ${missingSources.join(' and ')}.`;
};

export function RiskySeasonsTable({
  seasons,
  isLoading = false,
  error = null,
  riskDataLimited = false,
  dataCoverage,
}: RiskySeasonsTableProps) {
  const navigate = useNavigate();

  const handleRowClick = (seasonId: number) => {
    navigate(`/admin/seasons/${seasonId}`);
  };

  const coverageMessage = buildCoverageMessage(dataCoverage);

  return (
    <Card className="border-0 shadow-sm">
      <CardHeader>
        <div className="flex items-center gap-2">
          <AlertTriangle className="h-5 w-5 text-amber-500" />
          <CardTitle className="text-lg">Risky Seasons</CardTitle>
        </div>
        <CardDescription>
          Seasons with incidents, overdue tasks, or operational risks requiring attention
        </CardDescription>
      </CardHeader>
      <CardContent>
        {isLoading ? (
          <div className="flex items-center justify-center py-8 text-muted-foreground">
            <Loader2 className="h-4 w-4 mr-2 animate-spin" />
            Loading risky seasons...
          </div>
        ) : error ? (
          <Alert variant="destructive">
            <AlertTriangle className="h-4 w-4" />
            <AlertTitle>Risk data unavailable</AlertTitle>
            <AlertDescription>
              {error.message || 'Unable to load risky season data right now.'}
            </AlertDescription>
          </Alert>
        ) : seasons.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-8 text-center">
            <div className={`rounded-full p-3 mb-3 ${riskDataLimited ? 'bg-amber-50' : 'bg-emerald-50'}`}>
              <AlertTriangle className={`h-6 w-6 ${riskDataLimited ? 'text-amber-600' : 'text-emerald-500'}`} />
            </div>
            <p className="text-sm font-medium text-muted-foreground">
              {riskDataLimited ? 'Risk data limited' : 'No risky seasons detected'}
            </p>
            <p className="text-xs text-muted-foreground mt-1">
              {riskDataLimited ? coverageMessage : 'All seasons are running smoothly'}
            </p>
          </div>
        ) : (
          <div className="space-y-4">
            {riskDataLimited && (
              <Alert>
                <AlertTriangle className="h-4 w-4" />
                <AlertTitle>Risk data limited</AlertTitle>
                <AlertDescription>{coverageMessage}</AlertDescription>
              </Alert>
            )}
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Season</TableHead>
                  <TableHead>Farm / Plot</TableHead>
                  <TableHead className="text-center">Incidents</TableHead>
                  <TableHead className="text-center">Overdue</TableHead>
                  <TableHead>Risk Basis</TableHead>
                  <TableHead className="text-center">Risk</TableHead>
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
                            {getRiskBasisLabel(basis)}
                          </Badge>
                        ))}
                      </div>
                    </TableCell>
                    <TableCell className="text-center">
                      <Badge variant={getRiskBadgeVariant(season.riskLevel)}>
                        {getRiskLabel(season.riskLevel)}
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
    </Card>
  );
}
