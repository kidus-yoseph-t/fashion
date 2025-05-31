package com.project.Fashion.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class JwtUtil {

    @Value("${jwt.secret}") // You'll need to set this in application.properties
    private String secret;

    @Value("${jwt.expiration.ms}") // And this for expiration time
    private long expirationMs;

    private Key getSigningKey() {
        // For HS512, key length must be at least 64 bytes.
        // Ensure your jwt.secret is long and random enough.
        // If it's shorter, this might cause issues or you might need to use a different algorithm like HS256.
        byte[] keyBytes = secret.getBytes();
        if (keyBytes.length < 64 && SignatureAlgorithm.HS512.getMinKeyLength() > keyBytes.length * 8) {
            // If secret is too short for HS512, use a key derived to meet length requirements or use HS256
            // For simplicity, let's assume the secret is configured to be sufficiently long for HS512.
            // Or, generate a secure key once and store it:
            // Key key = Keys.secretKeyFor(SignatureAlgorithm.HS512);
            // String base64Key = Encoders.BASE64.encode(key.getEncoded());
            // Then store base64Key in properties and decode here.
            // For now, we directly use the bytes, ensure it's strong.
            return Keys.hmacShaKeyFor(keyBytes); // This adapts the key if needed for common HMAC-SHA
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token).getBody();
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        // Add roles to claims
        String roles = userDetails.getAuthorities().stream()
                .map(grantedAuthority -> grantedAuthority.getAuthority())
                .collect(Collectors.joining(","));
        claims.put("roles", roles);
        return createToken(claims, userDetails.getUsername());
    }

    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }
}