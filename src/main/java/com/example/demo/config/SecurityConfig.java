package com.example.demo.config;


import java.io.IOException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Configuration
public class SecurityConfig {

    @Bean
    public AuthenticationFilter authenticationFilter() {
        return new AuthenticationFilter();
    }

    public static class AuthenticationFilter extends OncePerRequestFilter {

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                FilterChain filterChain) throws ServletException, IOException {
            
            String path = request.getRequestURI();
            
            // Intercepte les appels aux ressources ERPNext
            if (path.startsWith("/api/resource/")) {
                HttpSession session = request.getSession(false);
                
                //  Vérifie si une session existe
                if (session == null || session.getAttribute("frappe_sid") == null) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"Session ERPNext requise. Veuillez vous connecter via /api/auth/login\"}");
                    return;
                }
                
                System.out.println("Session trouvée pour " + path + ": " + session.getAttribute("frappe_sid"));
            }
            
            filterChain.doFilter(request, response);
        }
    }
}