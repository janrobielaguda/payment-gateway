package ph.api.payment_gateway.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ph.api.payment_gateway.dto.response.GatewayInitResponse;
import ph.api.payment_gateway.dto.request.CreatePaymentRequest;
import ph.api.payment_gateway.dto.request.GatewayWebhookRequest;
import ph.api.payment_gateway.dto.response.PaymentResponse;
import ph.api.payment_gateway.entity.Payment;
import ph.api.payment_gateway.entity.PaymentTransaction;
import ph.api.payment_gateway.entity.enums.PaymentStatus;
import ph.api.payment_gateway.entity.enums.TransactionStatus;
import ph.api.payment_gateway.repository.PaymentRepository;
import ph.api.payment_gateway.service.PaymentService;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    private final GatewayService gatewayService;
    private final PaymentRepository paymentRepository;

    @Override
    public PaymentResponse getPayment(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found with id: " + paymentId));

        return new PaymentResponse(
                payment.getId(),
                payment.getExternalReference(),
                payment.getAmount(),
                payment.getStatus(),
                null
        );
    }

    @Override
    public PaymentResponse payment(CreatePaymentRequest createPaymentRequest) {

        Payment payment = new Payment();
        payment.setReferenceId(createPaymentRequest.referenceId());
        payment.setAmount(createPaymentRequest.amount());
        payment.setCurrency(createPaymentRequest.currency());
        payment.setStatus(PaymentStatus.PENDING);

        Payment savedPayment = paymentRepository.save(payment);

        return new PaymentResponse(
                savedPayment.getId(),
                savedPayment.getExternalReference(),
                savedPayment.getAmount(),
                savedPayment.getStatus(),
                "http://localhost:8080/api/v1/payment/" + savedPayment.getId()
        );
    }

    @Transactional
    @Override
    public PaymentResponse processPayment(UUID paymentId) {

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found with id: " + paymentId));

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            throw new RuntimeException("Payment already processed");
        }

        GatewayInitResponse gateway = gatewayService.initiate(payment);

        payment.setExternalReference(gateway.externalReference());
        payment.setStatus(PaymentStatus.PENDING);

        paymentRepository.save(payment);

        return new PaymentResponse(
                payment.getId(),
                payment.getExternalReference(),
                payment.getAmount(),
                payment.getStatus(),
                gateway.checkoutUrl()
        );
    }

    @Transactional
    public void handleGatewayCallback(GatewayWebhookRequest request) {

        Payment payment = paymentRepository
                .findByExternalReference(request.externalReference())
                .orElseThrow(() -> new RuntimeException("Payment not found with id: " + request.externalReference()));

        PaymentTransaction tx = new PaymentTransaction();
        tx.setReferenceNumber(request.transactionId());
        tx.setPayment(payment);

        if (request.status() == PaymentStatus.SUCCESS) {
            payment.setStatus(PaymentStatus.SUCCESS);
            tx.setStatus(TransactionStatus.COMPLETED);
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            tx.setStatus(TransactionStatus.FAILED);
        }

        payment.getTransactions().add(tx);

        paymentRepository.save(payment);
    }


}
