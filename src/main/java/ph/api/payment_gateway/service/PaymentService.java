package ph.api.payment_gateway.service;

import ph.api.payment_gateway.dto.request.CreatePaymentRequest;
import ph.api.payment_gateway.dto.request.GatewayWebhookRequest;
import ph.api.payment_gateway.dto.response.PaymentResponse;

import java.util.UUID;

public interface PaymentService {
    PaymentResponse payment(CreatePaymentRequest createPaymentRequest);
    PaymentResponse processPayment(UUID paymentId);
    void handleGatewayCallback(GatewayWebhookRequest request);
}
