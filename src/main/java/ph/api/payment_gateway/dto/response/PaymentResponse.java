package ph.api.payment_gateway.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import ph.api.payment_gateway.entity.enums.PaymentStatus;

import java.util.UUID;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentResponse {
    private UUID paymentId;
    private String externalReference;
    private PaymentStatus status;
    private String checkoutUrl;
}
