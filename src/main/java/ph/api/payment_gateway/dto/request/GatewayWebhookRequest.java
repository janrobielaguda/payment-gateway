package ph.api.payment_gateway.dto.request;

import lombok.Data;
import ph.api.payment_gateway.entity.enums.PaymentStatus;

@Data
public class GatewayWebhookRequest {
    private String transactionId;
    private String externalReference;
    private PaymentStatus status;
}
