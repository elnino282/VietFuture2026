package org.example.QuanLyMuaVu.module.ai.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.Enums.StockMovementType;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.module.admin.service.AuditLogService;
import org.example.QuanLyMuaVu.module.ai.dto.request.SeasonCostOptimizationSuggestionRequest;
import org.example.QuanLyMuaVu.module.ai.dto.response.SeasonCostCategoryBreakdown;
import org.example.QuanLyMuaVu.module.ai.dto.response.SeasonCostOptimizationSuggestionResponse;
import org.example.QuanLyMuaVu.module.ai.dto.response.SeasonCostOptimizationSummaryResponse;
import org.example.QuanLyMuaVu.module.ai.dto.response.SeasonInventoryUsageSummary;
import org.example.QuanLyMuaVu.module.farm.port.FarmAccessPort;
import org.example.QuanLyMuaVu.module.financial.entity.Expense;
import org.example.QuanLyMuaVu.module.financial.repository.ExpenseRepository;
import org.example.QuanLyMuaVu.module.inventory.entity.StockMovement;
import org.example.QuanLyMuaVu.module.inventory.repository.StockMovementRepository;
import org.example.QuanLyMuaVu.module.season.entity.DiseaseTreatment;
import org.example.QuanLyMuaVu.module.season.entity.Season;
import org.example.QuanLyMuaVu.module.season.repository.DiseaseTreatmentRepository;
import org.example.QuanLyMuaVu.module.season.repository.HarvestRepository;
import org.example.QuanLyMuaVu.module.season.repository.PayrollRecordRepository;
import org.example.QuanLyMuaVu.module.season.repository.SeasonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class SeasonCostOptimizationService {

    static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    static final BigDecimal LOW_REMAINING_BUDGET_RATIO = new BigDecimal("0.10");
    static final BigDecimal HIGH_CATEGORY_SHARE_RATIO = new BigDecimal("0.40");
    static final BigDecimal HIGH_TREATMENT_SHARE_RATIO = new BigDecimal("0.25");
    static final int TOP_CATEGORY_LIMIT = 3;
    static final int MAX_INVENTORY_USAGE_ROWS = 12;
    static final int MAX_TEXT_LENGTH = 280;
    static final String DEFAULT_QUESTION_VI =
            "Hay giai thich tinh hinh chi phi mua vu nay va goi y toi uu chi phi tham khao.";
    static final String DEFAULT_QUESTION_EN =
            "Please explain this season's cost situation and suggest reference cost optimization actions.";
    static final String DISCLAIMER_MESSAGE_VI =
            "AI chi ho tro quyet dinh tham khao, khong thay the tu van tai chinh/chuyen gia nong nghiep va khong tu dong sua expense, budget, inventory.";
    static final String DISCLAIMER_MESSAGE_EN =
            "AI provides reference-only guidance, does not replace financial/agronomy experts, and does not auto-edit expenses, budget, or inventory.";
    static final String NOT_AVAILABLE_TEXT = "N/A";

    GeminiService geminiService;
    SeasonRepository seasonRepository;
    ExpenseRepository expenseRepository;
    HarvestRepository harvestRepository;
    PayrollRecordRepository payrollRecordRepository;
    DiseaseTreatmentRepository diseaseTreatmentRepository;
    StockMovementRepository stockMovementRepository;
    FarmAccessPort farmAccessService;
    AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public SeasonCostOptimizationSummaryResponse getSummary(Integer seasonId) {
        return getSummary(seasonId, null);
    }

    @Transactional(readOnly = true)
    public SeasonCostOptimizationSummaryResponse getSummary(Integer seasonId, String locale) {
        boolean vietnamese = isVietnameseLocale(locale);
        CostSummaryContext context = buildCostSummaryContext(seasonId, true, vietnamese);
        return context.summary();
    }

    public SeasonCostOptimizationSuggestionResponse generateSuggestion(
            Integer seasonId,
            SeasonCostOptimizationSuggestionRequest request) {
        return generateSuggestion(seasonId, request, null);
    }

    public SeasonCostOptimizationSuggestionResponse generateSuggestion(
            Integer seasonId,
            SeasonCostOptimizationSuggestionRequest request,
            String locale) {
        String resolvedLocale = normalizeLocale(
                request != null ? request.getLocale() : null,
                locale);
        boolean vietnamese = isVietnameseLocale(resolvedLocale);
        boolean includeInventory = request == null
                || request.getIncludeInventory() == null
                || request.getIncludeInventory();
        String question = normalizeText(request != null ? request.getQuestion() : null);
        if (!StringUtils.hasText(question)) {
            question = getDefaultQuestion(vietnamese);
        }
        String additionalNote = normalizeText(request != null ? request.getAdditionalNote() : null);

        CostSummaryContext context = buildCostSummaryContext(seasonId, includeInventory, vietnamese);
        SeasonCostOptimizationSummaryResponse summary = context.summary();

        String instruction = buildInstruction(
                question,
                includeInventory,
                summary.getInventoryUsageSummary() == null || summary.getInventoryUsageSummary().isEmpty(),
                vietnamese);
        String structuredContext = buildStructuredContext(summary, additionalNote, vietnamese);
        String suggestionText = geminiService.chatAsAgriculturalExpert(instruction, structuredContext);
        LocalDateTime generatedAt = LocalDateTime.now();

        Map<String, Object> usedContextSummary = buildUsedContextSummary(
                context,
                includeInventory,
                question,
                additionalNote,
                generatedAt);
        logSuggestionAudit(summary.getSeasonId(), usedContextSummary, getDisclaimerMessage(vietnamese));

        return SeasonCostOptimizationSuggestionResponse.builder()
                .seasonId(summary.getSeasonId())
                .seasonName(summary.getSeasonName())
                .budgetAmount(summary.getBudgetAmount())
                .totalExpense(summary.getTotalExpense())
                .remainingBudget(summary.getRemainingBudget())
                .expenseByCategory(summary.getExpenseByCategory())
                .topCostCategories(summary.getTopCostCategories())
                .expectedYieldKg(summary.getExpectedYieldKg())
                .actualYieldKg(summary.getActualYieldKg())
                .costPerExpectedKg(summary.getCostPerExpectedKg())
                .costPerActualKg(summary.getCostPerActualKg())
                .laborCost(summary.getLaborCost())
                .pesticideTreatmentCost(summary.getPesticideTreatmentCost())
                .inventoryUsageSummary(summary.getInventoryUsageSummary())
                .warnings(summary.getWarnings())
                .aiSuggestionText(suggestionText)
                .usedContextSummary(usedContextSummary)
                .generatedAt(generatedAt)
                .disclaimer(getDisclaimerMessage(vietnamese))
                .build();
    }

    private CostSummaryContext buildCostSummaryContext(Integer seasonId, boolean includeInventory, boolean vietnamese) {
        if (seasonId == null) {
            throw new AppException(ErrorCode.KEY_INVALID);
        }

        Season season = seasonRepository.findById(seasonId)
                .orElseThrow(() -> new AppException(ErrorCode.SEASON_NOT_FOUND));
        farmAccessService.assertCurrentUserCanAccessSeason(season);

        List<Expense> expenses = expenseRepository.findAllBySeasonId(seasonId);
        BigDecimal totalExpense = sumExpenseAmount(expenses);

        BigDecimal budgetAmount = normalize(season.getBudgetAmount());
        BigDecimal remainingBudget = budgetAmount.subtract(totalExpense);

        List<SeasonCostCategoryBreakdown> expenseByCategory = buildExpenseByCategory(expenses, totalExpense);
        List<SeasonCostCategoryBreakdown> topCostCategories = expenseByCategory.stream()
                .limit(TOP_CATEGORY_LIMIT)
                .toList();

        BigDecimal expectedYieldKg = normalizeNullablePositive(season.getExpectedYieldKg());
        BigDecimal actualYieldKg = normalizeNullablePositive(season.getActualYieldKg());
        if (actualYieldKg == null) {
            actualYieldKg = normalizeNullablePositive(harvestRepository.sumQuantityBySeasonId(seasonId));
        }

        BigDecimal costPerExpectedKg = divideNullable(totalExpense, expectedYieldKg);
        BigDecimal costPerActualKg = divideNullable(totalExpense, actualYieldKg);

        BigDecimal payrollCost = normalize(payrollRecordRepository.sumTotalAmountBySeasonId(seasonId));
        BigDecimal laborExpenseCost = sumLaborExpenseCost(expenses);
        BigDecimal laborCost = payrollCost.add(laborExpenseCost);

        List<DiseaseTreatment> treatments = diseaseTreatmentRepository.findAllBySeasonIdWithExpense(seasonId);
        BigDecimal pesticideTreatmentCost = sumTreatmentCost(treatments);

        List<StockMovement> inventoryMovements = includeInventory
                ? stockMovementRepository.findAllBySeasonIdWithLotAndItem(seasonId)
                : List.of();
        List<SeasonInventoryUsageSummary> inventoryUsageSummary = includeInventory
                ? buildInventoryUsageSummary(inventoryMovements)
                : List.of();

        List<String> warnings = buildWarnings(
                budgetAmount,
                totalExpense,
                remainingBudget,
                topCostCategories,
                pesticideTreatmentCost,
                expectedYieldKg,
                vietnamese);

        SeasonCostOptimizationSummaryResponse summary = SeasonCostOptimizationSummaryResponse.builder()
                .seasonId(season.getId())
                .seasonName(season.getSeasonName())
                .budgetAmount(budgetAmount)
                .totalExpense(totalExpense)
                .remainingBudget(remainingBudget)
                .expenseByCategory(expenseByCategory)
                .topCostCategories(topCostCategories)
                .expectedYieldKg(expectedYieldKg)
                .actualYieldKg(actualYieldKg)
                .costPerExpectedKg(costPerExpectedKg)
                .costPerActualKg(costPerActualKg)
                .laborCost(laborCost)
                .pesticideTreatmentCost(pesticideTreatmentCost)
                .inventoryUsageSummary(inventoryUsageSummary)
                .warnings(warnings)
                .disclaimer(getDisclaimerMessage(vietnamese))
                .build();

        return new CostSummaryContext(summary, expenses, treatments, inventoryMovements);
    }

    private BigDecimal sumExpenseAmount(List<Expense> expenses) {
        BigDecimal total = BigDecimal.ZERO;
        for (Expense expense : expenses) {
            total = total.add(normalize(expense.getEffectiveAmount()));
        }
        return total;
    }

    private List<SeasonCostCategoryBreakdown> buildExpenseByCategory(List<Expense> expenses, BigDecimal totalExpense) {
        Map<String, BigDecimal> categoryTotals = new HashMap<>();
        for (Expense expense : expenses) {
            String category = normalizeCategory(expense.getCategory());
            BigDecimal amount = normalize(expense.getEffectiveAmount());
            categoryTotals.merge(category, amount, BigDecimal::add);
        }

        return categoryTotals.entrySet().stream()
                .map(entry -> SeasonCostCategoryBreakdown.builder()
                        .category(entry.getKey())
                        .amount(entry.getValue())
                        .percentageOfTotal(calculatePercentage(entry.getValue(), totalExpense))
                        .build())
                .sorted(Comparator
                        .comparing(SeasonCostCategoryBreakdown::getAmount, Comparator.nullsLast(BigDecimal::compareTo))
                        .reversed()
                        .thenComparing(SeasonCostCategoryBreakdown::getCategory, Comparator.nullsLast(String::compareTo)))
                .toList();
    }

    private BigDecimal sumLaborExpenseCost(List<Expense> expenses) {
        BigDecimal total = BigDecimal.ZERO;
        for (Expense expense : expenses) {
            if (isLaborCategory(expense.getCategory())) {
                total = total.add(normalize(expense.getEffectiveAmount()));
            }
        }
        return total;
    }

    private BigDecimal sumTreatmentCost(List<DiseaseTreatment> treatments) {
        BigDecimal total = BigDecimal.ZERO;
        Set<Integer> linkedExpenseIds = new HashSet<>();
        for (DiseaseTreatment treatment : treatments) {
            if (treatment.getCostAmount() != null) {
                total = total.add(treatment.getCostAmount());
                continue;
            }
            if (treatment.getExpense() == null || treatment.getExpense().getId() == null) {
                continue;
            }
            Integer expenseId = treatment.getExpense().getId();
            if (linkedExpenseIds.add(expenseId)) {
                total = total.add(normalize(treatment.getExpense().getEffectiveAmount()));
            }
        }
        return total;
    }

    private List<SeasonInventoryUsageSummary> buildInventoryUsageSummary(List<StockMovement> movements) {
        Map<String, UsageAggregate> aggregates = new HashMap<>();
        for (StockMovement movement : movements) {
            if (movement.getMovementType() != StockMovementType.OUT) {
                continue;
            }

            String itemName = "N/A";
            String unit = "N/A";
            if (movement.getSupplyLot() != null && movement.getSupplyLot().getSupplyItem() != null) {
                itemName = safeText(movement.getSupplyLot().getSupplyItem().getName());
                unit = safeText(movement.getSupplyLot().getSupplyItem().getUnit());
            }
            final String itemNameValue = itemName;
            final String unitValue = unit;
            String key = itemNameValue + "||" + unitValue;

            UsageAggregate aggregate = aggregates.computeIfAbsent(
                    key,
                    ignored -> new UsageAggregate(itemNameValue, unitValue));
            aggregate.totalOutQuantity = aggregate.totalOutQuantity.add(normalize(movement.getQuantity()));
            aggregate.movementCount += 1;
        }

        return aggregates.values().stream()
                .map(aggregate -> SeasonInventoryUsageSummary.builder()
                        .itemName(aggregate.itemName)
                        .unit(aggregate.unit)
                        .totalOutQuantity(aggregate.totalOutQuantity)
                        .movementCount(aggregate.movementCount)
                        .build())
                .sorted((left, right) -> {
                    int byQuantity = compareNullableBigDecimalDesc(left.getTotalOutQuantity(), right.getTotalOutQuantity());
                    if (byQuantity != 0) {
                        return byQuantity;
                    }

                    int byCount = compareNullableIntegerDesc(left.getMovementCount(), right.getMovementCount());
                    if (byCount != 0) {
                        return byCount;
                    }

                    return compareNullableStringAsc(left.getItemName(), right.getItemName());
                })
                .limit(MAX_INVENTORY_USAGE_ROWS)
                .toList();
    }

    private List<String> buildWarnings(
            BigDecimal budgetAmount,
            BigDecimal totalExpense,
            BigDecimal remainingBudget,
            List<SeasonCostCategoryBreakdown> topCostCategories,
            BigDecimal treatmentCost,
            BigDecimal expectedYieldKg,
            boolean vietnamese) {
        List<String> warnings = new ArrayList<>();

        if (budgetAmount.compareTo(BigDecimal.ZERO) <= 0) {
            warnings.add(vietnamese
                    ? "Khong co budget hop le, can cap nhat budgetAmount de theo doi vuot ngan sach."
                    : "No valid budget found. Please update budgetAmount to monitor overruns.");
        } else {
            if (totalExpense.compareTo(budgetAmount) > 0) {
                warnings.add(vietnamese
                        ? "Tong chi phi dang vuot ngan sach mua vu."
                        : "Total costs are currently exceeding the seasonal budget.");
            }

            BigDecimal lowRemainingThreshold = budgetAmount.multiply(LOW_REMAINING_BUDGET_RATIO);
            if (remainingBudget.compareTo(BigDecimal.ZERO) >= 0
                    && remainingBudget.compareTo(lowRemainingThreshold) <= 0) {
                warnings.add(vietnamese
                        ? "Ngan sach con lai dang thap (<=10% budget)."
                        : "Remaining budget is low (<=10% of budget).");
            }
        }

        if (!topCostCategories.isEmpty()
                && topCostCategories.getFirst().getPercentageOfTotal() != null
                && topCostCategories.getFirst().getPercentageOfTotal()
                        .compareTo(HIGH_CATEGORY_SHARE_RATIO.multiply(ONE_HUNDRED)) >= 0) {
            warnings.add(vietnamese
                    ? "Mot nhom chi phi dang chiem ty trong rat cao, can ra soat de tranh mat can doi."
                    : "A single cost category is taking a very high share. Review for balance risk.");
        }

        if (totalExpense.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal treatmentRatio = treatmentCost.divide(totalExpense, 4, RoundingMode.HALF_UP);
            if (treatmentRatio.compareTo(HIGH_TREATMENT_SHARE_RATIO) >= 0) {
                warnings.add(vietnamese
                        ? "Chi phi thuoc/dieu tri dang chiem ty le cao trong tong chi phi."
                        : "Treatment/pesticide costs are consuming a high share of total costs.");
            }
        }

        if (expectedYieldKg == null) {
            warnings.add(vietnamese
                    ? "Thieu expectedYieldKg nen chua tinh duoc costPerExpectedKg."
                    : "Missing expectedYieldKg, so costPerExpectedKg cannot be computed yet.");
        }

        return warnings;
    }

    private String buildInstruction(String question, boolean includeInventory, boolean inventoryEmpty, boolean vietnamese) {
        StringBuilder sb = new StringBuilder();
        if (vietnamese) {
            sb.append("Cau hoi cua nguoi dung: ").append(question).append("\n\n");
            sb.append("Ban chi duoc giai thich va goi y dua tren tong hop backend da tinh san.\n");
            sb.append("Khong duoc tu tinh lai so lieu goc neu backend da cung cap.\n\n");
            sb.append("Tra loi dung format:\n");
            sb.append("a. Tom tat buc tranh chi phi mua vu\n");
            sb.append("b. Du lieu con thieu\n");
            sb.append("c. Huong toi uu chi phi tham khao\n");
            sb.append("d. Vat tu su dung hien co co the can nhac\n");
            sb.append("e. Rui ro/canh bao\n");
            sb.append("f. Buoc tiep theo nen ghi nhan tren he thong\n\n");
            sb.append("Rang buoc an toan bat buoc:\n");
            sb.append("- AI chi ho tro quyet dinh, khong thay the tu van tai chinh/chuyen gia nong nghiep.\n");
            sb.append("- Khong tu dong chinh sua expense, budget, inventory hoac quyet dinh mua vat tu.\n");
            sb.append("- Khong khang dinh loi nhuan neu thieu du lieu revenue.\n");
            sb.append("- Neu thieu du lieu, phai noi ro can bo sung thong tin gi.\n");
            if (!includeInventory) {
                sb.append("- Request khong bao gom inventory summary, khong goi y vat tu cu the.\n");
            } else if (inventoryEmpty) {
                sb.append("- Khong co inventory usage summary noi bo, khong goi y mua vat tu tu dong.\n");
            } else {
                sb.append("- Chi goi y vat tu dua tren inventory usage summary noi bo da cung cap.\n");
            }
            sb.append("Bat buoc ket thuc bang dong disclaimer: ");
            sb.append("'Goi y toi uu chi phi chi mang tinh tham khao, vui long tham van chuyen gia truoc khi ap dung.'");
            return sb.toString();
        }

        sb.append("User question: ").append(question).append("\n\n");
        sb.append("You must explain and suggest only from backend-precomputed summaries.\n");
        sb.append("Do not recalculate raw source values when backend already provides them.\n\n");
        sb.append("Return with this exact structure:\n");
        sb.append("a. Seasonal cost overview\n");
        sb.append("b. Missing data\n");
        sb.append("c. Reference cost optimization directions\n");
        sb.append("d. Candidate materials based on current usage\n");
        sb.append("e. Risks/warnings\n");
        sb.append("f. Next steps to record in the system\n\n");
        sb.append("Mandatory safety constraints:\n");
        sb.append("- AI supports decisions and does not replace finance/agronomy experts.\n");
        sb.append("- Do not auto-edit expenses, budget, inventory, or purchase decisions.\n");
        sb.append("- Do not claim profit certainty if revenue data is missing.\n");
        sb.append("- If data is missing, clearly state what additional information is required.\n");
        if (!includeInventory) {
            sb.append("- Request does not include inventory summary, so do not suggest specific materials.\n");
        } else if (inventoryEmpty) {
            sb.append("- No internal inventory usage summary is available; do not auto-suggest purchases.\n");
        } else {
            sb.append("- Only suggest materials that can be grounded in the provided internal inventory usage summary.\n");
        }
        sb.append("Must end with this disclaimer line: ");
        sb.append("'Cost optimization suggestions are reference-only. Consult experts before applying.'");
        return sb.toString();
    }

    private String buildStructuredContext(
            SeasonCostOptimizationSummaryResponse summary,
            String additionalNote,
            boolean vietnamese) {
        StringBuilder sb = new StringBuilder();
        sb.append(vietnamese
                ? "Tom tat chi phi mua vu duoc backend tinh san:\n"
                : "Precomputed seasonal cost summary from backend:\n");
        sb.append("- SeasonId: ").append(summary.getSeasonId()).append("\n");
        sb.append("- SeasonName: ").append(safeText(summary.getSeasonName())).append("\n");
        sb.append("- BudgetAmount: ").append(toPlain(summary.getBudgetAmount())).append("\n");
        sb.append("- TotalExpense: ").append(toPlain(summary.getTotalExpense())).append("\n");
        sb.append("- RemainingBudget: ").append(toPlain(summary.getRemainingBudget())).append("\n");
        sb.append("- ExpectedYieldKg: ").append(toNullablePlain(summary.getExpectedYieldKg())).append("\n");
        sb.append("- ActualYieldKg: ").append(toNullablePlain(summary.getActualYieldKg())).append("\n");
        sb.append("- CostPerExpectedKg: ").append(toNullablePlain(summary.getCostPerExpectedKg())).append("\n");
        sb.append("- CostPerActualKg: ").append(toNullablePlain(summary.getCostPerActualKg())).append("\n");
        sb.append("- LaborCost: ").append(toPlain(summary.getLaborCost())).append("\n");
        sb.append("- PesticideTreatmentCost: ").append(toPlain(summary.getPesticideTreatmentCost())).append("\n\n");

        sb.append(vietnamese ? "Expense by category:\n" : "Expense by category:\n");
        if (summary.getExpenseByCategory() == null || summary.getExpenseByCategory().isEmpty()) {
            sb.append(vietnamese
                    ? "- Chua co du lieu chi phi theo nhom.\n"
                    : "- No grouped expense data is currently available.\n");
        } else {
            for (SeasonCostCategoryBreakdown category : summary.getExpenseByCategory()) {
                sb.append("- ")
                        .append(safeText(category.getCategory()))
                        .append(": amount=").append(toPlain(category.getAmount()))
                        .append(", share=").append(toNullablePlain(category.getPercentageOfTotal())).append("%\n");
            }
        }
        sb.append("\n");

        sb.append(vietnamese ? "Inventory usage summary:\n" : "Inventory usage summary:\n");
        if (summary.getInventoryUsageSummary() == null || summary.getInventoryUsageSummary().isEmpty()) {
            sb.append(vietnamese
                    ? "- Chua co du lieu su dung vat tu theo stock movement.\n"
                    : "- No inventory usage data from stock movements is available.\n");
        } else {
            for (SeasonInventoryUsageSummary row : summary.getInventoryUsageSummary()) {
                sb.append("- item=").append(safeText(row.getItemName()))
                        .append(", outQty=").append(toPlain(row.getTotalOutQuantity())).append(" ")
                        .append(safeText(row.getUnit()))
                        .append(", movementCount=").append(row.getMovementCount())
                        .append("\n");
            }
        }
        sb.append("\n");

        sb.append(vietnamese ? "Warnings hien tai:\n" : "Current warnings:\n");
        if (summary.getWarnings() == null || summary.getWarnings().isEmpty()) {
            sb.append(vietnamese
                    ? "- Khong co canh bao nghiem trong tu bo rule hien tai.\n"
                    : "- No severe warnings from the current rule set.\n");
        } else {
            for (String warning : summary.getWarnings()) {
                sb.append("- ").append(safeText(warning)).append("\n");
            }
        }
        sb.append("\n");

        if (StringUtils.hasText(additionalNote)) {
            sb.append(vietnamese
                    ? "Ghi chu bo sung tu nguoi dung:\n"
                    : "Additional user note:\n");
            sb.append("- ").append(safeText(additionalNote)).append("\n\n");
        }

        sb.append(vietnamese
                ? "Chi dung du lieu tren, khong suy dien them du lieu ngoai he thong."
                : "Use only the data above. Do not infer extra data outside the system.");
        return sb.toString();
    }

    private Map<String, Object> buildUsedContextSummary(
            CostSummaryContext context,
            boolean includeInventory,
            String question,
            String additionalNote,
            LocalDateTime generatedAt) {
        SeasonCostOptimizationSummaryResponse summary = context.summary();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("seasonId", summary.getSeasonId());
        result.put("seasonName", summary.getSeasonName());
        result.put("expenseRows", context.expenses().size());
        result.put("categoryRows", summary.getExpenseByCategory() != null ? summary.getExpenseByCategory().size() : 0);
        result.put("treatmentRows", context.treatments().size());
        result.put("inventoryMovementRows", context.inventoryMovements().size());
        result.put("inventoryUsageRows",
                summary.getInventoryUsageSummary() != null ? summary.getInventoryUsageSummary().size() : 0);
        result.put("warningCount", summary.getWarnings() != null ? summary.getWarnings().size() : 0);
        result.put("includeInventory", includeInventory);
        result.put("hasQuestion", StringUtils.hasText(question));
        result.put("hasAdditionalNote", StringUtils.hasText(additionalNote));
        result.put("generatedAt", generatedAt);
        return result;
    }

    private void logSuggestionAudit(
            Integer seasonId,
            Map<String, Object> usedContextSummary,
            String disclaimerMessage) {
        Map<String, Object> snapshot = new LinkedHashMap<>(usedContextSummary);
        snapshot.put("disclaimer", disclaimerMessage);

        auditLogService.logModuleOperation(
                "AI",
                "SEASON_COST_OPTIMIZATION",
                seasonId,
                "AI_SEASON_COST_OPTIMIZATION_SUGGESTION_REQUESTED",
                resolveAuditActor(),
                snapshot,
                null,
                null);
    }

    private String resolveAuditActor() {
        try {
            org.example.QuanLyMuaVu.module.identity.entity.User actor = farmAccessService.getCurrentUser();
            if (actor != null && StringUtils.hasText(actor.getUsername())) {
                return actor.getUsername();
            }
        } catch (Exception ignored) {
            // Keep fallback actor.
        }
        return "system";
    }

    private String normalizeCategory(String category) {
        if (!StringUtils.hasText(category)) {
            return "UNCATEGORIZED";
        }
        return category.trim().toUpperCase();
    }

    private boolean isLaborCategory(String category) {
        if (!StringUtils.hasText(category)) {
            return false;
        }
        String normalized = category.trim().toUpperCase();
        return normalized.contains("LABOR")
                || normalized.contains("LABOUR")
                || normalized.contains("PAYROLL")
                || normalized.contains("NHAN_CONG");
    }

    private BigDecimal calculatePercentage(BigDecimal value, BigDecimal total) {
        if (value == null || total == null || total.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return value.multiply(ONE_HUNDRED).divide(total, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal divideNullable(BigDecimal numerator, BigDecimal denominator) {
        if (numerator == null || denominator == null || denominator.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return numerator.divide(denominator, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal normalize(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private BigDecimal normalizeNullablePositive(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return value;
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String safeText(String value) {
        String normalized = normalizeText(value);
        if (!StringUtils.hasText(normalized)) {
            return NOT_AVAILABLE_TEXT;
        }
        if (normalized.length() <= MAX_TEXT_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, MAX_TEXT_LENGTH) + "...";
    }

    private String toPlain(BigDecimal value) {
        return normalize(value).stripTrailingZeros().toPlainString();
    }

    private String toNullablePlain(BigDecimal value) {
        if (value == null) {
            return NOT_AVAILABLE_TEXT;
        }
        return value.stripTrailingZeros().toPlainString();
    }

    private String normalizeLocale(String primaryLocale, String fallbackLocale) {
        String normalizedPrimary = normalizeText(primaryLocale);
        if (StringUtils.hasText(normalizedPrimary)) {
            return normalizedPrimary;
        }
        return normalizeText(fallbackLocale);
    }

    private boolean isVietnameseLocale(String locale) {
        if (!StringUtils.hasText(locale)) {
            return false;
        }
        String normalized = locale.trim().toLowerCase();
        return normalized.equals("vi") || normalized.startsWith("vi-");
    }

    private String getDefaultQuestion(boolean vietnamese) {
        return vietnamese ? DEFAULT_QUESTION_VI : DEFAULT_QUESTION_EN;
    }

    private String getDisclaimerMessage(boolean vietnamese) {
        return vietnamese ? DISCLAIMER_MESSAGE_VI : DISCLAIMER_MESSAGE_EN;
    }

    private int compareNullableBigDecimalDesc(BigDecimal left, BigDecimal right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return 1;
        }
        if (right == null) {
            return -1;
        }
        return right.compareTo(left);
    }

    private int compareNullableIntegerDesc(Integer left, Integer right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return 1;
        }
        if (right == null) {
            return -1;
        }
        return Integer.compare(right, left);
    }

    private int compareNullableStringAsc(String left, String right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return 1;
        }
        if (right == null) {
            return -1;
        }
        return left.compareTo(right);
    }

    private record CostSummaryContext(
            SeasonCostOptimizationSummaryResponse summary,
            List<Expense> expenses,
            List<DiseaseTreatment> treatments,
            List<StockMovement> inventoryMovements) {
    }

    private static class UsageAggregate {
        final String itemName;
        final String unit;
        BigDecimal totalOutQuantity = BigDecimal.ZERO;
        int movementCount = 0;

        private UsageAggregate(String itemName, String unit) {
            this.itemName = itemName;
            this.unit = unit;
        }
    }
}
