package com.example.khmer_market.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.UUID;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public Object handleNotFound(ResourceNotFoundException ex, HttpServletRequest request, Model model) {
        log.warn("Resource not found: {}", ex.getMessage());
        if (isJsonRequest(request)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("RESOURCE_NOT_FOUND", ex.getMessage()));
        }
        model.addAttribute("isAdminArea", isAdminPath(request));
        model.addAttribute("errorMessage", ex.getMessage());
        return "error/404";
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public Object handleNoHandler(NoHandlerFoundException ex, HttpServletRequest request, Model model) {
        if (isJsonRequest(request)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("NOT_FOUND", "The requested endpoint does not exist."));
        }
        model.addAttribute("isAdminArea", isAdminPath(request));
        return "error/404";
    }

    @ExceptionHandler(AccessDeniedException.class)
    public Object handleAccessDenied(AccessDeniedException ex, HttpServletRequest request, Model model) {
        log.warn("Access denied for request '{}': {}", request.getRequestURI(), ex.getMessage());
        if (isJsonRequest(request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("ACCESS_DENIED", "Access denied. You do not have permission to view this resource."));
        }
        model.addAttribute("isAdminArea", isAdminPath(request));
        model.addAttribute("errorMessage", ex.getMessage());
        return "error/403";
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public Object handleRateLimit(RateLimitExceededException ex, HttpServletRequest request, Model model) {
        log.warn("Rate limit triggered for request '{}': {}", request.getRequestURI(), ex.getMessage());
        if (isJsonRequest(request)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()))
                    .body(new ErrorResponse("RATE_LIMITED", ex.getMessage(), ex.getRetryAfterSeconds()));
        }
        model.addAttribute("isAdminArea", isAdminPath(request));
        model.addAttribute("retryAfterSeconds", ex.getRetryAfterSeconds());
        return "error/429";
    }

    @ExceptionHandler({InvalidOrderTransitionException.class, OutOfStockException.class, IllegalArgumentException.class})
    public Object handleBusinessExceptions(RuntimeException ex, HttpServletRequest request, Model model) {
        log.warn("Business rule exception: {}", ex.getMessage());
        if (isJsonRequest(request)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("BAD_REQUEST", ex.getMessage()));
        }
        model.addAttribute("isAdminArea", isAdminPath(request));
        model.addAttribute("errorMessage", ex.getMessage());
        return "error/error";
    }

    @ExceptionHandler(Exception.class)
    public Object handleGeneralException(Exception ex, HttpServletRequest request, Model model) {
        String errorRef = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.error("Internal Server Error [Ref: {}] on URI '{}'", errorRef, request.getRequestURI(), ex);

        if (isJsonRequest(request)) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("INTERNAL_SERVER_ERROR", "An unexpected error occurred. Reference ID: " + errorRef));
        }

        model.addAttribute("isAdminArea", isAdminPath(request));
        model.addAttribute("refId", errorRef);
        return "error/500";
    }

    private boolean isJsonRequest(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        String requestedWith = request.getHeader("X-Requested-With");
        return "XMLHttpRequest".equalsIgnoreCase(requestedWith) || (accept != null && accept.contains("application/json"));
    }

    private boolean isAdminPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri != null && uri.startsWith("/admin");
    }
}
