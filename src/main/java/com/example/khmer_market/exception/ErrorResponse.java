package com.example.khmer_market.exception;

public class ErrorResponse {

    private ErrorDetails error;

    public ErrorResponse() {}

    public ErrorResponse(String code, String message) {
        this.error = new ErrorDetails(code, message);
    }

    public ErrorResponse(String code, String message, Long retryAfterSeconds) {
        this.error = new ErrorDetails(code, message, retryAfterSeconds);
    }

    public ErrorDetails getError() {
        return error;
    }

    public void setError(ErrorDetails error) {
        this.error = error;
    }

    public static class ErrorDetails {
        private String code;
        private String message;
        private Long retryAfterSeconds;

        public ErrorDetails() {}

        public ErrorDetails(String code, String message) {
            this.code = code;
            this.message = message;
        }

        public ErrorDetails(String code, String message, Long retryAfterSeconds) {
            this.code = code;
            this.message = message;
            this.retryAfterSeconds = retryAfterSeconds;
        }

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public Long getRetryAfterSeconds() { return retryAfterSeconds; }
        public void setRetryAfterSeconds(Long retryAfterSeconds) { this.retryAfterSeconds = retryAfterSeconds; }
    }
}
