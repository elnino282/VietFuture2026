import { useEffect, useState } from "react";
import { cn } from "@/shared/lib";
import {
  getChatDisplayName,
  getInitials,
  getRoleKey,
} from "../lib/chatDisplayHelpers";
import type { ChatContactProfile } from "../model/types";

type ChatAvatarSize = "sm" | "md" | "lg";

type ChatAvatarProps = {
  profile: ChatContactProfile | null | undefined;
  fallbackUid?: string | null;
  roleOverride?: string | null;
  size?: ChatAvatarSize;
  className?: string;
};

const sizeClassByVariant: Record<ChatAvatarSize, string> = {
  sm: "text-sm",
  md: "text-sm",
  lg: "text-2xl",
};

const sizeStyleByVariant: Record<
  ChatAvatarSize,
  { height: string; width: string }
> = {
  sm: { height: "40px", width: "40px" },
  md: { height: "44px", width: "44px" },
  lg: { height: "70px", width: "70px" },
};

function getAvatarToneClass(role: string | null | undefined): string {
  switch (getRoleKey(role)) {
    case "FARMER":
      return "chat-avatar--farmer";
    case "BUYER":
      return "chat-avatar--buyer";
    case "ADMIN":
      return "chat-avatar--admin";
    case "EMPLOYEE":
      return "chat-avatar--employee";
    default:
      return "chat-avatar--unknown";
  }
}

export function ChatAvatar({
  profile,
  fallbackUid = null,
  roleOverride = null,
  size = "md",
  className,
}: ChatAvatarProps) {
  const avatarUrl = profile?.avatarUrl?.trim() || null;
  const [hasImageError, setHasImageError] = useState(false);
  const initials = getInitials(profile, fallbackUid);
  const toneClass = getAvatarToneClass(roleOverride ?? profile?.role ?? null);
  const displayName = getChatDisplayName(profile, fallbackUid);
  const avatarLabel = `${displayName} avatar`;
  const showImage = Boolean(avatarUrl && !hasImageError);

  useEffect(() => {
    setHasImageError(false);
  }, [avatarUrl]);

  return (
    <div
      role={showImage ? undefined : "img"}
      aria-label={showImage ? undefined : avatarLabel}
      style={sizeStyleByVariant[size]}
      className={cn(
        "chat-avatar select-none overflow-hidden rounded-full font-semibold tracking-wide text-white shadow-sm ring-2 ring-white",
        sizeClassByVariant[size],
        toneClass,
        className
      )}
    >
      {showImage ? (
        <img
          src={avatarUrl ?? undefined}
          alt={avatarLabel}
          className="h-full w-full object-cover"
          loading="lazy"
          onError={() => setHasImageError(true)}
        />
      ) : (
        <span aria-hidden="true">{initials}</span>
      )}
    </div>
  );
}
