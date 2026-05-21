import { useDrop } from "react-dnd";
import { Badge } from "@/shared/ui/badge";
import { ScrollArea } from "@/shared/ui/scroll-area";
import { useI18n } from "@/hooks/useI18n";
import type { Task, TaskStatus } from "../types";
import { TaskCard } from "./TaskCard";

interface KanbanColumnProps {
  status: TaskStatus;
  title: string;
  color: string;
  tasks: Task[];
  onTaskMove: (taskId: string, newStatus: TaskStatus) => void;
  onDelete: (taskId: string) => void;
  disableMutations?: boolean;
}

export function KanbanColumn({
  status,
  title,
  color,
  tasks,
  onTaskMove,
  onDelete,
  disableMutations = false,
}: KanbanColumnProps) {
  const { t } = useI18n();
  const [{ isOver }, drop] = useDrop({
    accept: "TASK",
    canDrop: () => !disableMutations,
    drop: (item: { id: string }) => {
      if (disableMutations) return;
      onTaskMove(item.id, status);
    },
    collect: (monitor) => ({
      isOver: monitor.isOver(),
    }),
  });

  return (
    <div
      ref={drop}
      className={`bg-card rounded-xl border-2 transition-all ${
        isOver ? "border-primary bg-primary/5" : "border-border"
      }`}
    >
      <div className="p-4 border-b border-border">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <div className="w-3 h-3 rounded-full" style={{ backgroundColor: color }}></div>
            <h3 className="text-sm">{title}</h3>
          </div>
          <Badge className="numeric bg-muted text-foreground border-border">{tasks.length}</Badge>
        </div>
      </div>
      <ScrollArea className="h-[600px] p-3">
        <div className="space-y-3">
          {tasks.map((task) => (
            <TaskCard
              key={task.id}
              task={task}
              onDelete={onDelete}
              disableMutations={disableMutations}
            />
          ))}
          {tasks.length === 0 && (
            <div className="text-center py-8 text-sm text-muted-foreground">{t("tasks.empty.title")}</div>
          )}
        </div>
      </ScrollArea>
    </div>
  );
}
