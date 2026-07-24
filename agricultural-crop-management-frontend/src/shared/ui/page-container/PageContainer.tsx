import * as React from "react";
import { cn } from "@/shared/lib";

type PageContainerVariant =
    | "default"
    | "wide"
    | "dashboard"
    | "narrow"
    | "form"
    | "full";

export interface PageContainerProps {
    /** Page content */
    children: React.ReactNode;
    /** Additional className for the container */
    className?: string;
    /** Legacy max width variant */
    maxWidth?: "default" | "full" | "narrow";
    /** Preferred layout variant */
    variant?: PageContainerVariant;
}

/**
 * PageContainer Component
 * 
 * Consistent page wrapper that provides:
 * - Max width constraint
 * - Consistent padding
 * - Background color
 * 
 * @example
 * ```tsx
 * <PageContainer>
 *   <PageHeader ... />
 *   <Card>...</Card>
 * </PageContainer>
 * ```
 */
export function PageContainer({
    children,
    className,
    maxWidth = "default",
    variant,
}: PageContainerProps) {
    const resolvedVariant: PageContainerVariant = variant
        ?? (maxWidth === "full"
            ? "full"
            : maxWidth === "narrow"
                ? "narrow"
                : "default");

    return (
        <div className={cn("min-h-screen acm-main-content pb-20 relative", className)}>
            {/* Ambient Top Banner for Dashboard */}
            <div className="absolute top-0 inset-x-0 h-64 pointer-events-none z-0 bg-gradient-to-b from-primary/[0.06] via-primary/[0.02] to-transparent" />
            <div className="absolute top-0 inset-x-0 h-64 pointer-events-none z-0 opacity-20 [mask-image:linear-gradient(to_bottom,white,transparent)]" 
                 style={{ backgroundImage: 'radial-gradient(var(--primary) 1px, transparent 1px)', backgroundSize: '24px 24px' }} />
                 
            <div
                className={cn(
                    "mx-auto p-4 md:p-6 relative z-10",
                    {
                        "max-w-[1920px]": resolvedVariant === "default",
                        "max-w-[1800px]": resolvedVariant === "wide",
                        "max-w-[1600px]": resolvedVariant === "dashboard",
                        "max-w-[1200px]": resolvedVariant === "narrow",
                        "max-w-[960px]": resolvedVariant === "form",
                        // full has no max-width
                    }
                )}
            >
                {children}
            </div>
        </div>
    );
}

PageContainer.displayName = "PageContainer";

