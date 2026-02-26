package com.wallet.auth.service;

import com.wallet.auth.entity.User;
import com.wallet.auth.mapper.UserMapper;
import com.wallet.common.dto.AuthResponseDTO;
import com.wallet.common.dto.LoginRequestDTO;
import com.wallet.common.dto.RegisterRequestDTO;
import com.wallet.common.exception.WalletBusinessException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Key;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    // This MUST exactly match the secret in your Gateway's JwtUtil!
    private static final String SECRET = "5367566B59703373367639792F423F4528482B4D6251655468576D5A71347437";

    @Transactional
    public AuthResponseDTO register(RegisterRequestDTO request) {
        log.info("Attempting to register user: {}", request.username());

        // 1. Prevent duplicate usernames
        if (userMapper.findByUsername(request.username()).isPresent()) {
            throw new WalletBusinessException("Username already exists");
        }

        // 2. Generate a User ID
        String userId = "U-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // 3. Hash the password with BCrypt!
        String hashedPassword = passwordEncoder.encode(request.password());

        // 4. Save to DB
        User newUser = new User(userId, request.username(), hashedPassword, LocalDateTime.now());
        userMapper.insertUser(newUser);

        // 5. Generate their first JWT
        String token = generateToken(userId);
        return new AuthResponseDTO(userId, token, "Registration successful");
    }

    public AuthResponseDTO login(LoginRequestDTO request) {
        log.info("Login attempt for user: {}", request.username());

        // 1. Find user
        User user = userMapper.findByUsername(request.username())
                .orElseThrow(() -> new WalletBusinessException("Invalid username or password"));

        // 2. Verify the BCrypt hash mathematically
        if (!passwordEncoder.matches(request.password(), user.passwordHash())) {
            throw new WalletBusinessException("Invalid username or password");
        }

        // 3. Issue fresh JWT
        String token = generateToken(user.id());
        return new AuthResponseDTO(user.id(), token, "Login successful");
    }

    private String generateToken(String userId) {
        byte[] keyBytes = io.jsonwebtoken.io.Decoders.BASE64.decode(SECRET);
        Key key = Keys.hmacShaKeyFor(keyBytes);

        return Jwts.builder()
                .setSubject(userId)
                .setIssuedAt(new java.util.Date())
                .setExpiration(new java.util.Date(System.currentTimeMillis() + 86400000)) // 24 hours
                .signWith(key)
                .compact();
    }
}