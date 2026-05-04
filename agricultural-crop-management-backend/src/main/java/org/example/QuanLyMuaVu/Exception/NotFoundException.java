package org.example.QuanLyMuaVu.Exception;

public class NotFoundException extends AppException {

    public NotFoundException() {
        super(ErrorCode.RESOURCE_NOT_FOUND);
    }

    public NotFoundException(String message) {
        super(ErrorCode.RESOURCE_NOT_FOUND);
    }

    public NotFoundException(ErrorCode errorCode) {
        super(errorCode);
    }
}
