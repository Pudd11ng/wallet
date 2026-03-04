package com.wallet.core.aspect;

import com.wallet.common.exception.WalletBusinessException;
import com.wallet.core.service.IdempotencyService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotencyAspectTest {

    @Mock
    private IdempotencyService idempotencyService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private ServletRequestAttributes attributes;

    @InjectMocks
    private IdempotencyAspect idempotencyAspect;

    private MockedStatic<RequestContextHolder> mockedRequestContextHolder;

    @BeforeEach
    void setUp() {
        mockedRequestContextHolder = mockStatic(RequestContextHolder.class);
    }

    @AfterEach
    void tearDown() {
        mockedRequestContextHolder.close();
    }

    @Test
    void aroundAdvice_WhenRequestNotProcessed_ProceedsWithExecution() {
        // Arrange
        String requestId = "req-123";
        mockedRequestContextHolder.when(RequestContextHolder::getRequestAttributes).thenReturn(attributes);
        when(attributes.getRequest()).thenReturn(request);
        when(request.getHeader("X-Request-ID")).thenReturn(requestId);

        // Act
        // The aspect uses @Before rather than @Around, so we call the check directly.
        idempotencyAspect.checkIdempotency();

        // Assert
        verify(idempotencyService).checkAndLock(requestId);
    }

    @Test
    void aroundAdvice_WhenHeaderMissing_ThrowsException() {
        // Arrange
        mockedRequestContextHolder.when(RequestContextHolder::getRequestAttributes).thenReturn(attributes);
        when(attributes.getRequest()).thenReturn(request);
        when(request.getHeader("X-Request-ID")).thenReturn(null);

        // Act & Assert
        assertThatThrownBy(() -> idempotencyAspect.checkIdempotency())
                .isInstanceOf(WalletBusinessException.class)
                .hasMessage("Missing required header: X-Request-ID");

        verifyNoInteractions(idempotencyService);
    }
}
