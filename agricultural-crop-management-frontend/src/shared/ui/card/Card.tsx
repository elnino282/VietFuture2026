import * as React from "react";
import { cva, type VariantProps } from "class-variance-authority";

import { cn } from "@/shared/lib";

const cardVariants = cva(
    "bg-card text-card-foreground flex flex-col rounded-xl border border-border",
    {
        variants: {
            variant: {
                default: "gap-6 shadow-[0_4px_12px_rgba(0,0,0,0.05)]",
                "page-header": "gap-4 shadow-sm",
                filter: "gap-4 shadow-sm",
                metric: "gap-3 shadow-sm",
                content: "gap-4 shadow-sm",
                elevated: "gap-6 acm-card-elevated acm-hover-surface",
            },
        },
        defaultVariants: {
            variant: "default",
        },
    },
);

type CardVariantProps = VariantProps<typeof cardVariants>;

export interface CardProps
    extends React.HTMLAttributes<HTMLDivElement>,
    CardVariantProps {}

const Card = React.forwardRef<
    HTMLDivElement,
    CardProps
>(({ className, variant, ...props }, ref) => (
    <div
        ref={ref}
        data-slot="card"
        className={cn(
            cardVariants({ variant }),
            className,
        )}
        {...props}
    />
));
Card.displayName = "Card";

const CardHeader = React.forwardRef<
    HTMLDivElement,
    React.HTMLAttributes<HTMLDivElement>
>(({ className, ...props }, ref) => (
    <div
        ref={ref}
        data-slot="card-header"
        className={cn(
            "@container/card-header grid auto-rows-min grid-rows-[auto_auto] items-start gap-1.5 px-6 pt-6 has-data-[slot=card-action]:grid-cols-[1fr_auto] [.border-b]:pb-6",
            className,
        )}
        {...props}
    />
));
CardHeader.displayName = "CardHeader";

const CardTitle = React.forwardRef<
    HTMLHeadingElement,
    React.HTMLAttributes<HTMLHeadingElement>
>(({ className, ...props }, ref) => (
    <h4
        ref={ref}
        data-slot="card-title"
        className={cn("leading-none", className)}
        {...props}
    />
));
CardTitle.displayName = "CardTitle";

const CardDescription = React.forwardRef<
    HTMLParagraphElement,
    React.HTMLAttributes<HTMLParagraphElement>
>(({ className, ...props }, ref) => (
    <p
        ref={ref}
        data-slot="card-description"
        className={cn("text-muted-foreground", className)}
        {...props}
    />
));
CardDescription.displayName = "CardDescription";

const CardAction = React.forwardRef<
    HTMLDivElement,
    React.HTMLAttributes<HTMLDivElement>
>(({ className, ...props }, ref) => (
    <div
        ref={ref}
        data-slot="card-action"
        className={cn(
            "col-start-2 row-span-2 row-start-1 self-start justify-self-end",
            className,
        )}
        {...props}
    />
));
CardAction.displayName = "CardAction";

const CardContent = React.forwardRef<
    HTMLDivElement,
    React.HTMLAttributes<HTMLDivElement>
>(({ className, ...props }, ref) => (
    <div
        ref={ref}
        data-slot="card-content"
        className={cn("px-6 [&:last-child]:pb-6", className)}
        {...props}
    />
));
CardContent.displayName = "CardContent";

const CardFooter = React.forwardRef<
    HTMLDivElement,
    React.HTMLAttributes<HTMLDivElement>
>(({ className, ...props }, ref) => (
    <div
        ref={ref}
        data-slot="card-footer"
        className={cn("flex items-center px-6 pb-6 [.border-t]:pt-6", className)}
        {...props}
    />
));
CardFooter.displayName = "CardFooter";

export {
    cardVariants,
    Card,
    CardHeader,
    CardFooter,
    CardTitle,
    CardAction,
    CardDescription,
    CardContent,
};
