package ph.api.payment_gateway.service.impl;

import org.springframework.stereotype.Service;
import ph.api.payment_gateway.dto.response.GatewayInitResponse;
import ph.api.payment_gateway.entity.Payment;


@Service
public class GatewayService {

    public GatewayInitResponse initiate(Payment payment) {

        GatewayInitResponse res = new GatewayInitResponse();

        res.setExternalReference("EXT-" + System.currentTimeMillis());
        res.setCheckoutUrl("https://fake-gateway/pay/" + res.getExternalReference());

        return res;
    }
}
