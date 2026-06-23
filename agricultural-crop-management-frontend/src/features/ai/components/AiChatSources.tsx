import type { AiChatSource } from '@/services/aiChatService';

type AiChatSourcesProps = {
    sources?: AiChatSource[];
};

export function AiChatSources({ sources }: AiChatSourcesProps) {
    if (!sources?.length) {
        return null;
    }

    return (
        <details className="mt-3 rounded-lg border border-border bg-muted/40 px-3 py-2 text-xs text-muted-foreground">
            <summary className="cursor-pointer select-none font-semibold text-foreground">
                Nguồn tham khảo
            </summary>
            <div className="mt-2 space-y-2">
                {sources.map((source, index) => {
                    const title = source.heading || source.file_name || `Nguồn ${index + 1}`;

                    return (
                        <div key={`${title}-${source.page ?? index}`} className="space-y-1">
                            <div className="font-medium text-foreground">{title}</div>
                            <div className="flex flex-wrap gap-x-2 gap-y-1">
                                {source.file_name && <span>{source.file_name}</span>}
                                {source.page !== undefined && <span>Trang {source.page}</span>}
                            </div>
                            {source.snippet && (
                                <p className="line-clamp-3 whitespace-pre-wrap break-words leading-5">
                                    {source.snippet}
                                </p>
                            )}
                        </div>
                    );
                })}
            </div>
        </details>
    );
}
