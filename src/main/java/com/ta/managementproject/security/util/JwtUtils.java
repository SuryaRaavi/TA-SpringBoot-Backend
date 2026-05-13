package com.ta.managementproject.security.util;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Date;
import org.slf4j.Logger;

@Component
public class JwtUtils {
    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    @Value("${k3.app.jwtSecret}")
    private String jwtSecret;

    @Value("${k3.app.jwtExpirationMs}")
    private long jwtExpirationMs;
//    private long jwtExpirationMs = 86400000;
    public String generateJwtToken(String email, String role) { // CYC: 1, LOC: 9, COG: 0
        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes()))
                .compact();
    }

    public static String getCurrentEmail(){ // CYC: 1, LOC: 3, COG: 0
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    public boolean validateJwtToken(String authToken) {
        Jwts.parser().verifyWith(Keys.hmacShaKeyFor(jwtSecret.getBytes())).build().parse(authToken);
        return true;
    }

    public String getEmailFromJwtToken(String token) {
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
