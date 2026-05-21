import { Button } from '@/components/ui/button';
import { Sheet, SheetContent, SheetDescription, SheetHeader, SheetTitle } from '@/components/ui/sheet';
import { Download, Filter, History, Info } from 'lucide-react';

interface AuditHistoryDrawerProps {
    open: boolean;
    onOpenChange: (open: boolean) => void;
}

export function AuditHistoryDrawer({
    open,
    onOpenChange,
}: AuditHistoryDrawerProps) {
    return (
        <Sheet open={open} onOpenChange={onOpenChange}>
            <SheetContent className="w-full sm:w-[600px]">
                <SheetHeader>
                    <SheetTitle className="flex items-center gap-2">
                        <History className="w-5 h-5" />
                        Account Change History
                    </SheetTitle>
                    <SheetDescription>
                        View all changes and activities for this account
                    </SheetDescription>
                </SheetHeader>

                <div className="mt-6 space-y-4">
                    <div className="flex items-center gap-2">
                        <Button variant="outline" size="sm" className="flex-1">
                            <Filter className="w-4 h-4 mr-2" />
                            Filter by Type
                        </Button>
                        <Button variant="outline" size="sm" className="flex-1">
                            <Download className="w-4 h-4 mr-2" />
                            Export CSV
                        </Button>
                    </div>

                    <div className="rounded-lg border bg-muted/30 p-4 text-sm text-muted-foreground">
                        <div className="flex items-center gap-2 text-foreground mb-2">
                            <Info className="w-4 h-4" />
                            Audit history endpoint is not available yet.
                        </div>
                        <p>
                            Account activity cannot be loaded in this drawer until backend audit-log API for farmer management is wired.
                        </p>
                    </div>
                </div>
            </SheetContent>
        </Sheet>
    );
}
