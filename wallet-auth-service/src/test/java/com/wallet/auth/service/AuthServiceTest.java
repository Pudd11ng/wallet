package com.wallet.auth.service;

import com.wallet.auth.entity.User;
import com.wallet.auth.mapper.UserMapper;
import com.wallet.common.dto.AuthResponseDTO;
import com.wallet.common.dto.LoginRequestDTO;
import com.wallet.common.dto.RegisterRequestDTO;
import com.wallet.common.exception.WalletBusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    // A valid Base64 encoded secret for JWT (at least 256 bits)
    private static final String SECRET_BASE64 = "MTIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTI=";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "secret", SECRET_BASE64);
    }

    @Test
    void registerUser_WhenNewValidUser_ReturnsAuthResponseAndInsertCalled() {
        // Arrange
        RegisterRequestDTO request = new RegisterRequestDTO("newuser", "Password123!");

        when(userMapper.findByUsername(request.username())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(request.password())).thenReturn("encodedPassword");

        // Act
        AuthResponseDTO response = authService.register(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.userId()).startsWith("U-");
        assertThat(response.token()).isNotBlank();
        assertThat(response.message()).isEqualTo("Registration successful");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insertUser(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertThat(savedUser.username()).isEqualTo("newuser");
        assertThat(savedUser.passwordHash()).isEqualTo("encodedPassword");
    }

    @Test
    void registerUser_WhenUsernameExists_ThrowsException() {
        // Arrange
        RegisterRequestDTO request = new RegisterRequestDTO("existinguser", "Password123!");

        User existingUser = new User("U-EXIST", "existinguser", "hash", null);
        when(userMapper.findByUsername(request.username())).thenReturn(Optional.of(existingUser));

        // Act & Assert
        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(WalletBusinessException.class)
                .hasMessage("Username already exists");

        verify(userMapper, never()).insertUser(any());
    }

    @Test
    void loginUser_WhenValidCredentials_ReturnsAuthResponse() {
        // Arrange
        LoginRequestDTO request = new LoginRequestDTO("testuser", "CorrectPassword");
        User mockUser = new User("U-12345", "testuser", "hashedPassword", null);

        when(userMapper.findByUsername(request.username())).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches(request.password(), mockUser.passwordHash())).thenReturn(true);

        // Act
        AuthResponseDTO response = authService.login(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.userId()).isEqualTo("U-12345");
        assertThat(response.token()).isNotBlank();
        assertThat(response.message()).isEqualTo("Login successful");
    }

    @Test
    void loginUser_WhenUserNotFound_ThrowsException() {
        // Arrange
        LoginRequestDTO request = new LoginRequestDTO("unknownuser", "password");

        when(userMapper.findByUsername(request.username())).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(WalletBusinessException.class)
                .hasMessage("Invalid username or password");
    }

    @Test
    void loginUser_WhenInvalidPassword_ThrowsException() {
        // Arrange
        LoginRequestDTO request = new LoginRequestDTO("testuser", "WrongPassword");
        User mockUser = new User("U-12345", "testuser", "hashedPassword", null);

        when(userMapper.findByUsername(request.username())).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches(request.password(), mockUser.passwordHash())).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(WalletBusinessException.class)
                .hasMessage("Invalid username or password");
    }
}
