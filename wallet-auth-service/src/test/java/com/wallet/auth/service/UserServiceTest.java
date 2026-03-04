package com.wallet.auth.service;

import com.wallet.auth.entity.User;
import com.wallet.auth.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    @Test
    void getUserById_WhenUserExists_ReturnsUser() {
        // Arrange
        String userId = "U-12345678";
        User mockUser = new User(userId, "testuser", "hashedpass", null);
        when(userMapper.findById(userId)).thenReturn(Optional.of(mockUser));

        // Act
        User actualUser = userService.getUserById(userId);

        // Assert
        assertThat(actualUser).isNotNull();
        assertThat(actualUser.id()).isEqualTo(userId);
        assertThat(actualUser.username()).isEqualTo("testuser");
        verify(userMapper).findById(userId);
    }

    @Test
    void getUserById_WhenUserNotFound_ReturnsNull() {
        // Arrange
        String userId = "U-NOTFOUND";
        when(userMapper.findById(userId)).thenReturn(Optional.empty());

        // Act
        User actualUser = userService.getUserById(userId);

        // Assert
        assertThat(actualUser).isNull();
        verify(userMapper).findById(userId);
    }
}
