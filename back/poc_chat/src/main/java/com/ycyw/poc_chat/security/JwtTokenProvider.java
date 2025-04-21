package com.ycyw.poc_chat.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import com.ycyw.poc_chat.model.Role;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

/**
 * Génère et valide les JSON Web Tokens (JWT)
 */
@Component
public class JwtTokenProvider {

  private final SecretKey secretKey;
  private final long jwtExpirationMs;

  public JwtTokenProvider(
    @org.springframework.beans.factory.annotation.Value(
      "${jwt.secret}"
    ) String jwtSecret,
    @org.springframework.beans.factory.annotation.Value(
      "${jwt.expiration-ms}"
    ) long jwtExpirationMs
  ) {
    // Crée une clé HMAC à partir de la chaîne de caractères
    this.secretKey =
      Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    this.jwtExpirationMs = jwtExpirationMs;
  }

  /**
   * Génère un token JWT contenant l'ID et le rôle de l'utilisateur,
   * signé avec la clé HMAC et l'algorithme HS512.
   *
   * @param userId l'ID de l'utilisateur
   * @param role rôle de l'utilisateur (USER, AGENT, ADMIN)
   * @return le token JWT signé
   */
  public String generateToken(Long userId, Role role) {
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

    return Jwts
      .builder()
      .setSubject(userId.toString())
      .claim("role", role)
      .setIssuedAt(now)
      .setExpiration(expiryDate)
      .signWith(secretKey, SignatureAlgorithm.HS512)
      .compact();
  }

  /**
   * Récupère l'ID utilisateur depuis le token JWT.
   *
   * @param token le JWT
   * @return l'ID utilisateur
   */
  public Long getUserIdFromToken(String token) {
    Claims claims = Jwts
      .parserBuilder()
      .setSigningKey(secretKey)
      .build()
      .parseClaimsJws(token)
      .getBody();
    return Long.valueOf(claims.getSubject());
  }

  /**
   * Récupère le rôle depuis le token JWT.
   *
   * @param token le JWT
   * @return rôle de l'utilisateur
   */
  public String getRoleFromToken(String token) {
    Claims claims = Jwts
      .parserBuilder()
      .setSigningKey(secretKey)
      .build()
      .parseClaimsJws(token)
      .getBody();
    return claims.get("role", String.class);
  }

  /**
   * Valide la structure, la signature et l'expiration d'un token JWT.
   *
   * @param token le JWT
   * @return true si valide, false sinon
   */
  public boolean validateToken(String token) {
    try {
      Jwts
        .parserBuilder()
        .setSigningKey(secretKey)
        .build()
        .parseClaimsJws(token);
      return true;
    } catch (JwtException | IllegalArgumentException ex) {
      // Ici on peut logger la raison de l'échec : ex.getMessage()
      return false;
    }
  }
}
