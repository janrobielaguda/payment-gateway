package ph.api.payment_gateway.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import ph.api.payment_gateway.entity.enums.PaymentStatus;

import java.math.BigDecimal;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PaymentResponse(
        UUID paymentId,
        String externalReference,
        BigDecimal amount,
        PaymentStatus status,
        String checkoutUrl
) {
}
