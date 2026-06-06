import { useI18n } from "@/shared/lib/hooks/useI18n";
import { cn } from "@/shared/lib/utils";
import { ArrowLeft } from "lucide-react";
import { forwardRef } from "react";
import { useLocation, useNavigate, type NavigateOptions, type To } from "react-router-dom";
import { Button } from "../button";

type ButtonProps = React.ComponentProps<typeof Button>;

export interface BackButtonProps extends Omit<ButtonProps, "asChild"> {
  /** Parent route to navigate to. If omitted, the current route's parent path is used. */
  to?: To;
  /** Fallback route to use when `to` is not provided. */
  fallbackTo?: To;
  /** Custom label. Defaults to common.back. */
  label?: string;
  /** Show icon only (no label). */
  iconOnly?: boolean;
  /** Ask for confirmation before leaving, intended for dirty create/edit forms. */
  confirmOnLeave?: boolean;
  /** Confirmation message shown when `confirmOnLeave` is true. */
  confirmMessage?: string;
  /** React Router navigate options. */
  navigateOptions?: NavigateOptions;
}

function getParentPath(pathname: string): string {
  const trimmedPath = pathname.replace(/\/+$/, "");
  const segments = trimmedPath.split("/").filter(Boolean);

  if (segments.length <= 1) {
    return "/";
  }

  return `/${segments.slice(0, -1).join("/")}`;
}

export const BackButton = forwardRef<HTMLButtonElement, BackButtonProps>(
  (
    {
      to,
      fallbackTo,
      label,
      iconOnly = false,
      variant = "ghost",
      size,
      className,
      onClick,
      confirmOnLeave = false,
      confirmMessage,
      navigateOptions,
      ...props
    },
    ref,
  ) => {
    const navigate = useNavigate();
    const location = useLocation();
    const { t } = useI18n();

    const resolvedLabel = label ?? t("common.back");
    const resolvedConfirmMessage =
      confirmMessage ?? t("common.unsavedChangesConfirm", "You have unsaved changes. Leave this page?");

    const handleClick = (event: React.MouseEvent<HTMLButtonElement>) => {
      if (confirmOnLeave && !window.confirm(resolvedConfirmMessage)) {
        event.preventDefault();
        return;
      }

      if (onClick) {
        onClick(event);
        return;
      }

      navigate(to ?? fallbackTo ?? getParentPath(location.pathname), navigateOptions);
    };

    const buttonSize = iconOnly ? "icon" : size;

    return (
      <Button
        ref={ref}
        variant={variant}
        size={buttonSize}
        className={cn(!iconOnly && "pl-0", className)}
        onClick={handleClick}
        aria-label={iconOnly ? props["aria-label"] ?? resolvedLabel : props["aria-label"]}
        {...props}
      >
        <ArrowLeft className={cn("w-4 h-4", !iconOnly && "mr-2")} />
        {!iconOnly && resolvedLabel}
      </Button>
    );
  },
);

BackButton.displayName = "BackButton";
