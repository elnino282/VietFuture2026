import * as React from "react";
import { Card, CardContent } from "@/shared/ui/card";
import { cn } from "@/shared/lib";
import { BackButton } from "@/shared/ui/back-button";
import { useLocation } from "react-router-dom";

export const adminCardClass =
  "rounded-[18px] border border-border bg-card shadow-sm";

export const adminControlClass =
  "h-9 rounded-[14px] border-border bg-card text-sm";

export const adminButtonClass = "rounded-[14px]";

export const adminTabsListClass =
  "h-10 w-max rounded-[18px] border-0 bg-muted p-1";

export const adminTabsTriggerClass =
  "h-8 rounded-[18px] px-4 text-sm font-medium text-muted-foreground transition-all data-[state=active]:bg-card data-[state=active]:text-foreground data-[state=active]:shadow-sm";

type AdminPageContainerProps = {
  children: React.ReactNode;
  className?: string;
};

export function AdminPageContainer({
  children,
  className,
}: AdminPageContainerProps) {
  return (
    <div className={cn("min-h-full space-y-6 p-4 sm:p-6", className)}>
      {children}
    </div>
  );
}

type AdminCardProps = React.ComponentProps<typeof Card>;

export function AdminCard({ className, ...props }: AdminCardProps) {
  return <Card className={cn(adminCardClass, className)} {...props} />;
}

export function AdminFilterCard({ className, ...props }: AdminCardProps) {
  return (
    <Card
      className={cn(adminCardClass, className)}
      {...props}
    />
  );
}

export function AdminContentCard({ className, ...props }: AdminCardProps) {
  return (
    <Card
      className={cn(adminCardClass, "overflow-hidden", className)}
      {...props}
    />
  );
}

type AdminHeaderCardProps = {
  title: React.ReactNode;
  description?: React.ReactNode;
  metadata?: React.ReactNode;
  actions?: React.ReactNode;
  className?: string;
  backConfirmOnLeave?: boolean;
};

export function AdminHeaderCard({
  title,
  description,
  metadata,
  actions,
  className,
  backConfirmOnLeave = false,
}: AdminHeaderCardProps) {
  const location = useLocation();
  const normalizedPath = location.pathname.replace(/\/+$/, "");
  const showAdminBackButton =
    normalizedPath.startsWith("/admin/") && normalizedPath !== "/admin/dashboard";

  return (
    <>
      {showAdminBackButton ? (
        <BackButton
          to="/admin/dashboard"
          confirmOnLeave={backConfirmOnLeave}
          className="w-fit"
        />
      ) : null}
      <AdminCard className={className}>
        <CardContent className="p-4">
          <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
            <div className="min-w-0 space-y-1">
              <div className="flex flex-wrap items-center gap-2">
                <h1 className="truncate text-2xl font-bold text-foreground">
                  {title}
                </h1>
                {metadata}
              </div>
              {description ? (
                <p className="text-sm text-muted-foreground sm:text-base">
                  {description}
                </p>
              ) : null}
            </div>
            {actions ? (
              <div className="flex w-full flex-col gap-2 sm:w-auto sm:flex-row sm:items-center">
                {actions}
              </div>
            ) : null}
          </div>
        </CardContent>
      </AdminCard>
    </>
  );
}

type AdminMetricCardProps = {
  label: React.ReactNode;
  value: React.ReactNode;
  helper?: React.ReactNode;
  icon?: React.ComponentType<{ className?: string }>;
  iconClassName?: string;
  iconWrapClassName?: string;
  onClick?: () => void;
  disabled?: boolean;
  ariaLabel?: string;
  className?: string;
};

export function AdminMetricCard({
  label,
  value,
  helper,
  icon: Icon,
  iconClassName,
  iconWrapClassName,
  onClick,
  disabled = false,
  ariaLabel,
  className,
}: AdminMetricCardProps) {
  const isClickable = !!onClick && !disabled;

  const handleKeyDown = (event: React.KeyboardEvent<HTMLDivElement>) => {
    if (!isClickable) return;
    if (event.key === "Enter" || event.key === " ") {
      event.preventDefault();
      onClick();
    }
  };

  return (
    <AdminCard
      role={isClickable ? "button" : undefined}
      tabIndex={isClickable ? 0 : undefined}
      aria-label={isClickable ? ariaLabel : undefined}
      aria-disabled={disabled || undefined}
      onClick={isClickable ? onClick : undefined}
      onKeyDown={isClickable ? handleKeyDown : undefined}
      className={cn(
        "transition-shadow duration-200",
        isClickable && "cursor-pointer hover:shadow-md",
        disabled && "cursor-not-allowed opacity-70",
        className,
      )}
    >
      <CardContent className="p-5">
        <div className="flex items-start gap-3">
          {Icon ? (
            <div
              className={cn(
                "flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-primary/10 text-primary",
                iconWrapClassName,
              )}
            >
              <Icon className={cn("h-5 w-5", iconClassName)} />
            </div>
          ) : null}
          <div className="min-w-0 flex-1 space-y-0.5">
            <p className="truncate text-sm font-medium text-muted-foreground">
              {label}
            </p>
            <p className="truncate text-xl font-bold leading-tight text-foreground">
              {value}
            </p>
            {helper ? (
              <p className="text-xs leading-4 text-muted-foreground">
                {helper}
              </p>
            ) : null}
          </div>
        </div>
      </CardContent>
    </AdminCard>
  );
}
