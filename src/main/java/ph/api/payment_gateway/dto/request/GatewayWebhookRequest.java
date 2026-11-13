package ph.api.payment_gateway.dto.request;

import ph.api.payment_gateway.entity.enums.PaymentStatus;

public record GatewayWebhookRequest(
        String transactionId,
        String externalReference,
        PaymentStatus status
) {
}
