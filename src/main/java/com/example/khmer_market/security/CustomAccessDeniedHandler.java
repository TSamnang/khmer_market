package com.example.khmer_market.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {

        boolean isCsrf = accessDeniedException instanceof CsrfException ||
                (accessDeniedException.getCause() != null && accessDeniedException.getCause() instanceof CsrfException);

        boolean isJson = isJsonRequest(request);

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);

        if (isJson) {
            response.setContentType("application/json;charset=UTF-8");
            String code = isCsrf ? "CSRF_ERROR" : "ACCESS_DENIED";
            String msg = isCsrf ? "Your session or security token expired. Please reload and try again."
                                : "Access denied. You do not have permission to perform this action.";
            response.getWriter().write(String.format("{\"error\":{\"code\":\"%s\",\"message\":\"%s\"}}", code, msg));
            return;
        }

        request.setAttribute("isCsrfError", isCsrf);
        request.setAttribute("errorMessage", accessDeniedException.getMessage());
        request.getRequestDispatcher("/error/403").forward(request, response);
    }

    private boolean isJsonRequest(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        String requestedWith = request.getHeader("X-Requested-With");
        return "XMLHttpRequest".equalsIgnoreCase(requestedWith) || (accept != null && accept.contains("application/json"));
    }
}
