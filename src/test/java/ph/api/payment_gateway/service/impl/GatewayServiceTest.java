package ph.api.payment_gateway.service.impl;

import org.junit.jupiter.api.Test;
import ph.api.payment_gateway.dto.response.GatewayInitResponse;
import ph.api.payment_gateway.entity.Payment;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayServiceTest {

    private final GatewayService gatewayService = new GatewayService();

    @Test
    void initiateReturnsExternalReferenceAndMatchingCheckoutUrl() {
        GatewayInitResponse response = gatewayService.initiate(new Payment());

        assertThat(response.externalReference()).startsWith("EXT-");
        assertThat(response.checkoutUrl())
                .isEqualTo("https://fake-gateway/pay/" + response.externalReference());
    }
}
