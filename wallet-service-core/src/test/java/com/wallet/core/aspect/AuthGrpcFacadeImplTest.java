package com.wallet.core.facade.impl;

import com.wallet.common.exception.WalletBusinessException;
import com.wallet.common.grpc.AuthServiceGrpcApiGrpc;
import com.wallet.common.grpc.UserRequest;
import com.wallet.common.grpc.UserResponse;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthGrpcFacadeImplTest {

    @Mock
    private AuthServiceGrpcApiGrpc.AuthServiceGrpcApiBlockingStub authBlockingStub;

    @InjectMocks
    private AuthGrpcFacadeImpl authGrpcFacade;

    @Test
    void fetchUsername_WhenGrpcCallSucceeds_ReturnsUsername() {
        // Arrange
        String userId = "user-123";
        String expectedUsername = "john_doe";

        UserResponse mockResponse = UserResponse.newBuilder()
                .setUsername(expectedUsername)
                .build();

        // Inject the stub via ReflectionTestUtils, matching the @GrpcClient behavior
        ReflectionTestUtils.setField(authGrpcFacade, "authBlockingStub", authBlockingStub);

        when(authBlockingStub.getUserById(any(UserRequest.class))).thenReturn(mockResponse);

        // Act
        String actualUsername = authGrpcFacade.fetchUsername(userId);

        // Assert
        assertThat(actualUsername).isEqualTo(expectedUsername);
    }

    @Test
    void fetchUsername_WhenGrpcCallFails_ThrowsException() {
        // Arrange
        String userId = "user-123";
        StatusRuntimeException exception = new StatusRuntimeException(
                Status.NOT_FOUND.withDescription("User mapping not found in IDP"));

        ReflectionTestUtils.setField(authGrpcFacade, "authBlockingStub", authBlockingStub);

        when(authBlockingStub.getUserById(any(UserRequest.class))).thenThrow(exception);

        // Act & Assert
        assertThatThrownBy(() -> authGrpcFacade.fetchUsername(userId))
                .isInstanceOf(WalletBusinessException.class)
                .hasMessageContaining("Could not verify user identity");
    }
}
