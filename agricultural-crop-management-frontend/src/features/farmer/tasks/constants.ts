import { Droplets, Sprout, ShowerHead, Eye, Package } from "lucide-react";
import type { TaskStatus } from "./types";

export const TASK_TYPES = {
  irrigation: {
    labelKey: "tasks.types.irrigation",
    fallbackLabel: "Irrigation",
    icon: Droplets,
    color: "var(--secondary)",
  },
  fertilizing: {
    labelKey: "tasks.types.fertilizing",
    fallbackLabel: "Fertilizing",
    icon: Sprout,
    color: "var(--primary)",
  },
  spraying: {
    labelKey: "tasks.types.spraying",
    fallbackLabel: "Spraying",
    icon: ShowerHead,
    color: "var(--accent)",
  },
  scouting: {
    labelKey: "tasks.types.scouting",
    fallbackLabel: "Scouting",
    icon: Eye,
    color: "var(--muted-foreground)",
  },
  harvesting: {
    labelKey: "tasks.types.harvesting",
    fallbackLabel: "Harvesting",
    icon: Package,
    color: "var(--destructive)",
  },
} as const;

export const STATUS_COLORS: Record<TaskStatus, string> = {
  todo: "bg-secondary/10 text-secondary border-secondary/20",
  "in-progress": "bg-accent/10 text-accent border-accent/20",
  overdue: "bg-destructive/10 text-destructive border-destructive/20",
  completed: "bg-primary/10 text-primary border-primary/20",
};

export const STATUS_LABELS: Record<TaskStatus, { labelKey: string; fallbackLabel: string }> = {
  todo: { labelKey: "tasks.status.pending", fallbackLabel: "Pending" },
  "in-progress": { labelKey: "tasks.status.inProgress", fallbackLabel: "In progress" },
  overdue: { labelKey: "tasks.status.overdue", fallbackLabel: "Overdue" },
  completed: { labelKey: "tasks.status.done", fallbackLabel: "Done" },
};

export const STATUS_COLOR_VALUES: Record<TaskStatus, string> = {
  todo: "var(--secondary)",
  "in-progress": "var(--accent)",
  overdue: "var(--destructive)",
  completed: "var(--primary)",
};

export const KANBAN_COLUMNS = [
  { status: "todo" as TaskStatus, titleKey: "tasks.status.pending", fallbackTitle: "Pending", color: "var(--secondary)" },
  { status: "in-progress" as TaskStatus, titleKey: "tasks.status.inProgress", fallbackTitle: "In progress", color: "var(--accent)" },
  { status: "overdue" as TaskStatus, titleKey: "tasks.status.overdue", fallbackTitle: "Overdue", color: "var(--destructive)" },
  { status: "completed" as TaskStatus, titleKey: "tasks.status.done", fallbackTitle: "Done", color: "var(--primary)" },
];

// Note: MOCK_TASKS removed - now using entity API hooks
