import type { ChatContactProfile } from "../model/types";

export function getChatDisplayName(
  profile: ChatContactProfile | null | undefined,
  fallbackUid?: string | null,
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

export function getChatSubtitle(
  profile: ChatContactProfile | null | undefined,
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

export function joinDefinedParts(
  parts: Array<string | null | undefined>,
  separator = " - ",
): string | null {
  const normalized = parts
    .map((part) => part?.trim())
    .filter((part): part is string => Boolean(part));

  if (normalized.length === 0) {
    return null;
  }

  return normalized.join(separator);
}

export function getRoleKey(role: string | null | undefined): string {
  return (role ?? "").toUpperCase().replace(/^ROLE_/, "");
}

export function getInitials(
  profile: ChatContactProfile | null | undefined,
  fallbackUid?: string | null,
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

export function formatMessageTime(value: Date | null | undefined): string {
  if (!value) {
    return "";
  }
  return value.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });
}

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
