package com.example.tb.jwt;

import com.example.tb.authentication.service.admin.AdminServiceImpl;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtTokenFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenFilter.class);
    
    private final AdminServiceImpl adminServiceImpl;
    private final JwtTokenUtils jwtTokenUtil;

    public JwtTokenFilter(AdminServiceImpl adminServiceImpl, JwtTokenUtils jwtTokenUtil) {
        this.adminServiceImpl = adminServiceImpl;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        final String requestTokenHeader = request.getHeader("Authorization");        String username = null;
        String jwtToken = null;
        if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer ")) {
            jwtToken = requestTokenHeader.substring(7);
            
            // Enhanced JWT validation to prevent parsing errors
            if (jwtToken == null || jwtToken.trim().isEmpty()) {
                logger.debug("Empty JWT token provided");
            } else if (jwtToken.split("\\.").length != 3) {
                logger.debug("Invalid JWT token format - expected 3 parts separated by dots, found: {}", jwtToken.split("\\.").length);
            } else if (jwtToken.equals("google-auth-error") || jwtToken.equals("undefined") || jwtToken.equals("null")) {
                logger.debug("Invalid JWT token value: {}", jwtToken);
            } else {
                try {
                    username = jwtTokenUtil.getUsernameFromToken(jwtToken);
                } catch (IllegalArgumentException e) {
                    logger.debug("Unable to get JWT Token: {}", e.getMessage());
                } catch (ExpiredJwtException e) {
                    logger.debug("JWT Token has expired: {}", e.getMessage());
                } catch (Exception e) {
                    logger.debug("JWT Token parsing error: {}", e.getMessage());
                }
            }
        } else if (requestTokenHeader != null && !requestTokenHeader.equals("Bearer")) {
            logger.debug("JWT Token does not begin with Bearer String: {}", requestTokenHeader);
        }        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                UserDetails userDetails = this.adminServiceImpl.loadUserByUsername(username);
                if (userDetails != null && jwtTokenUtil.validateToken(jwtToken, userDetails)) {

                    UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    usernamePasswordAuthenticationToken
                            .setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
                }
            } catch (org.springframework.security.core.userdetails.UsernameNotFoundException e) {
                logger.debug("User not found for JWT token: {}", username);
            }
        }
        chain.doFilter(request, response);
    }
}
