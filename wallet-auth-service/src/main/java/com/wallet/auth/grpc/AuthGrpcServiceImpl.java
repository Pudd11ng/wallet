package com.wallet.auth.grpc;

import com.wallet.auth.entity.User;
import com.wallet.auth.mapper.UserMapper;
import com.wallet.common.grpc.AuthServiceGrpcApiGrpc;
import com.wallet.common.grpc.UserRequest;
import com.wallet.common.grpc.UserResponse;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class AuthGrpcServiceImpl extends AuthServiceGrpcApiGrpc.AuthServiceGrpcApiImplBase {

    private final UserMapper userMapper;

    @Override
    public void getUserById(UserRequest request, StreamObserver<UserResponse> responseObserver) {
        log.info("Received gRPC request to fetch username for User ID: {}", request.getUserId());

        try {
            // 1. Fetch the user directly from the database using the new ID lookup
            User user = userMapper.findById(request.getUserId())
                    .orElseThrow(() -> {
                        log.warn("gRPC User lookup failed: ID {} not found in database", request.getUserId());
                        // Return a standard gRPC 404 NOT FOUND error over the wire
                        return Status.NOT_FOUND
                                .withDescription("User not found with ID: " + request.getUserId())
                                .asRuntimeException();
                    });

            // 2. Build the Protobuf Response using real database values
            UserResponse response = UserResponse.newBuilder()
                    .setUserId(user.id())
                    .setUsername(user.username())
                    .build();

            // 3. Send the real data back over the HTTP/2 stream
            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            // Catch any unexpected database errors and return a gRPC INTERNAL error
            log.error("Error during gRPC user lookup", e);
            responseObserver.onError(
                    Status.INTERNAL.withDescription(e.getMessage()).withCause(e).asRuntimeException()
            );
        }
    }
}