package com.milktea.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.security.SecureRandom;
import java.util.Base64;

public class CsrfTokenUtils {

    public static final String CSRF_TOKEN_COOKIE_NAME = "XSRF-TOKEN";
    public static final String CSRF_TOKEN_HEADER_NAME = "X-XSRF-TOKEN";
    private static final int TOKEN_LENGTH = 32;
    private static final SecureRandom secureRandom = new SecureRandom();

    private CsrfTokenUtils() {
    }

    public static String generateToken() {
        byte[] tokenBytes = new byte[TOKEN_LENGTH];
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    public static void setTokenCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie(CSRF_TOKEN_COOKIE_NAME, token);
        cookie.setPath("/");
        cookie.setHttpOnly(false);
        cookie.setSecure(false);
        cookie.setMaxAge(-1);
        response.addCookie(cookie);
    }

    public static String getTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (CSRF_TOKEN_COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    public static String getTokenFromHeader(HttpServletRequest request) {
        return request.getHeader(CSRF_TOKEN_HEADER_NAME);
    }

    public static boolean validateToken(String cookieToken, String headerToken) {
        if (cookieToken == null || headerToken == null) {
            return false;
        }
        return cookieToken.equals(headerToken);
    }

    public static boolean isSafeMethod(String method) {
        return "GET".equals(method) || "HEAD".equals(method) || "OPTIONS".equals(method) || "TRACE".equals(method);
    }
}
