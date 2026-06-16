package br.com.ottonsam.toothy_planner_api.auth.usecases;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class AuthCookieFactory {

    private final boolean cookieSecure;

    public AuthCookieFactory(@Value("${app.auth.cookie-secure:false}") boolean cookieSecure) {
        this.cookieSecure = cookieSecure;
    }

    public void addAuthCookies(HttpServletResponse response, AuthTokens tokens) {
        response.addHeader(
                HttpHeaders.SET_COOKIE,
                createCookie("access_token", tokens.accessToken(), 15 * 60).toString());
        response.addHeader(
                HttpHeaders.SET_COOKIE,
                createCookie("refresh_token", tokens.refreshToken(), 2 * 24 * 60 * 60)
                        .toString());
    }

    public void clearAuthCookies(HttpServletResponse response) {
        response.addHeader(
                HttpHeaders.SET_COOKIE, createCookie("access_token", "", 0).toString());
        response.addHeader(
                HttpHeaders.SET_COOKIE, createCookie("refresh_token", "", 0).toString());
    }

    private ResponseCookie createCookie(String name, String value, long maxAgeSeconds) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAgeSeconds)
                .build();
    }
}
