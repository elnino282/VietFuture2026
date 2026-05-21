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
        <div className={cn("min-h-screen acm-main-content pb-20", className)}>
            <div
                className={cn(
                    "mx-auto p-4 md:p-6",
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

