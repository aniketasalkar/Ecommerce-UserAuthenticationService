package com.example.authenticationservice.utils;

import com.example.authenticationservice.dtos.UserResponseDto;
import com.example.authenticationservice.exceptions.InvalidTokenException;
import com.example.authenticationservice.models.ServiceRegistry;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtils {
    @Autowired
    SecretKey secretKey;

    public String generateServiceToken(ServiceRegistry serviceRegistry) {

        Map<String, Object> claims = compactServiceClaims(serviceRegistry);
        claims.put("token_type", "serviceToken");

        return Jwts.builder()
                .claims(claims)
                .signWith(secretKey)
                .compact();
    }

    public String generateAccessToken(UserResponseDto user) {
        Long accessTokenValidity = 6 * 60 * 60 * 1000L;

        Map<String, Object> claims = compactClaims(user, accessTokenValidity);
        claims.put("token_type", "AccessToken");

        return Jwts.builder()
                .claims(claims)
                .signWith(secretKey)
                .compact();
    }

    public String generateRefreshToken(UserResponseDto user) {

        Long refreshTokenValidity = 7 * 24 * 60 * 60 * 1000L;

        Map<String, Object> claims = compactClaims(user, refreshTokenValidity);
        claims.put("token_type", "RefreshToken");

        return Jwts.builder()
                .claims(claims)
                .signWith(secretKey)
                .compact();
    }

    public Boolean validateToken(String token_type, String token, UserResponseDto user) {
        Claims claims = getClaimsFromToken(token);
//        if (!claims.get("user_id").equals(user.getId())) {
//            throw new InvalidTokenException("Invalid token user_id");
//        }
        if (!claims.get("email").equals(user.getEmail())) {
            throw new InvalidTokenException("Invalid token email");
        }
        if (!claims.get("token_type").equals(token_type)) {
            throw new InvalidTokenException("Invalid token token_type");
        }
        if (!claims.get("issuer").equals("shop.at.ecommerce")) {
            throw new InvalidTokenException("Invalid token issuer");
        }
        if (!claims.get("roles").equals("USER")) {
            throw new InvalidTokenException("Invalid token roles");
        }

        return !claims.getExpiration().before(new Date());
    }

    public Claims getClaimsFromToken(String token) {
        Claims claims = null;
        try {
            JwtParser parser = Jwts.parser().verifyWith(secretKey).build();
            claims = parser.parseSignedClaims(token).getPayload();
        } catch (Exception exception) {
//            throw new InvalidTokenException("Invalid token Signature");
            System.out.println(exception.getMessage());
        }

        return claims;
    }

    private Map<String, Object> compactClaims(UserResponseDto user, Long validityInMilliSeconds) {
        Long currentTimeMillis = System.currentTimeMillis();

        Map<String, Object> claims = new HashMap<>();
        claims.put("user_id", user.getId());
        claims.put("email", user.getEmail());
        claims.put("roles", "USER");
        claims.put("iat", currentTimeMillis);
        claims.put("exp", currentTimeMillis + validityInMilliSeconds);
        claims.put("issuer", "shop.at.ecommerce");

        return claims;
    }

    private Map<String, Object> compactServiceClaims(ServiceRegistry serviceRegistry) {

        Map<String, Object> claims = new HashMap<>();
//        claims.put("serviceId", serviceRegistry.getId());
        claims.put("serviceName", serviceRegistry.getServiceName());
        claims.put("iat", System.currentTimeMillis());
//        claims.put("exp", currentTimeMillis + validityInMilliSeconds);
        claims.put("issuer", "shop.at.ecommerce");

        return claims;
    }

    public Boolean validateServiceToken(String token, ServiceRegistry serviceRegistry) {
        Claims claims = getClaimsFromToken(token);

        if (!claims.get("serviceName").equals(serviceRegistry.getServiceName())) {
            throw new InvalidTokenException("Invalid serviceToken name");
        }
        if (!claims.get("token_type").equals("serviceToken")) {
            throw new InvalidTokenException("Invalid token token_type");
        }
        if (!claims.get("issuer").equals("shop.at.ecommerce")) {
            throw new InvalidTokenException("Invalid token issuer");
        }

        return true;
    }
}
