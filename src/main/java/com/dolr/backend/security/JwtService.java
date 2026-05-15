package com.dolr.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Service
public class JwtService {

	private static final String DEFAULT_SECRET =
			"dolrSuperSecretKeyDolrSuperSecretKey123456";

	@Value("${jwt.secret:dolrSuperSecretKeyDolrSuperSecretKey123456}")
	private String secret;

	private Key key;

	@PostConstruct
	void init() {
		String effective = secret == null || secret.isBlank() ? DEFAULT_SECRET : secret.trim();
		byte[] secretBytes = effective.getBytes(StandardCharsets.UTF_8);
		if (secretBytes.length < 32) {
			throw new IllegalStateException(
					"jwt.secret must be at least 32 characters for HS256. Set JWT_SECRET in the environment.");
		}
		key = Keys.hmacShaKeyFor(secretBytes);
	}

	public String generateToken(String email) {
		return Jwts.builder()
				.setSubject(email)
				.setIssuedAt(new Date())
				.setExpiration(new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24))
				.signWith(key, SignatureAlgorithm.HS256)
				.compact();
	}

	public String extractEmail(String token) {
		return extractClaims(token).getSubject();
	}

	public boolean isValid(String token, String email) {
		String extractedEmail = extractEmail(token);
		return extractedEmail.equals(email) && !isTokenExpired(token);
	}

	private boolean isTokenExpired(String token) {
		return extractClaims(token).getExpiration().before(new Date());
	}

	private Claims extractClaims(String token) {
		return Jwts.parserBuilder()
				.setSigningKey(key)
				.build()
				.parseClaimsJws(token)
				.getBody();
	}
}
