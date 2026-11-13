package ph.api.payment_gateway.service.impl;

import org.springframework.stereotype.Service;
import ph.api.payment_gateway.dto.response.GatewayInitResponse;
import ph.api.payment_gateway.entity.Payment;


@Service
public class GatewayService {

    public GatewayInitResponse initiate(Payment payment) {

        String externalReference = "EXT-" + System.currentTimeMillis();

        return new GatewayInitResponse(
                externalReference,
                "https://fake-gateway/pay/" + externalReference
        );
    }
}
