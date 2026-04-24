package ph.api.payment_gateway.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ph.api.payment_gateway.entity.Payment;
import ph.api.payment_gateway.entity.PaymentTransaction;

import java.util.UUID;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {
}
