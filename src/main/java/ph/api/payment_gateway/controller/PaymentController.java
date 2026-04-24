package ph.api.payment_gateway.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.repository.query.Param;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ph.api.payment_gateway.dto.request.CreatePaymentRequest;
import ph.api.payment_gateway.dto.request.GatewayWebhookRequest;
import ph.api.payment_gateway.dto.response.PaymentResponse;
import ph.api.payment_gateway.entity.enums.PaymentStatus;
import ph.api.payment_gateway.service.PaymentService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(@RequestBody CreatePaymentRequest paymentRequest){
        PaymentResponse createdPayment = paymentService.payment(paymentRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdPayment);
    }

    @PostMapping("/{id}/pay")
    public ResponseEntity<PaymentResponse> processPayment(@PathVariable("id") UUID id){
        PaymentResponse processedPayment = paymentService.processPayment(id);
        return ResponseEntity.ok(processedPayment);
    }

    @PostMapping("/webhook/payment")
    public ResponseEntity<Void> handleWebhook(@RequestBody GatewayWebhookRequest request) {

        paymentService.handleGatewayCallback(request);

        return ResponseEntity.ok().build();
    }

    //FOR TESTING
    @PostMapping("/simulate/success")
    public void simulateSuccess(@RequestBody GatewayWebhookRequest externalRef) {

        GatewayWebhookRequest req = new GatewayWebhookRequest();
        req.setExternalReference(externalRef.getExternalReference());
        req.setStatus(PaymentStatus.SUCCESS);
        req.setTransactionId("TXN-" + System.currentTimeMillis());

        paymentService.handleGatewayCallback(req);
    }
}
