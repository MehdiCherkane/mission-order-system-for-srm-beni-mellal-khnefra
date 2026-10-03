package com.ordreDeMission.ordreDeMissison.config;

import com.ordreDeMission.ordreDeMissison.model.Employee;
import com.ordreDeMission.ordreDeMissison.service.EmployeeService;
import com.ordreDeMission.ordreDeMissison.service.LoginAttemptService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.session.HttpSessionEventPublisher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final EmployeeService employeeService;
    private final LoginAttemptService loginAttemptService;
    private final LoginRateLimitFilter loginRateLimitFilter;

    public SecurityConfig(EmployeeService employeeService, LoginAttemptService loginAttemptService,
                          LoginRateLimitFilter loginRateLimitFilter) {
        this.employeeService = employeeService;
        this.loginAttemptService = loginAttemptService;
        this.loginRateLimitFilter = loginRateLimitFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/css/**", "/logo.png", "/login", "/access-denied").permitAll()
                .requestMatchers("/chef/**").hasAuthority("CHEF_HIERARCHIQUE")
                .requestMatchers("/directeur/**").hasAuthority("DIRECTEUR")
                .requestMatchers("/admin/**").hasAuthority("ADMIN")
                .requestMatchers("/employee/**").authenticated()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .usernameParameter("matricule")
                .passwordParameter("motDePasse")
                .successHandler(authenticationSuccessHandler())
                .failureHandler(authenticationFailureHandler())
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            )
            .exceptionHandling(ex -> ex.accessDeniedPage("/access-denied"))
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .maximumSessions(1)
                .expiredUrl("/login?expired")
            )
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'; style-src 'self' 'unsafe-inline'; script-src 'self' 'unsafe-inline'"))
                .frameOptions(frame -> frame.deny())
                .xssProtection(xss -> xss.disable())
                )
            .requestCache(rcc -> rcc.disable());
        http.addFilterBefore(loginRateLimitFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    private AuthenticationSuccessHandler authenticationSuccessHandler() {
        return (HttpServletRequest request, HttpServletResponse response, Authentication authentication) -> {
            String matricule = authentication.getName();
            loginAttemptService.recordSuccess(LoginAttemptService.loginKey(
                    LoginAttemptService.clientIp(request), matricule));
            Employee emp = employeeService.findByMatricule(matricule);
            if (emp == null) {
                response.sendRedirect("/login?error");
                return;
            }
            request.getSession().setAttribute("user", emp);
            String redirect = switch (emp.getRole()) {
                case "CHEF_HIERARCHIQUE" -> "/chef/pending";
                case "DIRECTEUR" -> "/directeur/pending";
                case "ADMIN" -> "/admin/dashboard";
                default -> "/employee/dashboard";
            };
            response.sendRedirect(redirect);
        };
    }

    private AuthenticationFailureHandler authenticationFailureHandler() {
        return (HttpServletRequest request, HttpServletResponse response,
                org.springframework.security.core.AuthenticationException exception) -> {
            String key = LoginAttemptService.loginKey(
                    LoginAttemptService.clientIp(request), request.getParameter("matricule"));
            loginAttemptService.recordFailure(key);
            if (loginAttemptService.isBlocked(key)) {
                response.sendRedirect("/login?blocked");
            } else {
                response.sendRedirect("/login?error");
            }
        };
    }
}
