package com.example.app.security;

import com.example.app.exception.InvalidTokenException;
import com.example.app.exception.ExpiredTokenException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Validates the JWT (if one is supplied) and populates the SecurityContext.
 *
 * Important: a *missing* token is not an error - the request simply proceeds
 * unauthenticated (some endpoints are public). However, a token that IS
 * supplied but is malformed/invalid/expired IS an authentication error and
 * must short-circuit the request with 401, rather than being swallowed and
 * silently treated as "no token", which previously caused such requests to
 * fall through to method-level @PreAuthorize checks and come back as a
 * misleading 403.
 */
@Slf4j
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;
    private final HandlerExceptionResolver handlerExceptionResolver;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            // No credentials supplied at all - proceed unauthenticated so public
            // endpoints keep working; protected endpoints will correctly reject
            // this as "not authenticated" further down.
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);
        String email;
        try {
            // Parse once and raise a specific, meaningful error instead of
            // quietly swallowing it - this is what allows the existing
            // GlobalExceptionHandler 401 handlers to actually be triggered.
            email = jwtUtil.parseClaims(token).getSubject();
        } catch (ExpiredJwtException e) {
            log.debug("Expired JWT token presented: {}", e.getMessage());
            handlerExceptionResolver.resolveException(request, response, null,
                    new ExpiredTokenException("Access token has expired - please log in again"));
            return;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Invalid JWT token presented: {}", e.getMessage());
            handlerExceptionResolver.resolveException(request, response, null,
                    new InvalidTokenException("Invalid access token - please log in again"));
            return;
        }

        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                if (!userDetails.isEnabled()) {
                    handlerExceptionResolver.resolveException(request, response, null,
                            new InvalidTokenException("Account is not active - please log in again"));
                    return;
                }
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            } catch (Exception e) {
                log.debug("Could not resolve user for valid token: {}", e.getMessage());
                handlerExceptionResolver.resolveException(request, response, null,
                        new InvalidTokenException("Invalid access token - please log in again"));
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
