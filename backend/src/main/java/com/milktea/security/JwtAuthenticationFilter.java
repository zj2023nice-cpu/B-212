package com.milktea.security;

import com.milktea.util.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtils jwtUtils;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);
        try {
            username = jwtUtils.extractUsername(jwt);
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // 从 Token 中提取角色信息，避免每次请求都查数据库
                String role = jwtUtils.extractRole(jwt);
                
                // 创建权限列表
                Collection<GrantedAuthority> authorities = new ArrayList<>();
                if (role != null && !role.isEmpty()) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
                }
                
                // 验证 Token 是否有效
                if (jwtUtils.validateToken(jwt, username)) {
                    // 从 Token 中提取用户ID
                    Long userId = jwtUtils.extractUserId(jwt);
                    
                    // 创建一个简单的 UserDetails 对象，包含用户名和权限
                    // 这里不需要查询数据库，因为信息都在 Token 中
                    org.springframework.security.core.userdetails.User userDetails = 
                            new org.springframework.security.core.userdetails.User(
                                    username, 
                                    "", 
                                    authorities
                            );
                    
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, authorities);
                    // 将用户ID存储在 details 中，方便后续使用
                    authToken.setDetails(userId);
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // Token 验证失败，不设置安全上下文
        }
        filterChain.doFilter(request, response);
    }
}
