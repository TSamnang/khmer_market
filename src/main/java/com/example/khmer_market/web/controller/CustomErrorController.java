package com.example.khmer_market.web.controller;

import com.example.khmer_market.exception.ErrorResponse;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.UUID;

@Controller
public class CustomErrorController implements ErrorController {

    private static final Logger log = LoggerFactory.getLogger(CustomErrorController.class);

    @RequestMapping("/error")
    public Object handleError(HttpServletRequest request, Model model) {
        Object statusObj = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int statusCode = HttpStatus.INTERNAL_SERVER_ERROR.value();
        if (statusObj != null) {
            try {
                statusCode = Integer.parseInt(statusObj.toString());
            } catch (NumberFormatException ignored) {}
        }

        boolean isAdminArea = isAdminPath(request);
        model.addAttribute("isAdminArea", isAdminArea);

        if (isJsonRequest(request)) {
            String code = switch (statusCode) {
                case 400 -> "BAD_REQUEST";
                case 403 -> "FORBIDDEN";
                case 404 -> "NOT_FOUND";
                case 429 -> "RATE_LIMITED";
                default -> "SERVER_ERROR";
            };
            return ResponseEntity.status(statusCode).body(new ErrorResponse(code, "An error occurred. Status: " + statusCode));
        }

        if (statusCode == 404) {
            return "error/404";
        }
        if (statusCode == 403) {
            return "error/403";
        }
        if (statusCode == 429) {
            Object retryAttr = request.getAttribute("retryAfterSeconds");
            model.addAttribute("retryAfterSeconds", retryAttr != null ? retryAttr : 60);
            return "error/429";
        }

        String refId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.error("Internal Server Error via ErrorController [Ref: {}] on URI '{}' status {}",
                refId, request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI), statusCode);
        model.addAttribute("refId", refId);
        return "error/500";
    }

    @RequestMapping("/error/403")
    public String accessDeniedPage(HttpServletRequest request, Model model) {
        model.addAttribute("isAdminArea", isAdminPath(request));
        return "error/403";
    }

    @RequestMapping("/error/404")
    public String notFoundPage(HttpServletRequest request, Model model) {
        model.addAttribute("isAdminArea", isAdminPath(request));
        return "error/404";
    }

    @RequestMapping("/error/429")
    public String rateLimitedPage(HttpServletRequest request, Model model) {
        model.addAttribute("isAdminArea", isAdminPath(request));
        Object retryAttr = request.getAttribute("retryAfterSeconds");
        model.addAttribute("retryAfterSeconds", retryAttr != null ? retryAttr : 60);
        return "error/429";
    }

    @RequestMapping("/error/500")
    public String serverErrorPage(HttpServletRequest request, Model model) {
        model.addAttribute("isAdminArea", isAdminPath(request));
        String refId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        model.addAttribute("refId", refId);
        return "error/500";
    }

    private boolean isJsonRequest(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        String requestedWith = request.getHeader("X-Requested-With");
        return "XMLHttpRequest".equalsIgnoreCase(requestedWith) || (accept != null && accept.contains("application/json"));
    }

    private boolean isAdminPath(HttpServletRequest request) {
        Object origUri = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        String uri = origUri != null ? origUri.toString() : request.getRequestURI();
        return uri != null && uri.startsWith("/admin");
    }
}
