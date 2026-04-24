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
    public PaymentResponse payment(CreatePaymentRequest createPaymentRequest) {

        Payment payment = new Payment();
        payment.setReferenceId(createPaymentRequest.getReferenceId());
        payment.setAmount(createPaymentRequest.getAmount());
        payment.setCurrency(createPaymentRequest.getCurrency());
        payment.setStatus(PaymentStatus.PENDING);

        Payment savedPayment = paymentRepository.save(payment);

        PaymentResponse paymentResponse = new PaymentResponse();
        paymentResponse.setPaymentId(savedPayment.getId());
        paymentResponse.setStatus(savedPayment.getStatus());
        
        return paymentResponse;
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

        payment.setExternalReference(gateway.getExternalReference());
        payment.setStatus(PaymentStatus.PENDING);

        paymentRepository.save(payment);

        PaymentResponse paymentResponse = new PaymentResponse();
        paymentResponse.setExternalReference(payment.getExternalReference());
        paymentResponse.setStatus(payment.getStatus());
        paymentResponse.setCheckoutUrl(gateway.getCheckoutUrl());

        return paymentResponse;
    }

    @Transactional
    public void handleGatewayCallback(GatewayWebhookRequest request) {

        Payment payment = paymentRepository
                .findByExternalReference(request.getExternalReference())
                .orElseThrow(() -> new RuntimeException("Payment not found with id: " + request.getExternalReference()));

        PaymentTransaction tx = new PaymentTransaction();
        tx.setReferenceNumber(request.getTransactionId());
        tx.setPayment(payment);

        if (request.getStatus() == PaymentStatus.SUCCESS) {
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
