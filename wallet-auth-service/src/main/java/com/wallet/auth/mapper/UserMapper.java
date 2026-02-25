package com.wallet.auth.mapper;

import com.wallet.auth.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.Optional;

@Mapper


public interface UserMapper {
    void insertUser(User user);
    Optional<User> findByUsername(@Param("username") String username);
    Optional<User> findById(@Param("id") String id);
}
