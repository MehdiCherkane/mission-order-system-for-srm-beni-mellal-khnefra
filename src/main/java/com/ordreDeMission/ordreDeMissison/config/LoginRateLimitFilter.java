package com.ordreDeMission.ordreDeMissison.config;

import com.ordreDeMission.ordreDeMissison.service.LoginAttemptService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Rejects already-blocked login attempts BEFORE Spring Security burns
 * BCrypt on them. Runs before UsernamePasswordAuthenticationFilter.
 */
@Component
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private final LoginAttemptService loginAttemptService;

    public LoginRateLimitFilter(LoginAttemptService loginAttemptService) {
        this.loginAttemptService = loginAttemptService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if ("POST".equalsIgnoreCase(request.getMethod()) && "/login".equals(request.getServletPath())) {
            String key = LoginAttemptService.loginKey(
                    LoginAttemptService.clientIp(request), request.getParameter("matricule"));
            if (loginAttemptService.isBlocked(key)) {
                response.sendRedirect(request.getContextPath() + "/login?blocked");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
