package com.ta.managementproject.security.util;


import com.ta.managementproject.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Date;
import org.slf4j.Logger;
import org.springframework.util.StringUtils;

@Component
public class JwtUtils {
    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    @Value("${k3.app.jwtSecret}")
    private String jwtSecret;

    @Value("${k3.app.jwtExpirationMs}")
    private long jwtExpirationMs;
//    private long jwtExpirationMs = 86400000;
    public String generateJwtToken(String username, String role) { // CYC: 1, LOC: 9, COG: 0
        return Jwts.builder()
                .subject(username)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes()))
                .compact();
    }

    public static String getCurrentUsername(){ // CYC: 1, LOC: 3, COG: 0
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    // CYC: 3, LOC: 10
    public String getUserNameFromRequest(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");

        String token = "";

        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            token = headerAuth.substring(7);
        }

        JwtParser jwtParser = Jwts.parser().verifyWith(Keys.hmacShaKeyFor(jwtSecret.getBytes())).build();
        Claims claims = jwtParser.parse(token).accept(Jws.CLAIMS).getPayload();
        return claims.getSubject();
    }

    public boolean validateJwtToken(String authToken) {
        Jwts.parser().verifyWith(Keys.hmacShaKeyFor(jwtSecret.getBytes())).build().parse(authToken);
        return true;
    }

    public String getUserNameFromJwtToken(String token) {
        JwtParser jwtParser = Jwts.parser().verifyWith(Keys.hmacShaKeyFor(jwtSecret.getBytes())).build();
        Claims claims = jwtParser.parse(token).accept(Jws.CLAIMS).getPayload();
        return claims.getSubject();
    }

    // Total CYC: 1, LOC: 5, COG: 0
    public Date getExpirationFromToken(String token) {
        JwtParser jwtParser = Jwts.parser().verifyWith(Keys.hmacShaKeyFor(jwtSecret.getBytes())).build();
        Claims claims = jwtParser.parse(token).accept(Jws.CLAIMS).getPayload();
        return claims.getExpiration();
    }
}
