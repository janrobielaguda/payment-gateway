package ph.api.payment_gateway.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ph.api.payment_gateway.entity.Payment;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByExternalReference(String externalReference);
}
