import { fireEvent, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { HarvestTable } from "./HarvestTable";
import type { HarvestBatch, HarvestGrade, HarvestStatus } from "../types";

vi.mock("@/shared/contexts", () => ({
  usePreferences: () => ({
    preferences: {
      locale: "en-US",
      weightUnit: "kg",
    },
  }),
}));

vi.mock("@/shared/lib/hooks/useI18n", () => ({
  useI18n: () => ({
    t: (key: string, optionsOrDefault?: Record<string, unknown> | string) => {
      if (typeof optionsOrDefault === "string") return optionsOrDefault;
      if (typeof optionsOrDefault?.defaultValue === "string") {
        return optionsOrDefault.defaultValue;
      }
      return key;
    },
  }),
}));

const batch: HarvestBatch = {
  id: "77",
  batchId: "77",
  seasonId: 33,
  seasonName: "Winter 2026",
  date: "2026-04-18",
  createdAt: "2026-04-18T10:00:00",
  quantity: 120.5,
  grade: "A",
  status: "stored",
};

describe("HarvestTable", () => {
  it("renders the status badge supplied for a harvest batch", () => {
    render(
      <HarvestTable
        batches={[batch]}
        totalBatches={1}
        selectedBatchIds={[]}
        onViewDetails={vi.fn()}
        onEditBatch={vi.fn()}
        onDeleteBatch={vi.fn()}
        onDeleteSelected={vi.fn()}
        onToggleBatchSelection={vi.fn()}
        onToggleAllSelection={vi.fn()}
        onExport={vi.fn()}
        onPrint={vi.fn()}
        getStatusBadge={(status?: HarvestStatus | null) => status ? <span>{status === "stored" ? "Stored" : status}</span> : null}
        getGradeBadge={(grade?: HarvestGrade | null) => <span>{grade ?? "N/A"}</span>}
      />
    );

    expect(screen.getByText("Stored")).toBeInTheDocument();
  });

  it("calls edit handler from the row action menu", async () => {
    const onEditBatch = vi.fn();
    const user = userEvent.setup();

    render(
      <HarvestTable
        batches={[batch]}
        totalBatches={1}
        selectedBatchIds={[]}
        onViewDetails={vi.fn()}
        onEditBatch={onEditBatch}
        onDeleteBatch={vi.fn()}
        onDeleteSelected={vi.fn()}
        onToggleBatchSelection={vi.fn()}
        onToggleAllSelection={vi.fn()}
        onExport={vi.fn()}
        onPrint={vi.fn()}
        getStatusBadge={(status?: HarvestStatus | null) => status ? <span>{status === "stored" ? "Stored" : status}</span> : null}
        getGradeBadge={(grade?: HarvestGrade | null) => <span>{grade ?? "N/A"}</span>}
      />
    );

    await user.click(screen.getByRole("button", { name: "Open actions for batch 77" }));
    await user.click(await screen.findByText("harvests.table.actions.editBatch"));

    expect(onEditBatch).toHaveBeenCalledWith(batch);
  });
});
