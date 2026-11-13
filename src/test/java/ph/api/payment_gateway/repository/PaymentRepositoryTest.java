package ph.api.payment_gateway.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import ph.api.payment_gateway.entity.Payment;
import ph.api.payment_gateway.entity.enums.PaymentStatus;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class PaymentRepositoryTest {

    @Autowired
    private PaymentRepository paymentRepository;

    @Test
    void savesPaymentAndFindsItByExternalReference() {
        Payment payment = new Payment();
        payment.setReferenceId("REF-1");
        payment.setAmount(new BigDecimal("150.00"));
        payment.setCurrency("PHP");
        payment.setStatus(PaymentStatus.PENDING);
        payment.setExternalReference("EXT-1");

        Payment saved = paymentRepository.saveAndFlush(payment);
        Optional<Payment> found = paymentRepository.findByExternalReference("EXT-1");

        assertThat(saved.getId()).isNotNull();
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
        assertThat(found.get().getReferenceId()).isEqualTo("REF-1");
        assertThat(found.get().getAmount()).isEqualByComparingTo("150.00");
        assertThat(found.get().getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(found.get().getCreated_at()).isNotNull();
    }

    @Test
    void findByExternalReferenceReturnsEmptyWhenPaymentDoesNotExist() {
        Optional<Payment> found = paymentRepository.findByExternalReference("EXT-404");

        assertThat(found).isEmpty();
    }
}
