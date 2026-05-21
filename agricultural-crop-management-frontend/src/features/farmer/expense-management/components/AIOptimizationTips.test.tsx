import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi, beforeEach } from "vitest";
import { AIOptimizationTips } from "./AIOptimizationTips";

const mockSummaryHook = vi.fn();
const mockAiMutationHook = vi.fn();

vi.mock("@/shared/contexts", () => ({
    usePreferences: () => ({
        preferences: {
            currency: "USD",
            locale: "en-US",
        },
    }),
}));

vi.mock("@/entities/expense", () => ({
    useExpenseCostInsightsSummary: (...args: unknown[]) => mockSummaryHook(...args),
    useExpenseCostAiSuggestion: (...args: unknown[]) => mockAiMutationHook(...args),
}));

describe("AIOptimizationTips", () => {
    beforeEach(() => {
        vi.clearAllMocks();
        mockSummaryHook.mockReturnValue({
            data: null,
            isLoading: false,
            isError: false,
            error: null,
            refetch: vi.fn(),
        });
        mockAiMutationHook.mockReturnValue({
            data: null,
            isPending: false,
            isError: false,
            error: null,
            mutate: vi.fn(),
        });
    });

    it("renders deterministic cost insights from real expense rows", () => {
        render(
            <AIOptimizationTips
                seasonId={1}
                expenses={[
                    {
                        id: 1,
                        date: "2026-05-10",
                        category: "Labor",
                        description: "Worker payment",
                        linkedSeasonId: 1,
                        linkedPlotId: 1,
                        amount: 500,
                        status: "PENDING",
                    },
                    {
                        id: 2,
                        date: "2026-05-12",
                        category: "Fertilizer",
                        description: "NPK",
                        linkedSeasonId: 1,
                        linkedPlotId: 1,
                        amount: 300,
                        status: "PENDING",
                        attachmentUrl: "https://example.com/receipt.jpg",
                    },
                ]}
            />
        );

        expect(screen.getByText("Cost insights")).toBeInTheDocument();
        expect(screen.getByText("Highest cost category")).toBeInTheDocument();
        expect(screen.getByText("Average expense amount")).toBeInTheDocument();
    });

    it("shows empty state with no fake tips when expenses are empty", () => {
        render(<AIOptimizationTips seasonId={1} expenses={[]} />);

        expect(
            screen.getByText("No expense records yet. Add expenses to generate cost insights.")
        ).toBeInTheDocument();
        expect(screen.queryByText("View All Insights")).not.toBeInTheDocument();
    });
});
