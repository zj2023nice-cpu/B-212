package com.milktea.security;

import com.milktea.util.CsrfTokenUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class CsrfTokenGeneratorFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String existingToken = CsrfTokenUtils.getTokenFromCookie(request);
        if (existingToken == null || existingToken.isEmpty()) {
            String newToken = CsrfTokenUtils.generateToken();
            CsrfTokenUtils.setTokenCookie(response, newToken);
        }
        
        filterChain.doFilter(request, response);
    }
}
