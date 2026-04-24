package ph.api.payment_gateway.dto.request;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class CreatePaymentRequest {
    private UUID id;
    private BigDecimal amount;
    private String currency;
    private String referenceId;
}
