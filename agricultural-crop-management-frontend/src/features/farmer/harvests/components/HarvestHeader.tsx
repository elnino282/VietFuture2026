import { Button } from "@/shared/ui/button";
import { Card, CardContent } from "@/shared/ui/card";
import { useI18n } from "@/hooks/useI18n";
import { PageHeader } from "@/shared/ui";
import { Plus, Wheat } from "lucide-react";

interface HarvestHeaderProps {
    onAddBatch: () => void;
    addDisabled?: boolean;
    lockMessage?: string;
}

export function HarvestHeader({
    onAddBatch,
    addDisabled = false,
    lockMessage,
}: HarvestHeaderProps) {
    const { t } = useI18n();
    
    return (
        <Card className="mb-6 border border-border rounded-xl shadow-sm">
            <CardContent className="px-6 py-4">
                <PageHeader
                    className="mb-0"
                    icon={<Wheat className="w-8 h-8" />}
                    title={t('harvests.title')}
                    subtitle={t('harvests.subtitle')}
                    actions={
                        <Button
                            onClick={onAddBatch}
                            variant="default"
                            disabled={addDisabled}
                            title={addDisabled ? lockMessage : undefined}
                        >
                            <Plus className="w-4 h-4 mr-2" />
                            {t('harvests.createButton')}
                        </Button>
                    }
                />
            </CardContent>
        </Card>
    );
}



