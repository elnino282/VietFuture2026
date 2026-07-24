import React from "react";
import { ImageOff, type LucideIcon } from "lucide-react";
import { cn } from "@/shared/lib";

export interface ImagePlaceholderProps {
  icon?: LucideIcon;
  label?: string;
  className?: string;
}

export function ImagePlaceholder({
  icon: Icon = ImageOff,
  label = "Chưa có ảnh",
  className,
}: ImagePlaceholderProps) {
  return (
    <div
      className={cn(
        "flex h-full w-full flex-col items-center justify-center bg-earth-100 text-earth-400",
        className
      )}
    >
      <Icon className="mb-2 h-8 w-8 opacity-50" />
      <span className="text-sm font-medium">{label}</span>
    </div>
  );
}
