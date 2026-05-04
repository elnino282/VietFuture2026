package org.example.QuanLyMuaVu.module.financial.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.DTO.Common.ApiResponse;
import org.example.QuanLyMuaVu.DTO.Common.PageResponse;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.module.financial.dto.request.CreateExpenseRequest;
import org.example.QuanLyMuaVu.module.financial.dto.request.ExpenseSearchCriteria;
import org.example.QuanLyMuaVu.module.financial.dto.request.UpdateExpenseRequest;
import org.example.QuanLyMuaVu.module.financial.dto.response.ExpenseResponse;
import org.example.QuanLyMuaVu.module.financial.service.SeasonExpenseService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * BR174-BR187: Expense Management REST Controller.
 * 
 * Endpoints for managing season-level expenses with full Business Rule
 * compliance:
 * - BR174: Display Expense Creation screen
 * - BR175: Validate input data format
 * - BR176: CreateExpense with constraint validation
 * - BR177: Query expense by ID
 * - BR178: Display Expense Update screen
 * - BR179: Validate update input format
 * - BR180: UpdateExpense with constraint validation
 * - BR181-BR182: Delete confirmation flow
 * - BR183: DeleteExpense with constraint check
 * - BR184-BR187: Search expenses with criteria
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@PreAuthorize("hasRole('FARMER')")
public class SeasonExpenseController {

        SeasonExpenseService seasonExpenseService;

        // ═══════════════════════════════════════════════════════════════════════════
        // BR176: CreateExpense(Expense expense) - Create Expense with Validation
        // ═══════════════════════════════════════════════════════════════════════════

        @Operation(summary = "BR176: Create expense for season", description = "Creates a new expense linked to a season. "
                        +
                        "Validates: season belongs to plot, task belongs to season, amount > 0. " +
                        "Returns MSG 7 on success, MSG 9 on constraint violation.")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "MSG 7: Save data successful."),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "MSG 1/MSG 4/MSG 9: Validation error or constraint violation"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Season not found")
        })
        @PostMapping("/seasons/{seasonId}/expenses")
        public ApiResponse<ExpenseResponse> createExpense(
                        @Parameter(description = "[cmbSeasonID] - Season ID from path") @PathVariable Integer seasonId,
                        @Parameter(description = "Expense data from form fields") @Valid @RequestBody CreateExpenseRequest request) {

                ExpenseResponse response = seasonExpenseService.CreateExpense(seasonId, request);

                // BR176: Step (7) - Return MSG 7: "Save data successful."
                return ApiResponse.success(ErrorCode.MSG_7_SAVE_SUCCESS.getMessage(), response);
        }

        // ═══════════════════════════════════════════════════════════════════════════
        // BR177: Query expense - SELECT * FROM expense WHERE expense_id = [ID]
        // ═══════════════════════════════════════════════════════════════════════════

        @Operation(summary = "BR177: Get expense by ID", description = "Queries expense by ID for display on update screen. "
                        +
                        "Used by displayExpenseUpdateScreen(Expense expense) method.")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Expense found and returned"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "MSG 10: Expense not found.")
        })
        @GetMapping("/expenses/{id}")
        public ApiResponse<ExpenseResponse> getExpense(
                        @Parameter(description = "[Expense.ID] from datagrid selection") @PathVariable Integer id) {
                return ApiResponse.success(seasonExpenseService.getExpense(id));
        }

        // ═══════════════════════════════════════════════════════════════════════════
        // BR180: UpdateExpense(Expense expense) - Update Expense with Validation
        // ═══════════════════════════════════════════════════════════════════════════

        @Operation(summary = "BR180: Update expense", description = "Updates expense information. " +
                        "Validates: season belongs to plot, task belongs to season, amount > 0. " +
                        "Returns MSG 7 on success, MSG 9 on constraint violation.")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "MSG 7: Save data successful."),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "MSG 1/MSG 4/MSG 9: Validation error or constraint violation"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "MSG 10: Expense not found.")
        })
        @PutMapping("/expenses/{id}")
        public ApiResponse<ExpenseResponse> updateExpense(
                        @Parameter(description = "Expense ID to update") @PathVariable Integer id,
                        @Parameter(description = "Updated expense data from form fields") @Valid @RequestBody UpdateExpenseRequest request) {

                ExpenseResponse response = seasonExpenseService.UpdateExpense(id, request);

                // BR180: Step (8) - Return MSG 7: "Save data successful."
                return ApiResponse.success(ErrorCode.MSG_7_SAVE_SUCCESS.getMessage(), response);
        }

        // ═══════════════════════════════════════════════════════════════════════════
        // BR181-BR183: DeleteExpense with Confirmation
        // ═══════════════════════════════════════════════════════════════════════════

        @Operation(summary = "BR181: Get delete confirmation message", description = "Returns MSG 11 confirmation message for delete dialog. "
                        +
                        "Frontend should display confirmation before calling DELETE.")
        @GetMapping("/expenses/{id}/delete-confirmation")
        public ApiResponse<String> getDeleteConfirmation(
                        @Parameter(description = "Expense ID to delete") @PathVariable Integer id) {

                // BR181: Return MSG 11: "Are you sure you want to delete this expense?"
                return ApiResponse.success(ErrorCode.MSG_11_CONFIRMATION.getMessage(),
                                ErrorCode.MSG_11_CONFIRMATION.getMessage());
        }

        @Operation(summary = "BR182/BR183: Delete expense after confirmation", description = "Deletes expense after user confirms. "
                        +
                        "Validates constraints before deletion. " +
                        "Returns MSG 7 on success, MSG 9 on constraint violation.")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "MSG 7: Save data successful. (Expense deleted)"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "MSG 9: Constraint violation"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "MSG 10: Expense not found.")
        })
        @DeleteMapping("/expenses/{id}")
        public ApiResponse<Void> deleteExpense(
                        @Parameter(description = "Expense ID to delete") @PathVariable Integer id) {

                seasonExpenseService.DeleteExpense(id);

                // BR183: Step (7) - Return MSG 7: "Save data successful."
                return ApiResponse.success(ErrorCode.MSG_7_SAVE_SUCCESS.getMessage(), null);
        }

        // ═══════════════════════════════════════════════════════════════════════════
        // BR185-BR187: SearchExpense(ExpenseSearchCriteria criteria)
        // ═══════════════════════════════════════════════════════════════════════════

        @Operation(summary = "BR185: Search expenses by criteria", description = "Searches expenses using criteria from search form. "
                        +
                        "Returns BR186 results if found, BR187 empty result if not found.")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "BR186: Search results returned (may be empty for BR187)")
        })
        @GetMapping("/expenses/search")
        public ApiResponse<PageResponse<ExpenseResponse>> searchExpenses(
                        @Parameter(description = "[cmbSeasonID] - Filter by season ID") @RequestParam(value = "seasonId", required = false) Integer seasonId,
                        @Parameter(description = "[cmbPlotID] - Filter by plot ID") @RequestParam(value = "plotId", required = false) Integer plotId,
                        @Parameter(description = "[cmbTaskID] - Filter by task ID") @RequestParam(value = "taskId", required = false) Integer taskId,
                        @Parameter(description = "[cmbCategory] - Filter by category") @RequestParam(value = "category", required = false) String category,
                        @Parameter(description = "From date (yyyy-MM-dd)") @RequestParam(value = "fromDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                        @Parameter(description = "To date (yyyy-MM-dd)") @RequestParam(value = "toDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
                        @Parameter(description = "Minimum amount") @RequestParam(value = "minAmount", required = false) BigDecimal minAmount,
                        @Parameter(description = "Maximum amount") @RequestParam(value = "maxAmount", required = false) BigDecimal maxAmount,
                        @Parameter(description = "Search keyword for item name") @RequestParam(value = "q", required = false) String keyword,
                        @Parameter(description = "Page index (0-based)") @RequestParam(value = "page", defaultValue = "0") int page,
                        @Parameter(description = "Page size") @RequestParam(value = "size", defaultValue = "20") int size) {

                ExpenseSearchCriteria criteria = ExpenseSearchCriteria.builder()
                                .seasonId(seasonId)
                                .plotId(plotId)
                                .taskId(taskId)
                                .category(category)
                                .fromDate(fromDate)
                                .toDate(toDate)
                                .minAmount(minAmount)
                                .maxAmount(maxAmount)
                                .keyword(keyword)
                                .build();

                PageResponse<ExpenseResponse> result = seasonExpenseService.SearchExpense(criteria, page, size);

                // BR187: If no results found, return with MSG 10 message
                if (result.getItems().isEmpty()) {
                        return ApiResponse.success(ErrorCode.MSG_10_EXPENSE_NOT_FOUND.getMessage(), result);
                }

                // BR186: Return search results
                return ApiResponse.success(result);
        }

        // ═══════════════════════════════════════════════════════════════════════════
        // ADDITIONAL ENDPOINTS (For existing functionality compatibility)
        // ═══════════════════════════════════════════════════════════════════════════

        @Operation(summary = "List expenses of a season", description = "Search expenses for a given season of the current farmer")
        @GetMapping("/seasons/{seasonId}/expenses")
        public ApiResponse<PageResponse<ExpenseResponse>> listExpenses(
                        @PathVariable Integer seasonId,
                        @Parameter(description = "From date (yyyy-MM-dd)") @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                        @Parameter(description = "To date (yyyy-MM-dd)") @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                        @RequestParam(value = "minAmount", required = false) BigDecimal minAmount,
                        @RequestParam(value = "maxAmount", required = false) BigDecimal maxAmount,
                        @Parameter(description = "Page index (0-based)") @RequestParam(value = "page", defaultValue = "0") int page,
                        @Parameter(description = "Page size") @RequestParam(value = "size", defaultValue = "20") int size) {
                return ApiResponse.success(
                                seasonExpenseService.listExpensesForSeason(seasonId, from, to, minAmount, maxAmount,
                                                page, size));
        }
}
