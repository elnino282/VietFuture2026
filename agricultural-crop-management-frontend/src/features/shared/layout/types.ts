import type {
  AiDrawerProps,
  AppShellProps,
  BreadcrumbKind,
  BreadcrumbPath,
  HeaderProps as WidgetHeaderProps,
  Language,
  NavigationItem,
  Notification,
  NotificationsDrawerProps,
  PortalConfig,
  PortalType,
  ProfileMenuProps,
  SearchBarProps as WidgetSearchBarProps,
  SidebarProps,
  Theme,
} from "@/widgets/layout/model/types";

export type {
  AiDrawerProps,
  AppShellProps,
  BreadcrumbKind,
  BreadcrumbPath,
  Language,
  NavigationItem,
  Notification,
  NotificationsDrawerProps,
  PortalConfig,
  PortalType,
  ProfileMenuProps,
  SidebarProps,
  Theme,
};

/**
 * Legacy Header props used by feature-level layout components.
 * Kept for backward compatibility with older header implementation.
 */
export interface HeaderProps extends WidgetHeaderProps {
  searchQuery: string;
  onSearchChange: (query: string) => void;
}

/**
 * Legacy SearchBar props used by feature-level layout components.
 * Kept for backward compatibility with older search bar implementation.
 */
export interface SearchBarProps extends Partial<WidgetSearchBarProps> {
  value: string;
  onChange: (value: string) => void;
}
