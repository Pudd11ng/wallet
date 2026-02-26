package com.wallet.core.facade.impl;

import com.wallet.common.exception.WalletBusinessException;
import com.wallet.common.grpc.AuthServiceGrpcApiGrpc;
import com.wallet.common.grpc.UserRequest;
import com.wallet.common.grpc.UserResponse;
import com.wallet.core.facade.AuthFacade;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;

import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AuthGrpcFacadeImpl implements AuthFacade {

    @GrpcClient("auth-service")
    private AuthServiceGrpcApiGrpc.AuthServiceGrpcApiBlockingStub authBlockingStub;

    @Override
    public String fetchUsername(String userId) {
        log.debug("Initiating gRPC call to fetch username for ID: {}", userId);

        try {
            // 1. Build the Protobuf Request
            UserRequest request = UserRequest.newBuilder()
                    .setUserId(userId)
                    .build();

            // 2. Make the synchronous, binary RPC call!
            UserResponse response = authBlockingStub.getUserById(request);

            return response.getUsername();

        } catch (StatusRuntimeException e) {
            // If the Auth Service throws a NOT_FOUND or goes offline, we catch it here!
            log.error("gRPC call failed for User ID {}: {}", userId, e.getStatus().getDescription());
            throw new WalletBusinessException("Could not verify user identity: " + e.getStatus().getDescription());
        }
    }
}