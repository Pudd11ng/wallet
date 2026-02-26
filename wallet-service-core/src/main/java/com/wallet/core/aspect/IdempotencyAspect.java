// wallet-service-core/src/main/java/com/wallet/core/aspect/IdempotencyAspect.java
package com.wallet.core.aspect;

import com.wallet.common.exception.WalletBusinessException;
import com.wallet.core.service.IdempotencyService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class IdempotencyAspect {

    private final IdempotencyService idempotencyService;

    // This tells Spring: "Run this exact method BEFORE any method annotated with @Idempotent"
    @Before("@annotation(com.wallet.core.annotation.Idempotent)")
    public void checkIdempotency() {
        // 1. Secretly reach into the current HTTP Request
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new WalletBusinessException("Cannot check idempotency outside of an HTTP request");
        }

        HttpServletRequest request = attributes.getRequest();

        // 2. Extract the header
        String requestId = request.getHeader("X-Request-ID");
        if (requestId == null || requestId.isBlank()) {
            throw new WalletBusinessException("Missing required header: X-Request-ID");
        }

        log.info("AOP Interceptor caught request. Checking Idempotency for ID: {}", requestId);

        // 3. Trigger the Redis lock
        idempotencyService.checkAndLock(requestId);
    }
}
