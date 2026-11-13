package ph.api.payment_gateway.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ph.api.payment_gateway.dto.request.CreatePaymentRequest;
import ph.api.payment_gateway.dto.request.GatewayWebhookRequest;
import ph.api.payment_gateway.dto.response.GatewayInitResponse;
import ph.api.payment_gateway.dto.response.PaymentResponse;
import ph.api.payment_gateway.entity.Payment;
import ph.api.payment_gateway.entity.PaymentTransaction;
import ph.api.payment_gateway.entity.enums.PaymentStatus;
import ph.api.payment_gateway.entity.enums.TransactionStatus;
import ph.api.payment_gateway.repository.PaymentRepository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private GatewayService gatewayService;

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @Test
    void getPaymentReturnsMappedResponse() {
        UUID paymentId = UUID.randomUUID();
        Payment payment = payment(paymentId, PaymentStatus.SUCCESS);
        payment.setExternalReference("EXT-1");
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        PaymentResponse response = paymentService.getPayment(paymentId);

        assertThat(response.paymentId()).isEqualTo(paymentId);
        assertThat(response.externalReference()).isEqualTo("EXT-1");
        assertThat(response.amount()).isEqualByComparingTo("150.00");
        assertThat(response.status()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(response.checkoutUrl()).isNull();
    }

    @Test
    void getPaymentThrowsWhenPaymentDoesNotExist() {
        UUID paymentId = UUID.randomUUID();
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getPayment(paymentId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining(paymentId.toString());
    }

    @Test
    void paymentCreatesPendingPayment() {
        CreatePaymentRequest request = new CreatePaymentRequest(
                null,
                new BigDecimal("150.00"),
                "PHP",
                "REF-1"
        );
        UUID paymentId = UUID.randomUUID();
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment saved = invocation.getArgument(0);
            saved.setId(paymentId);
            return saved;
        });

        PaymentResponse response = paymentService.payment(request);

        assertThat(response.paymentId()).isEqualTo(paymentId);
        assertThat(response.amount()).isEqualByComparingTo("150.00");
        assertThat(response.status()).isEqualTo(PaymentStatus.PENDING);
        assertThat(response.checkoutUrl())
                .isEqualTo("http://localhost:8080/api/v1/payment/" + paymentId);

        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void processPaymentInitiatesGatewayAndReturnsCheckoutUrl() {
        UUID paymentId = UUID.randomUUID();
        Payment payment = payment(paymentId, PaymentStatus.PENDING);
        GatewayInitResponse gatewayResponse = new GatewayInitResponse(
                "EXT-1",
                "https://fake-gateway/pay/EXT-1"
        );
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(gatewayService.initiate(payment)).thenReturn(gatewayResponse);
        when(paymentRepository.save(payment)).thenReturn(payment);

        PaymentResponse response = paymentService.processPayment(paymentId);

        assertThat(response.paymentId()).isEqualTo(paymentId);
        assertThat(response.externalReference()).isEqualTo("EXT-1");
        assertThat(response.checkoutUrl()).isEqualTo("https://fake-gateway/pay/EXT-1");
        assertThat(response.status()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getExternalReference()).isEqualTo("EXT-1");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);

        verify(gatewayService).initiate(payment);
        verify(paymentRepository).save(payment);
    }

    @Test
    void processPaymentThrowsWhenPaymentAlreadySucceeded() {
        UUID paymentId = UUID.randomUUID();
        Payment payment = payment(paymentId, PaymentStatus.SUCCESS);
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.processPayment(paymentId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Payment already processed");

        verifyNoInteractions(gatewayService);
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void processPaymentThrowsWhenPaymentDoesNotExist() {
        UUID paymentId = UUID.randomUUID();
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.processPayment(paymentId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining(paymentId.toString());

        verifyNoInteractions(gatewayService);
    }

    @Test
    void handleGatewayCallbackMarksSuccessfulPaymentAndCompletesTransaction() {
        Payment payment = payment(UUID.randomUUID(), PaymentStatus.PENDING);
        payment.setExternalReference("EXT-1");
        GatewayWebhookRequest request = new GatewayWebhookRequest(
                "TXN-1",
                "EXT-1",
                PaymentStatus.SUCCESS
        );
        when(paymentRepository.findByExternalReference("EXT-1")).thenReturn(Optional.of(payment));
        when(paymentRepository.save(payment)).thenReturn(payment);

        paymentService.handleGatewayCallback(request);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(payment.getTransactions()).hasSize(1);

        PaymentTransaction transaction = payment.getTransactions().get(0);
        assertThat(transaction.getReferenceNumber()).isEqualTo("TXN-1");
        assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(transaction.getPayment()).isSameAs(payment);

        verify(paymentRepository).save(payment);
    }

    @Test
    void handleGatewayCallbackMarksFailedPaymentAndFailsTransaction() {
        Payment payment = payment(UUID.randomUUID(), PaymentStatus.PENDING);
        payment.setExternalReference("EXT-1");
        GatewayWebhookRequest request = new GatewayWebhookRequest(
                "TXN-2",
                "EXT-1",
                PaymentStatus.FAILED
        );
        when(paymentRepository.findByExternalReference("EXT-1")).thenReturn(Optional.of(payment));
        when(paymentRepository.save(payment)).thenReturn(payment);

        paymentService.handleGatewayCallback(request);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getTransactions()).hasSize(1);
        assertThat(payment.getTransactions().get(0).getStatus()).isEqualTo(TransactionStatus.FAILED);
        assertThat(payment.getTransactions().get(0).getReferenceNumber()).isEqualTo("TXN-2");

        verify(paymentRepository).save(payment);
    }

    @Test
    void handleGatewayCallbackThrowsWhenPaymentDoesNotExist() {
        GatewayWebhookRequest request = new GatewayWebhookRequest(
                "TXN-1",
                "EXT-404",
                PaymentStatus.SUCCESS
        );
        when(paymentRepository.findByExternalReference("EXT-404")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.handleGatewayCallback(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("EXT-404");

        verify(paymentRepository, never()).save(any(Payment.class));
    }

    private Payment payment(UUID paymentId, PaymentStatus status) {
        Payment payment = new Payment();
        payment.setId(paymentId);
        payment.setReferenceId("REF-1");
        payment.setAmount(new BigDecimal("150.00"));
        payment.setCurrency("PHP");
        payment.setStatus(status);
        return payment;
    }
}
