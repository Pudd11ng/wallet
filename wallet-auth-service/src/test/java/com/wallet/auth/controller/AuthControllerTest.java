package com.wallet.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallet.auth.mapper.UserMapper;
import com.wallet.auth.service.AuthService;
import com.wallet.common.dto.AuthResponseDTO;
import com.wallet.common.dto.LoginRequestDTO;
import com.wallet.common.dto.RegisterRequestDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    // Infrastructure Mocks
    @MockitoBean
    private UserMapper userMapper;

    @MockitoBean
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void register_WhenValidPayload_Returns200Ok() throws Exception {
        RegisterRequestDTO request = new RegisterRequestDTO("testuser", "Password123!");
        AuthResponseDTO response = new AuthResponseDTO("U-123", "fake-jwt-token", "Registration successful");

        when(authService.register(any(RegisterRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("U-123"));
    }

    @Test
    void login_WhenValidPayload_Returns200Ok() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO("testuser", "Password123!");
        AuthResponseDTO response = new AuthResponseDTO("U-123", "fake-jwt-token", "Login successful");

        when(authService.login(any(LoginRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("fake-jwt-token"));
    }

    @Test
    void register_WhenMissingUsername_Returns400BadRequest() throws Exception {
        RegisterRequestDTO request = new RegisterRequestDTO(null, "Password123!");

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
