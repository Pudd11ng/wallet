// wallet-service-core/src/main/java/com/wallet/core/strategy/QrStrategyFactory.java
package com.wallet.core.strategy;

import com.wallet.common.enums.QrFormat;
import com.wallet.common.exception.WalletBusinessException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class QrStrategyFactory {

    private final Map<QrFormat, QrProcessingStrategy> strategies;

    // Spring magically injects ALL classes that implement QrProcessingStrategy into this list!
    public QrStrategyFactory(List<QrProcessingStrategy> strategyList) {
        // We map them by their QrFormat enum so we can look them up instantly in O(1) time
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(QrProcessingStrategy::getSupportedFormat, Function.identity()));
    }

    public QrProcessingStrategy getStrategy(QrFormat format) {
        QrProcessingStrategy strategy = strategies.get(format);
        if (strategy == null) {
            throw new WalletBusinessException("Unsupported QR Format: " + format);
        }
        return strategy;
    }
}
