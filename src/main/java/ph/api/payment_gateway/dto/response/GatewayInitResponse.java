package ph.api.payment_gateway.dto.response;

import lombok.Data;

@Data
public class GatewayInitResponse {
    private String externalReference;
    private String CheckoutUrl;
}
