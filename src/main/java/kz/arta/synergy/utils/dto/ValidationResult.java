package kz.arta.synergy.utils.dto;

import kz.arta.synergy.utils.constants.ValidationErrorType;

public class ValidationResult {
    private final boolean valid;
    private final String errorCode;
    private final String errorMessage;

    private ValidationResult(boolean valid, String errorCode, String errorMessage) {
        this.valid = valid;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public static ValidationResult success() {
        return new ValidationResult(true, null, null);
    }

    public static ValidationResult error(ValidationErrorType errorType, Object... args) {
        return new ValidationResult(false, errorType.getCode(), errorType.getMessage(args));
    }

    public boolean isValid() {
        return valid;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
