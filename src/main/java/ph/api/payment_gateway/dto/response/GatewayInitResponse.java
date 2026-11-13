package ph.api.payment_gateway.dto.response;

public record GatewayInitResponse(
        String externalReference,
        String checkoutUrl
) {
}
