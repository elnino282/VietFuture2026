import { BarChart3 } from "lucide-react";
import type { ReportSection } from "../types";
import { SIDEBAR_ITEMS } from "../constants";
import { useI18n } from "@/hooks/useI18n";

interface SidebarProps {
    activeSection: ReportSection;
    onSectionChange: (section: ReportSection) => void;
}

const SECTION_I18N_KEY: Record<string, string> = {
    yield: "reports.sidebar.sections.yield",
    cost: "reports.sidebar.sections.cost",
    performance: "reports.sidebar.sections.performance",
    pesticide: "reports.sidebar.sections.pesticide",
};

export function Sidebar({ activeSection, onSectionChange }: SidebarProps) {
    const { t } = useI18n();

    return (
        <aside className="sticky top-6 hidden self-start overflow-hidden rounded-[18px] border border-border bg-card shadow-sm lg:block">
            <div className="p-6">
                <div className="flex items-center gap-3 mb-6">
                    <div
                        className="w-10 h-10 rounded-full flex items-center justify-center"
                        style={{
                            background: "linear-gradient(to bottom right, var(--primary), var(--chart-4))",
                        }}
                    >
                        <BarChart3 className="w-6 h-6 text-white" />
                    </div>
                    <div>
                        <h2 className="text-lg text-foreground">{t("reports.sidebar.title")}</h2>
                        <p className="text-xs text-muted-foreground">{t("reports.sidebar.subtitle")}</p>
                    </div>
                </div>

                <nav className="space-y-1">
                    {SIDEBAR_ITEMS.map((item) => {
                        const Icon = item.icon;
                        const isActive = activeSection === item.id;
                        const label = SECTION_I18N_KEY[item.id]
                            ? t(SECTION_I18N_KEY[item.id])
                            : item.label;
                        return (
                            <button
                                key={item.id}
                                onClick={() => onSectionChange(item.id)}
                                className={`w-full flex items-center gap-3 px-4 py-3 rounded-[18px] text-sm transition-all relative ${isActive
                                        ? "bg-primary/10 text-primary"
                                        : "text-muted-foreground hover:bg-muted"
                                    }`}
                            >
                                {isActive && (
                                    <div className="absolute left-0 top-0 bottom-0 w-1 bg-primary rounded-r-full" />
                                )}
                                <Icon className="w-5 h-5" />
                                <span>{label}</span>
                            </button>
                        );
                    })}
                </nav>
            </div>
        </aside>
    );
}
