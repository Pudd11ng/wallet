package com.wallet.auth.service;

import com.wallet.auth.entity.User;
import com.wallet.auth.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;

    // THE MAGIC: If "users::U-123" is in Redis, Spring returns it instantly.
    // If not, it runs the SQL, saves it to Redis, and THEN returns it.
    @Cacheable(value = "users", key = "#id")
    public User getUserById(String id) {
        log.info("CACHE MISS! Fetching user {} directly from PostgreSQL...", id);
        return userMapper.findById(id).orElse(null);
    }
}