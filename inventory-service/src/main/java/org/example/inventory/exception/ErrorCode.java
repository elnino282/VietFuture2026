package org.example.inventory.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION("ERR_UNCATEGORIZED_EXCEPTION", "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
    KEY_INVALID("ERR_KEY_INVALID", "Invalid key", HttpStatus.BAD_REQUEST),
    INTERNAL_SERVER_ERROR("ERR_INTERNAL_SERVER_ERROR", "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR),
    BAD_REQUEST("ERR_BAD_REQUEST", "Bad request", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED("ERR_UNAUTHORIZED", "Unauthorized", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("ERR_FORBIDDEN", "Forbidden", HttpStatus.FORBIDDEN),
    RESOURCE_NOT_FOUND("ERR_RESOURCE_NOT_FOUND", "Resource not found", HttpStatus.NOT_FOUND),
    DUPLICATE_RESOURCE("ERR_DUPLICATE_RESOURCE", "Resource already exists", HttpStatus.CONFLICT),
    UNAUTHENTICATED("ERR_UNAUTHENTICATED", "Unauthenticated", HttpStatus.UNAUTHORIZED),
    FARM_NOT_FOUND("ERR_FARM_NOT_FOUND", "Farm not found", HttpStatus.NOT_FOUND),
    PLOT_NOT_FOUND("ERR_PLOT_NOT_FOUND", "Plot not found", HttpStatus.NOT_FOUND),
    SEASON_NOT_FOUND("ERR_SEASON_NOT_FOUND", "Season not found", HttpStatus.NOT_FOUND),
    HARVEST_NOT_FOUND("ERR_HARVEST_NOT_FOUND", "Harvest not found", HttpStatus.NOT_FOUND),
    WAREHOUSE_NOT_FOUND("ERR_WAREHOUSE_NOT_FOUND", "Warehouse not found", HttpStatus.NOT_FOUND),
    LOCATION_NOT_FOUND("ERR_LOCATION_NOT_FOUND", "Stock location not found", HttpStatus.NOT_FOUND),
    PRODUCT_WAREHOUSE_LOT_NOT_FOUND("ERR_PRODUCT_WAREHOUSE_LOT_NOT_FOUND", "Product warehouse lot not found", HttpStatus.NOT_FOUND),
    PRODUCT_WAREHOUSE_NO_OUTPUT_WAREHOUSE("ERR_PRODUCT_WAREHOUSE_NO_OUTPUT_WAREHOUSE", "No output warehouse available", HttpStatus.BAD_REQUEST),
    PRODUCT_WAREHOUSE_LOT_ALREADY_EXISTS("ERR_PRODUCT_WAREHOUSE_LOT_ALREADY_EXISTS", "Product lot already exists", HttpStatus.CONFLICT),
    PRODUCT_WAREHOUSE_INVALID_QUANTITY("ERR_PRODUCT_WAREHOUSE_INVALID_QUANTITY", "Invalid quantity", HttpStatus.BAD_REQUEST),
    PRODUCT_WAREHOUSE_INVALID_UNIT("ERR_PRODUCT_WAREHOUSE_INVALID_UNIT", "Invalid unit", HttpStatus.BAD_REQUEST),
    PRODUCT_WAREHOUSE_FARM_MISMATCH("ERR_PRODUCT_WAREHOUSE_FARM_MISMATCH", "Warehouse/plot doesn't belong to farm", HttpStatus.BAD_REQUEST),
    PRODUCT_WAREHOUSE_LOCATION_MISMATCH("ERR_PRODUCT_WAREHOUSE_LOCATION_MISMATCH", "Location doesn't belong to warehouse", HttpStatus.BAD_REQUEST),
    PRODUCT_WAREHOUSE_RECEIPT_DUPLICATE("ERR_PRODUCT_WAREHOUSE_RECEIPT_DUPLICATE", "Receipt for harvest already exists", HttpStatus.CONFLICT),
    INSUFFICIENT_STOCK("ERR_INSUFFICIENT_STOCK", "Insufficient stock", HttpStatus.BAD_REQUEST),
    ADJUST_NOTE_REQUIRED("ERR_ADJUST_NOTE_REQUIRED", "Adjustment note is required", HttpStatus.BAD_REQUEST);

    ErrorCode(String code, String message, HttpStatus statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }

    private final String code;
    private final String message;
    private final HttpStatus statusCode;
}
