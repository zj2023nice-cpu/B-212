package com.milktea.security;

import com.milktea.util.CsrfTokenUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.milktea.common.Result;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class CsrfTokenValidationFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String method = request.getMethod();
        
        if (CsrfTokenUtils.isSafeMethod(method)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        String cookieToken = CsrfTokenUtils.getTokenFromCookie(request);
        String headerToken = CsrfTokenUtils.getTokenFromHeader(request);
        
        if (!CsrfTokenUtils.validateToken(cookieToken, headerToken)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            
            Result<Void> errorResult = Result.error("Invalid CSRF token");
            response.getWriter().write(objectMapper.writeValueAsString(errorResult));
            return;
        }
        
        filterChain.doFilter(request, response);
    }
}
