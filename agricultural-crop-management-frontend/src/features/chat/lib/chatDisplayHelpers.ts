import type { ChatContactProfile } from "../model/types";
import type { ChatWidgetTransactionStatus } from "../model/widgetTypes";

/**
 * Returns the primary display name for a chat contact.
 *
 * Priority:
 *  1. farmName (if available - best identifier for farm accounts)
 *  2. representativeName
 *  3. displayName / fullName
 *  4. "Account #userId" (last resort fallback)
 */
export function getChatDisplayName(
  profile: ChatContactProfile | null | undefined,
  fallbackUid?: string | null
): string {
  if (!profile) {
    const idLabel = extractUserIdLabel(fallbackUid);
    return idLabel ? `Account ${idLabel}` : "Unknown contact";
  }

  if (profile.farmName) {
    return profile.farmName;
  }

  if (profile.representativeName) {
    return profile.representativeName;
  }

  if (profile.displayName) {
    return profile.displayName;
  }

  return `Account #${profile.userId}`;
}

/**
 * Returns a secondary subtitle line for a chat contact.
 *
 * If the primary name is the farm, subtitle shows representative name.
 * If the primary name is a person, subtitle shows farm or role info.
 */
export function getChatSubtitle(
  profile: ChatContactProfile | null | undefined
): string | null {
  if (!profile) {
    return null;
  }

  if (profile.farmName && profile.representativeName) {
    return profile.representativeName;
  }

  if (
    !profile.farmName &&
    profile.representativeName &&
    profile.displayName &&
    profile.representativeName !== profile.displayName
  ) {
    return profile.displayName;
  }

  if (profile.role) {
    return formatRole(profile.role);
  }

  return null;
}

/**
 * Joins non-empty text parts with a separator.
 */
export function joinDefinedParts(
  parts: Array<string | null | undefined>,
  separator = " · "
): string | null {
  const normalized = parts
    .map((part) => part?.trim())
    .filter((part): part is string => Boolean(part));

  if (normalized.length === 0) {
    return null;
  }

  return normalized.join(separator);
}

/**
 * Returns normalized role key for UI mappings.
 */
export function getRoleKey(role: string | null | undefined): string {
  return (role ?? "").toUpperCase().replace(/^ROLE_/, "");
}

/**
 * Returns up to 2 initials from a display name for avatar fallbacks.
 */
export function getInitials(
  profile: ChatContactProfile | null | undefined,
  fallbackUid?: string | null
): string {
  const name = getChatDisplayName(profile, fallbackUid).trim();
  if (!name) {
    return "A";
  }

  const words = name.split(/\s+/).filter(Boolean);
  const initials = words
    .map((word) => {
      const letter = /^\p{L}/u.exec(word);
      return letter ? letter[0].toUpperCase() : "";
    })
    .filter(Boolean);

  if (initials.length >= 2) {
    return `${initials[0]}${initials[initials.length - 1]}`.slice(0, 2);
  }

  if (initials.length === 1) {
    return initials[0];
  }

  return "A";
}

/**
 * Formats a chat timestamp for display in conversation list.
 * Shows time for today, date for older messages.
 */
export function formatChatTime(value: Date | null | undefined): string {
  if (!value) {
    return "";
  }

  const now = new Date();
  const isToday =
    value.getFullYear() === now.getFullYear() &&
    value.getMonth() === now.getMonth() &&
    value.getDate() === now.getDate();

  if (isToday) {
    return value.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });
  }

  const yesterday = new Date(now);
  yesterday.setDate(yesterday.getDate() - 1);
  const isYesterday =
    value.getFullYear() === yesterday.getFullYear() &&
    value.getMonth() === yesterday.getMonth() &&
    value.getDate() === yesterday.getDate();

  if (isYesterday) {
    return "Yesterday";
  }

  return value.toLocaleDateString([], {
    month: "short",
    day: "numeric",
  });
}

/**
 * Formats a message timestamp for display inside bubbles.
 */
export function formatMessageTime(value: Date | null | undefined): string {
  if (!value) {
    return "";
  }
  return value.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });
}

/**
 * Returns a date separator string for grouping messages by day.
 */
export function formatDateSeparator(value: Date | null | undefined): string {
  if (!value) {
    return "";
  }

  const now = new Date();
  const isToday =
    value.getFullYear() === now.getFullYear() &&
    value.getMonth() === now.getMonth() &&
    value.getDate() === now.getDate();

  if (isToday) {
    return "Today";
  }

  const yesterday = new Date(now);
  yesterday.setDate(yesterday.getDate() - 1);
  const isYesterday =
    value.getFullYear() === yesterday.getFullYear() &&
    value.getMonth() === yesterday.getMonth() &&
    value.getDate() === yesterday.getDate();

  if (isYesterday) {
    return "Yesterday";
  }

  return value.toLocaleDateString([], {
    weekday: "long",
    month: "long",
    day: "numeric",
    year: value.getFullYear() !== now.getFullYear() ? "numeric" : undefined,
  });
}

/**
 * Formats a role string for display.
 */
export function formatRole(role: string | null | undefined): string {
  if (!role) {
    return "";
  }

  const normalized = getRoleKey(role);

  const labels: Record<string, string> = {
    FARMER: "Farmer",
    BUYER: "Buyer",
    ADMIN: "Admin",
    EMPLOYEE: "Employee",
  };

  return labels[normalized] ?? normalized.charAt(0) + normalized.slice(1).toLowerCase();
}

/**
 * Returns a CSS class name for role-based badge coloring.
 */
export function getRoleBadgeClass(role: string | null | undefined): string {
  if (!role) {
    return "chat-role-badge chat-role-badge--unknown";
  }

  const normalized = getRoleKey(role);

  const classes: Record<string, string> = {
    FARMER: "chat-role-badge chat-role-badge--farmer",
    BUYER: "chat-role-badge chat-role-badge--buyer",
    ADMIN: "chat-role-badge chat-role-badge--admin",
    EMPLOYEE: "chat-role-badge chat-role-badge--employee",
  };

  return classes[normalized] ?? "chat-role-badge chat-role-badge--unknown";
}

/**
 * Extracts a user ID label from a Firebase UID (e.g., "u_5" -> "#5").
 */
function extractUserIdLabel(uid: string | null | undefined): string | null {
  if (!uid) {
    return null;
  }

  const match = /^u_(\d+)$/i.exec(uid.trim());
  if (!match) {
    return null;
  }
  return `#${match[1]}`;
}

/* ═══════════════════════════════════════════════════════════════
   Transaction status helpers (ChatWidget)
   ═══════════════════════════════════════════════════════════════ */

const TRANSACTION_STATUS_LABELS: Record<ChatWidgetTransactionStatus, string> = {
  trading: "Đang giao dịch",
  pending_confirmation: "Chờ xác nhận",
  delivered: "Đã giao",
  completed: "Hoàn thành",
  cancelled: "Đã hủy",
};

/**
 * Returns the Vietnamese display label for a transaction status enum key.
 */
export function getTransactionStatusLabel(status: ChatWidgetTransactionStatus): string {
  return TRANSACTION_STATUS_LABELS[status];
}

const TRANSACTION_STATUS_CLASSES: Record<ChatWidgetTransactionStatus, string> = {
  trading: "chat-widget-status--trading",
  pending_confirmation: "chat-widget-status--pending",
  delivered: "chat-widget-status--delivered",
  completed: "chat-widget-status--completed",
  cancelled: "chat-widget-status--cancelled",
};

/**
 * Returns the full CSS class string for a transaction status badge.
 */
export function getTransactionStatusClass(status: ChatWidgetTransactionStatus): string {
  return `chat-widget-status ${TRANSACTION_STATUS_CLASSES[status]}`;
}
