package com.wallet.auth.grpc;

import com.wallet.auth.entity.User;
import com.wallet.auth.service.UserService;
import com.wallet.common.grpc.UserRequest;
import com.wallet.common.grpc.UserResponse;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class AuthGrpcServiceImplTest {

    @Mock
    private UserService userService;

    @Mock
    private StreamObserver<UserResponse> responseObserver;

    @InjectMocks
    private AuthGrpcServiceImpl authGrpcService;

    @Test
    void getUserById_WhenUserExists_CallsOnNextAndOnCompleted() {
        // Arrange
        String userId = "U-12345";
        UserRequest request = UserRequest.newBuilder()
                .setUserId(userId)
                .build();

        User mockUser = new User(userId, "john_doe", "hashed", null);
        when(userService.getUserById(userId)).thenReturn(mockUser);

        // Act
        authGrpcService.getUserById(request, responseObserver);

        // Assert
        ArgumentCaptor<UserResponse> responseCaptor = ArgumentCaptor.forClass(UserResponse.class);
        verify(responseObserver).onNext(responseCaptor.capture());
        verify(responseObserver).onCompleted();

        UserResponse sentResponse = responseCaptor.getValue();
        assertThat(sentResponse.getUserId()).isEqualTo(userId);
        assertThat(sentResponse.getUsername()).isEqualTo("john_doe");
    }

    @Test
    void getUserById_WhenUserNotFound_CallsOnError() {
        // Arrange
        String userId = "U-NOTFOUND";
        UserRequest request = UserRequest.newBuilder()
                .setUserId(userId)
                .build();

        when(userService.getUserById(userId)).thenReturn(null);

        // Act
        authGrpcService.getUserById(request, responseObserver);

        // Assert
        ArgumentCaptor<StatusRuntimeException> exceptionCaptor = ArgumentCaptor.forClass(StatusRuntimeException.class);
        verify(responseObserver).onError(exceptionCaptor.capture());

        StatusRuntimeException exception = exceptionCaptor.getValue();
        assertThat(exception.getStatus().getCode()).isEqualTo(Status.NOT_FOUND.getCode());
        assertThat(exception.getStatus().getDescription()).contains("User not found with ID: U-NOTFOUND");
    }
}
