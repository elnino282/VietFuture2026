package org.example.QuanLyMuaVu.Exception;

public class ValidationException extends AppException {

    public ValidationException() {
        super(ErrorCode.BAD_REQUEST);
    }

    public ValidationException(String message) {
        super(ErrorCode.BAD_REQUEST);
    }

    public ValidationException(ErrorCode errorCode) {
        super(errorCode);
    }
}
