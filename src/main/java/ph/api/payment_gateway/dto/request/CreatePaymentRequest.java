package ph.api.payment_gateway.dto.request;

import java.math.BigDecimal;
import java.util.UUID;

public record CreatePaymentRequest(
        UUID id,
        BigDecimal amount,
        String currency,
        String referenceId
) {
}
