package org.example.QuanLyMuaVu.Exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

        // Generic / common
        UNCATEGORIZED_EXCEPTION("ERR_UNCATEGORIZED_EXCEPTION", "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
        KEY_INVALID("ERR_KEY_INVALID", "Invalid key", HttpStatus.BAD_REQUEST),

        INTERNAL_SERVER_ERROR("ERR_INTERNAL_SERVER_ERROR", "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR),
        FIREBASE_CHAT_UNAVAILABLE(
                        "FIREBASE_CHAT_UNAVAILABLE",
                        "Firebase Chat token service is not configured.",
                        HttpStatus.SERVICE_UNAVAILABLE),
        BAD_REQUEST("ERR_BAD_REQUEST", "Bad request", HttpStatus.BAD_REQUEST),
        LEGACY_NUTRIENT_INPUT_DEPRECATED(
                "ERR_LEGACY_NUTRIENT_INPUT_DEPRECATED",
                "Legacy nutrient input source is deprecated. Use dedicated irrigation-water-analysis or soil-test endpoints.",
                HttpStatus.BAD_REQUEST),
        UNAUTHORIZED("ERR_UNAUTHORIZED", "Unauthorized", HttpStatus.UNAUTHORIZED),
        FORBIDDEN("ERR_FORBIDDEN", "Forbidden", HttpStatus.FORBIDDEN),
        RESOURCE_NOT_FOUND("ERR_RESOURCE_NOT_FOUND", "Resource not found", HttpStatus.NOT_FOUND),
        DUPLICATE_RESOURCE("ERR_DUPLICATE_RESOURCE", "Resource already exists", HttpStatus.CONFLICT),
        UNAUTHENTICATED("ERR_UNAUTHENTICATED", "Unauthenticated", HttpStatus.UNAUTHORIZED),

        // User errors
        USERNAME_BLANK("ERR_USERNAME_BLANK", "Username must not be blank", HttpStatus.BAD_REQUEST),
        PASSWORD_BLANK("ERR_PASSWORD_BLANK", "Password must not be blank", HttpStatus.BAD_REQUEST),
        PASSWORD_INVALID("ERR_PASSWORD_INVALID", "Password must be at least 8 characters", HttpStatus.BAD_REQUEST),
        PASSWORD_MISMATCH("ERR_PASSWORD_MISMATCH", "Password confirmation does not match", HttpStatus.BAD_REQUEST),
        PASSWORD_RESET_TOKEN_INVALID("ERR_PASSWORD_RESET_TOKEN_INVALID",
                        "Reset token is invalid or expired", HttpStatus.BAD_REQUEST),

        USER_NOT_FOUND("ERR_USER_NOT_FOUND", "User not found", HttpStatus.NOT_FOUND),
        USER_EXISTED("ERR_USER_EXISTED", "User already exists", HttpStatus.BAD_REQUEST),
        USER_NOT_EXISTED("ERR_USER_NOT_EXISTED", "User does not exist", HttpStatus.NOT_FOUND),
        USERNAME_ALREADY_EXISTS("ERR_USERNAME_ALREADY_EXISTS", "Username is already in use", HttpStatus.CONFLICT),
        EMAIL_ALREADY_EXISTS("ERR_EMAIL_ALREADY_EXISTS", "Email is already in use", HttpStatus.CONFLICT),
        INVALID_CREDENTIALS("INVALID_CREDENTIALS", "Invalid username/email or password.", HttpStatus.UNAUTHORIZED),
        USER_INACTIVE("ERR_USER_INACTIVE", "Your account has been deactivated. Please contact administrator.", HttpStatus.FORBIDDEN),
        USER_LOCKED("USER_LOCKED", "Your account has been locked by administrator. Please contact support for assistance.", HttpStatus.FORBIDDEN),
        USER_HAS_ASSOCIATED_DATA("ERR_USER_HAS_ASSOCIATED_DATA", "Cannot delete user with associated farms or data",
                        HttpStatus.CONFLICT),
        ROLE_MISSING("ROLE_MISSING", "User has no assigned role.", HttpStatus.FORBIDDEN),
        NOT_OWNER("NOT_OWNER", "Access denied: you do not own this resource.", HttpStatus.FORBIDDEN),
        IDENTIFIER_REQUIRED("IDENTIFIER_REQUIRED", "Email or username is required for login.", HttpStatus.BAD_REQUEST),

        // Farm / Plot errors
        PLOT_NOT_FOUND("ERR_PLOT_NOT_FOUND", "Plot not found", HttpStatus.NOT_FOUND),
        PLOT_NAME_EXISTS("ERR_PLOT_NAME_EXISTS", "Plot name already exists", HttpStatus.CONFLICT),
        INVALID_PLOT_AREA("ERR_INVALID_PLOT_AREA", "Plot area must be greater than 0", HttpStatus.BAD_REQUEST),
        PLOT_STATUS_NOT_FOUND("ERR_PLOT_STATUS_NOT_FOUND", "Plot status not found", HttpStatus.NOT_FOUND),

        FARM_NOT_FOUND("ERR_FARM_NOT_FOUND", "Farm not found", HttpStatus.NOT_FOUND),
        FARM_NAME_EXISTS("ERR_FARM_NAME_EXISTS", "Farm name already exists", HttpStatus.CONFLICT),
        FARM_HAS_CHILD_RECORDS("ERR_FARM_HAS_CHILD_RECORDS", "Cannot delete farm with related plots or seasons",
                        HttpStatus.BAD_REQUEST),
        FARM_ALREADY_INACTIVE("ERR_FARM_ALREADY_INACTIVE", "Farm is already inactive", HttpStatus.BAD_REQUEST),
        FARM_ALREADY_ACTIVE("ERR_FARM_ALREADY_ACTIVE", "Farm is already active", HttpStatus.BAD_REQUEST),
        FARM_CANNOT_RESTORE("ERR_FARM_CANNOT_RESTORE", "Cannot restore farm: foreign key constraints violated",
                        HttpStatus.BAD_REQUEST),
        FARM_CANNOT_HARD_DELETE("ERR_FARM_CANNOT_HARD_DELETE",
                        "Cannot permanently delete farm with related data", HttpStatus.BAD_REQUEST),
        FARM_OWNER_INACTIVE("ERR_FARM_OWNER_INACTIVE", "Cannot restore farm: owner account is inactive",
                        HttpStatus.BAD_REQUEST),
        PLOT_HAS_ACTIVE_SEASONS("ERR_PLOT_HAS_ACTIVE_SEASONS",
                        "Cannot delete plot because it has active or planned seasons", HttpStatus.BAD_REQUEST),
        FARM_INACTIVE("ERR_FARM_INACTIVE", "Farm must be active to create plots or seasons", HttpStatus.BAD_REQUEST),

        // Crop / Season errors
        CROP_NOT_FOUND("ERR_CROP_NOT_FOUND", "Crop not found", HttpStatus.NOT_FOUND),
        INVALID_PLANTING_DATES("ERR_INVALID_PLANTING_DATES", "Planting date must be before or equal to harvest date",
                        HttpStatus.BAD_REQUEST),
        PLOT_FULL("ERR_PLOT_FULL", "Plot has no remaining capacity", HttpStatus.BAD_REQUEST),

        SEASON_NOT_FOUND("ERR_SEASON_NOT_FOUND", "Season not found", HttpStatus.NOT_FOUND),
        INVALID_SEASON_DATES("ERR_INVALID_SEASON_DATES", "Season start date must be before end date",
                        HttpStatus.BAD_REQUEST),
        SEASON_OVERLAP("ERR_SEASON_OVERLAP", "Season dates overlap with an existing season on the same plot",
                        HttpStatus.BAD_REQUEST),
        INVALID_SEASON_STATUS_TRANSITION("ERR_INVALID_SEASON_STATUS_TRANSITION", "Invalid season status transition",
                        HttpStatus.BAD_REQUEST),
        SEASON_HAS_CHILD_RECORDS("ERR_SEASON_HAS_CHILD_RECORDS",
                        "Cannot delete season with related harvests, expenses or sales", HttpStatus.BAD_REQUEST),
        SEASON_REPORT_REQUIRES_HARVEST_COMPLETED("ERR_SEASON_REPORT_REQUIRES_HARVEST_COMPLETED",
                        "Reports are available only for seasons that have finished harvesting 100%",
                        HttpStatus.BAD_REQUEST),

        // Harvest errors
        HARVEST_NOT_FOUND("ERR_HARVEST_NOT_FOUND", "Harvest not found", HttpStatus.NOT_FOUND),
        INVALID_HARVEST_QUANTITY("ERR_INVALID_HARVEST_QUANTITY", "Invalid harvest quantity", HttpStatus.BAD_REQUEST),
        HARVEST_DATE_BEFORE_PLANTING("ERR_HARVEST_DATE_BEFORE_PLANTING", "Harvest date cannot be before planting date",
                        HttpStatus.BAD_REQUEST),
        HARVEST_ALREADY_RECEIVED_TO_PRODUCT_WAREHOUSE("ERR_HARVEST_ALREADY_RECEIVED_TO_PRODUCT_WAREHOUSE",
                        "Harvest has already been received into product warehouse and cannot be edited",
                        HttpStatus.BAD_REQUEST),

        // Task / field log / expense / quality errors (season operations)
        TASK_NOT_FOUND("ERR_TASK_NOT_FOUND", "Task not found", HttpStatus.NOT_FOUND),
        INVALID_TASK_STATUS_TRANSITION("ERR_INVALID_TASK_STATUS_TRANSITION", "Invalid task status transition",
                        HttpStatus.BAD_REQUEST),
        INVALID_OPERATION("ERR_INVALID_OPERATION", "Invalid operation for current task status", HttpStatus.BAD_REQUEST),
        INVALID_DATE_RANGE("ERR_INVALID_DATE_RANGE",
                        "Invalid date range: end date must be after or equal to start date", HttpStatus.BAD_REQUEST),
        SEASON_CLOSED_CANNOT_ADD_TASK("ERR_SEASON_CLOSED_CANNOT_ADD_TASK", "Cannot add task to closed season",
                        HttpStatus.BAD_REQUEST),
        SEASON_CLOSED_CANNOT_MODIFY_TASK("ERR_SEASON_CLOSED_CANNOT_MODIFY_TASK", "Cannot modify task of closed season",
                        HttpStatus.BAD_REQUEST),
        EMPLOYEE_ROLE_REQUIRED("ERR_EMPLOYEE_ROLE_REQUIRED", "Selected user must have EMPLOYEE role",
                        HttpStatus.BAD_REQUEST),
        SEASON_EMPLOYEE_NOT_FOUND("ERR_SEASON_EMPLOYEE_NOT_FOUND", "Employee is not registered in this season",
                        HttpStatus.NOT_FOUND),
        SEASON_EMPLOYEE_ALREADY_EXISTS("ERR_SEASON_EMPLOYEE_ALREADY_EXISTS",
                        "Employee already exists in this season",
                        HttpStatus.CONFLICT),
        TASK_NOT_ASSIGNED_TO_EMPLOYEE("ERR_TASK_NOT_ASSIGNED_TO_EMPLOYEE",
                        "Task is not assigned to the current employee",
                        HttpStatus.FORBIDDEN),

        FIELD_LOG_NOT_FOUND("ERR_FIELD_LOG_NOT_FOUND", "Field log not found", HttpStatus.NOT_FOUND),
        INVALID_LOG_TYPE("ERR_INVALID_LOG_TYPE",
                        "Invalid log type. Allowed values: TRANSPLANT, FERTILIZE, PEST, SPRAY, IRRIGATE, WEED, HARVEST, WEATHER, GROWTH, OTHER",
                        HttpStatus.BAD_REQUEST),
        SEASON_CLOSED_CANNOT_ADD_FIELD_LOG("ERR_SEASON_CLOSED_CANNOT_ADD_FIELD_LOG",
                        "Cannot add field log to closed season", HttpStatus.BAD_REQUEST),
        SEASON_CLOSED_CANNOT_MODIFY_FIELD_LOG("ERR_SEASON_CLOSED_CANNOT_MODIFY_FIELD_LOG",
                        "Cannot modify field log of closed season", HttpStatus.BAD_REQUEST),

        EXPENSE_NOT_FOUND("ERR_EXPENSE_NOT_FOUND", "Expense not found", HttpStatus.NOT_FOUND),
        EXPENSE_PERIOD_LOCKED("ERR_EXPENSE_PERIOD_LOCKED", "Expenses cannot be modified in a closed or locked season",
                        HttpStatus.BAD_REQUEST),

        LOT_HAS_LINKED_ORDERS_OR_QC("ERR_LOT_HAS_LINKED_ORDERS_OR_QC",
                        "Cannot delete harvest lot with linked orders or quality results", HttpStatus.BAD_REQUEST),

        // Address / Location errors
        PROVINCE_NOT_FOUND("ERR_PROVINCE_NOT_FOUND", "Province not found", HttpStatus.NOT_FOUND),
        PROVINCE_REQUIRED("ERR_PROVINCE_REQUIRED", "Province is required when creating a farm", HttpStatus.BAD_REQUEST),
        WARD_NOT_FOUND("ERR_WARD_NOT_FOUND", "Ward not found", HttpStatus.NOT_FOUND),
        WARD_REQUIRED("ERR_WARD_REQUIRED", "Ward is required when creating a farm", HttpStatus.BAD_REQUEST),
        WARD_NOT_IN_PROVINCE("ERR_WARD_NOT_IN_PROVINCE", "Ward does not belong to the specified province",
                        HttpStatus.BAD_REQUEST),
        ADDRESS_IMPORT_FAILED("ERR_ADDRESS_IMPORT_FAILED", "Failed to import address data",
                        HttpStatus.INTERNAL_SERVER_ERROR),

        // Inventory errors
        INSUFFICIENT_STOCK("ERR_INSUFFICIENT_STOCK", "Insufficient stock for this operation", HttpStatus.BAD_REQUEST),
        ADJUST_NOTE_REQUIRED("ERR_ADJUST_NOTE_REQUIRED", "Note is required for ADJUST movements",
                        HttpStatus.BAD_REQUEST),
        OUT_SEASON_REQUIRED("ERR_OUT_SEASON_REQUIRED", "Season is required for OUT movements", HttpStatus.BAD_REQUEST),
        LOT_NOT_IN_STOCK("ERR_LOT_NOT_IN_STOCK", "Supply lot must have status IN_STOCK for OUT movements",
                        HttpStatus.BAD_REQUEST),
        WAREHOUSE_NOT_FOUND("ERR_WAREHOUSE_NOT_FOUND", "Warehouse not found", HttpStatus.NOT_FOUND),
        LOCATION_NOT_FOUND("ERR_LOCATION_NOT_FOUND", "Stock location not found", HttpStatus.NOT_FOUND),
        SUPPLY_LOT_NOT_FOUND("ERR_SUPPLY_LOT_NOT_FOUND", "Supply lot not found", HttpStatus.NOT_FOUND),
        PRODUCT_WAREHOUSE_LOT_NOT_FOUND("ERR_PRODUCT_WAREHOUSE_LOT_NOT_FOUND", "Product warehouse lot not found",
                        HttpStatus.NOT_FOUND),
        PRODUCT_WAREHOUSE_RECEIPT_DUPLICATE("ERR_PRODUCT_WAREHOUSE_RECEIPT_DUPLICATE",
                        "Product warehouse receipt already exists for this harvest", HttpStatus.CONFLICT),
        PRODUCT_WAREHOUSE_INVALID_UNIT("ERR_PRODUCT_WAREHOUSE_INVALID_UNIT",
                        "Invalid unit for product warehouse lot", HttpStatus.BAD_REQUEST),
        PRODUCT_WAREHOUSE_INVALID_QUANTITY("ERR_PRODUCT_WAREHOUSE_INVALID_QUANTITY",
                        "Quantity must be greater than 0", HttpStatus.BAD_REQUEST),
        PRODUCT_WAREHOUSE_LOCATION_MISMATCH("ERR_PRODUCT_WAREHOUSE_LOCATION_MISMATCH",
                        "Location does not belong to selected warehouse", HttpStatus.BAD_REQUEST),
        PRODUCT_WAREHOUSE_FARM_MISMATCH("ERR_PRODUCT_WAREHOUSE_FARM_MISMATCH",
                        "Selected farm data is inconsistent across season/plot/warehouse", HttpStatus.BAD_REQUEST),
        PRODUCT_WAREHOUSE_NO_OUTPUT_WAREHOUSE("ERR_PRODUCT_WAREHOUSE_NO_OUTPUT_WAREHOUSE",
                        "No warehouse available to receive harvested products", HttpStatus.BAD_REQUEST),

        // Marketplace errors
        MARKETPLACE_PRODUCT_NOT_FOUND("ERR_MARKETPLACE_PRODUCT_NOT_FOUND", "Marketplace product not found",
                        HttpStatus.NOT_FOUND),
        MARKETPLACE_PRODUCT_NOT_PUBLISHED("ERR_MARKETPLACE_PRODUCT_NOT_PUBLISHED",
                        "Marketplace product is not published", HttpStatus.CONFLICT),
        MARKETPLACE_CART_ITEM_NOT_FOUND("ERR_MARKETPLACE_CART_ITEM_NOT_FOUND", "Marketplace cart item not found",
                        HttpStatus.NOT_FOUND),
        MARKETPLACE_CART_EMPTY("ERR_MARKETPLACE_CART_EMPTY", "Marketplace cart is empty", HttpStatus.BAD_REQUEST),
        MARKETPLACE_STOCK_CONFLICT("ERR_MARKETPLACE_STOCK_CONFLICT",
                        "Marketplace stock is not sufficient for requested quantity", HttpStatus.CONFLICT),
        MARKETPLACE_IDEMPOTENCY_KEY_REQUIRED("ERR_MARKETPLACE_IDEMPOTENCY_KEY_REQUIRED",
                        "Idempotency key is required for order creation", HttpStatus.BAD_REQUEST),
        MARKETPLACE_IDEMPOTENCY_CONFLICT("ERR_MARKETPLACE_IDEMPOTENCY_CONFLICT",
                        "Idempotency key conflicts with an existing request payload", HttpStatus.CONFLICT),
        MARKETPLACE_ORDER_NOT_FOUND("ERR_MARKETPLACE_ORDER_NOT_FOUND", "Marketplace order not found",
                        HttpStatus.NOT_FOUND),
        MARKETPLACE_ORDER_CANCEL_NOT_ALLOWED("ERR_MARKETPLACE_ORDER_CANCEL_NOT_ALLOWED",
                        "Marketplace order cannot be cancelled in current status", HttpStatus.CONFLICT),
        MARKETPLACE_PAYMENT_PROOF_NOT_ALLOWED("ERR_MARKETPLACE_PAYMENT_PROOF_NOT_ALLOWED",
                        "Marketplace payment proof is not allowed for this order", HttpStatus.CONFLICT),
        MARKETPLACE_PAYMENT_PROOF_REQUIRED("ERR_MARKETPLACE_PAYMENT_PROOF_REQUIRED",
                        "Marketplace payment proof is required before verification", HttpStatus.BAD_REQUEST),
        MARKETPLACE_PAYMENT_PROOF_INVALID("ERR_MARKETPLACE_PAYMENT_PROOF_INVALID",
                        "Marketplace payment proof file is invalid", HttpStatus.BAD_REQUEST),
        MARKETPLACE_PAYMENT_VERIFICATION_INVALID("ERR_MARKETPLACE_PAYMENT_VERIFICATION_INVALID",
                        "Marketplace payment verification request is invalid", HttpStatus.BAD_REQUEST),
        MARKETPLACE_ADDRESS_NOT_FOUND("ERR_MARKETPLACE_ADDRESS_NOT_FOUND", "Marketplace address not found",
                        HttpStatus.NOT_FOUND),
        MARKETPLACE_ADDRESS_REQUIRED("ERR_MARKETPLACE_ADDRESS_REQUIRED",
                        "Marketplace shipping address is required", HttpStatus.BAD_REQUEST),
        MARKETPLACE_ADDRESS_PHONE_INVALID("ERR_MARKETPLACE_ADDRESS_PHONE_INVALID",
                        "Phone must be a valid Vietnamese mobile number", HttpStatus.BAD_REQUEST),
        MARKETPLACE_REVIEW_NOT_ALLOWED("ERR_MARKETPLACE_REVIEW_NOT_ALLOWED",
                        "Marketplace review is not allowed for this order state", HttpStatus.CONFLICT),
        MARKETPLACE_REVIEW_ALREADY_EXISTS("ERR_MARKETPLACE_REVIEW_ALREADY_EXISTS",
                        "Marketplace review already exists", HttpStatus.CONFLICT),
        MARKETPLACE_INVALID_RATING("ERR_MARKETPLACE_INVALID_RATING",
                        "Marketplace review rating must be in range 1..5", HttpStatus.BAD_REQUEST),
        MARKETPLACE_REVIEW_NOT_FOUND("ERR_MARKETPLACE_REVIEW_NOT_FOUND",
                        "Marketplace review not found", HttpStatus.NOT_FOUND),
        MARKETPLACE_REVIEW_ORDER_NOT_COMPLETED("ERR_MARKETPLACE_REVIEW_ORDER_NOT_COMPLETED",
                        "Review is only allowed for completed orders", HttpStatus.CONFLICT),
        MARKETPLACE_REVIEW_ITEM_NOT_IN_ORDER("ERR_MARKETPLACE_REVIEW_ITEM_NOT_IN_ORDER",
                        "Order item does not belong to the specified order", HttpStatus.BAD_REQUEST),
        MARKETPLACE_TRACEABILITY_CHAIN_INVALID("ERR_MARKETPLACE_TRACEABILITY_CHAIN_INVALID",
                        "Marketplace traceability chain is invalid", HttpStatus.CONFLICT),

        // Supplies errors
        SUPPLIER_NOT_FOUND("ERR_SUPPLIER_NOT_FOUND", "Supplier not found", HttpStatus.NOT_FOUND),
        SUPPLY_ITEM_NOT_FOUND("ERR_SUPPLY_ITEM_NOT_FOUND", "Supply item not found", HttpStatus.NOT_FOUND),
        RESTRICTED_CONFIRM_REQUIRED("ERR_RESTRICTED_CONFIRM_REQUIRED",
                        "Confirmation is required to handle restricted supplies", HttpStatus.BAD_REQUEST),

        // Incident errors
        INCIDENT_NOT_FOUND("ERR_INCIDENT_NOT_FOUND", "Incident not found", HttpStatus.NOT_FOUND),
        INVALID_INCIDENT_STATUS_TRANSITION("ERR_INVALID_INCIDENT_STATUS_TRANSITION",
                        "Invalid incident status transition",
                        HttpStatus.BAD_REQUEST),
        RESOLUTION_NOTE_REQUIRED("ERR_RESOLUTION_NOTE_REQUIRED",
                        "Resolution note is required when resolving an incident",
                        HttpStatus.BAD_REQUEST),
        CANNOT_DELETE_RESOLVED_INCIDENT("ERR_CANNOT_DELETE_RESOLVED_INCIDENT", "Cannot delete a resolved incident",
                        HttpStatus.BAD_REQUEST),
        INVALID_DEADLINE("ERR_INVALID_DEADLINE", "Deadline must be today or in the future", HttpStatus.BAD_REQUEST),
        OPTIMISTIC_LOCK_ERROR("ERR_OPTIMISTIC_LOCK_ERROR",
                        "Concurrent modification detected. Please refresh and try again.", HttpStatus.CONFLICT),

        // Season Admin Business Rules
        SEASON_COMPLETION_REQUIRES_YIELD_AND_DATE("ERR_SEASON_COMPLETION_REQUIRES_YIELD_AND_DATE",
                        "End date and actual yield are required when completing a season", HttpStatus.BAD_REQUEST),

        // Season Business Rules (BR8/BR12)
        SEASON_NAME_EXISTS_IN_PLOT("ERR_SEASON_NAME_EXISTS_IN_PLOT",
                        "Season name already exists in this plot", HttpStatus.CONFLICT),

        // Access Control Business Rules (BR1)
        ACCESS_DENIED_INACTIVE_USER("ERR_ACCESS_DENIED_INACTIVE_USER",
                        "Access denied: account is inactive or suspended", HttpStatus.FORBIDDEN),

        // Expense Business Rules (BR8/BR12)
        EXPENSE_SEASON_PLOT_MISMATCH("ERR_EXPENSE_SEASON_PLOT_MISMATCH",
                        "Selected season does not belong to the selected plot", HttpStatus.BAD_REQUEST),
        EXPENSE_TASK_SEASON_MISMATCH("ERR_EXPENSE_TASK_SEASON_MISMATCH",
                        "Selected task does not belong to the selected season", HttpStatus.BAD_REQUEST),
        EXPENSE_AMOUNT_INVALID("ERR_EXPENSE_AMOUNT_INVALID",
                        "Amount must be greater than 0", HttpStatus.BAD_REQUEST),

        // ═══════════════════════════════════════════════════════════════════════════
        // BR174-BR187: Expense Module Standard MSG Codes
        // ═══════════════════════════════════════════════════════════════════════════

        /**
         * MSG 1: Mandatory field is empty
         * BR175/BR179: When one of the mandatory fields' txtBox.Text isEmpty() = "true"
         */
        MSG_1_MANDATORY_FIELD_EMPTY("MSG_1", "Please enter mandatory data.", HttpStatus.BAD_REQUEST),

        /**
         * MSG 4: Invalid data format
         * BR175/BR179: When field is in wrong format or amount <= 0
         */
        MSG_4_INVALID_FORMAT("MSG_4", "Invalid data format. Please enter again.", HttpStatus.BAD_REQUEST),

        /**
         * MSG 7: Save data successful
         * BR176/BR180/BR183: When CRUD operation is successful
         */
        MSG_7_SAVE_SUCCESS("MSG_7", "Save data successful.", HttpStatus.OK),

        /**
         * MSG 9: Constraint violation
         * BR176/BR180/BR183: When season-plot/task-season mismatch or FK violation
         */
        MSG_9_CONSTRAINT_VIOLATION("MSG_9", "Your action is failed due to constraints in the system.",
                        HttpStatus.BAD_REQUEST),

        /**
         * MSG 10: Expense not found
         * BR187: When expense search returns no results
         */
        MSG_10_EXPENSE_NOT_FOUND("MSG_10", "Expense not found.", HttpStatus.NOT_FOUND),

        /**
         * MSG 10: Season not found
         * BR121: When season search returns no results
         */
        MSG_10_SEASON_NOT_FOUND("MSG_10", "Season not found.", HttpStatus.NOT_FOUND),

        /**
         * MSG 11: Confirmation message for delete/archive operations
         * BR134/BR181: Confirmation message for archive/delete operation
         */
        MSG_11_CONFIRMATION("MSG_11", "Are you sure you want to proceed with this action?", HttpStatus.OK);

        ErrorCode(String code, String message, HttpStatus statusCode) {
                this.code = code;
                this.message = message;
                this.statusCode = statusCode;
        }

        private final String code;
        private final String message;
        private final HttpStatus statusCode;
}
