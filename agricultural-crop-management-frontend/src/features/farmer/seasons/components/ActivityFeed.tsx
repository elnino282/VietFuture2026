import { Calendar, CheckCircle2, DollarSign, AlertCircle } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/shared/ui/card';
import { useI18n } from '@/shared/lib/hooks/useI18n';
import { Activity } from '../types';

interface ActivityFeedProps {
  activities: Activity[];
}

export function ActivityFeed({ activities }: ActivityFeedProps) {
  const { t } = useI18n();

  const getActivityIcon = (type: Activity['type']) => {
    switch (type) {
      case 'task':
        return <CheckCircle2 className="w-4 h-4 text-primary" />;
      case 'expense':
        return <DollarSign className="w-4 h-4 text-secondary" />;
      case 'incident':
        return <AlertCircle className="w-4 h-4 text-destructive" />;
      case 'season':
        return <Calendar className="w-4 h-4 text-accent" />;
    }
  };

  return (
    <div className="mt-12 pt-8 border-t border-border/40">
      <div className="mb-6">
        <h2 className="text-xl font-bold tracking-tight text-foreground">{t('seasonActivity.title', 'Hoạt động gần đây')}</h2>
        <p className="text-sm text-muted-foreground mt-1">{t('seasonActivity.description', 'Dòng thời gian các thao tác gần đây')}</p>
      </div>
      <div className="space-y-4 max-w-4xl">
        {activities.map((activity) => (
          <div key={activity.id} className="flex items-start gap-4 p-4 rounded-xl border border-border/50 bg-card/30 hover:bg-muted/50 transition-colors shadow-sm">
            <div className="mt-0.5 p-2 bg-background rounded-full border border-border shadow-sm">
              {getActivityIcon(activity.type)}
            </div>
            <div className="flex-1">
              <div className="text-sm font-medium text-foreground">{activity.action}</div>
              <div className="text-xs text-muted-foreground mt-1.5 flex items-center gap-2">
                <span className="font-medium text-foreground/80">{activity.user}</span>
                <span>•</span>
                <span>{activity.time}</span>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}




