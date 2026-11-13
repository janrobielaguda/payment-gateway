package ph.api.payment_gateway.controller;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ph.api.payment_gateway.dto.request.CreatePaymentRequest;
import ph.api.payment_gateway.dto.request.GatewayWebhookRequest;
import ph.api.payment_gateway.dto.response.PaymentResponse;
import ph.api.payment_gateway.entity.enums.PaymentStatus;
import ph.api.payment_gateway.service.PaymentService;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    @Test
    void getPaymentReturnsPayment() throws Exception {
        UUID paymentId = UUID.randomUUID();
        PaymentResponse response = new PaymentResponse(
                paymentId,
                "EXT-1",
                new BigDecimal("150.00"),
                PaymentStatus.SUCCESS,
                null
        );
        when(paymentService.getPayment(paymentId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/payment/{id}", paymentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(paymentId.toString()))
                .andExpect(jsonPath("$.externalReference").value("EXT-1"))
                .andExpect(jsonPath("$.amount").value(150.00))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.checkoutUrl").doesNotExist());

        verify(paymentService).getPayment(paymentId);
    }

    @Test
    void createPaymentReturnsCreatedPayment() throws Exception {
        UUID paymentId = UUID.randomUUID();
        PaymentResponse response = new PaymentResponse(
                paymentId,
                null,
                new BigDecimal("150.00"),
                PaymentStatus.PENDING,
                "http://localhost:8080/api/v1/payment/" + paymentId
        );
        when(paymentService.payment(any(CreatePaymentRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount": 150.00,
                                  "currency": "PHP",
                                  "referenceId": "REF-1"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentId").value(paymentId.toString()))
                .andExpect(jsonPath("$.amount").value(150.00))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.checkoutUrl")
                        .value("http://localhost:8080/api/v1/payment/" + paymentId));

        ArgumentCaptor<CreatePaymentRequest> captor = ArgumentCaptor.forClass(CreatePaymentRequest.class);
        verify(paymentService).payment(captor.capture());

        CreatePaymentRequest captured = captor.getValue();
        assertThat(captured.amount()).isEqualByComparingTo("150.00");
        assertThat(captured.currency()).isEqualTo("PHP");
        assertThat(captured.referenceId()).isEqualTo("REF-1");
    }

    @Test
    void processPaymentReturnsCheckoutUrl() throws Exception {
        UUID paymentId = UUID.randomUUID();
        PaymentResponse response = new PaymentResponse(
                paymentId,
                "EXT-1",
                new BigDecimal("150.00"),
                PaymentStatus.PENDING,
                "https://fake-gateway/pay/EXT-1"
        );
        when(paymentService.processPayment(paymentId)).thenReturn(response);

        mockMvc.perform(post("/api/v1/payment/{id}/pay", paymentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(paymentId.toString()))
                .andExpect(jsonPath("$.externalReference").value("EXT-1"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.checkoutUrl").value("https://fake-gateway/pay/EXT-1"));

        verify(paymentService).processPayment(paymentId);
    }

    @Test
    void handleWebhookDelegatesToService() throws Exception {
        mockMvc.perform(post("/api/v1/payment/webhook/payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "transactionId": "TXN-1",
                                  "externalReference": "EXT-1",
                                  "status": "SUCCESS"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        ArgumentCaptor<GatewayWebhookRequest> captor = ArgumentCaptor.forClass(GatewayWebhookRequest.class);
        verify(paymentService).handleGatewayCallback(captor.capture());

        GatewayWebhookRequest captured = captor.getValue();
        assertThat(captured.transactionId()).isEqualTo("TXN-1");
        assertThat(captured.externalReference()).isEqualTo("EXT-1");
        assertThat(captured.status()).isEqualTo(PaymentStatus.SUCCESS);
    }

    @Test
    void simulateSuccessSendsSuccessfulWebhookForExternalReference() throws Exception {
        mockMvc.perform(post("/api/v1/payment/simulate/success")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "transactionId": "ignored",
                                  "externalReference": "EXT-1",
                                  "status": "PENDING"
                                }
                                """))
                .andExpect(status().isOk());

        ArgumentCaptor<GatewayWebhookRequest> captor = ArgumentCaptor.forClass(GatewayWebhookRequest.class);
        verify(paymentService).handleGatewayCallback(captor.capture());

        GatewayWebhookRequest captured = captor.getValue();
        assertThat(captured.transactionId()).startsWith("TXN-");
        assertThat(captured.externalReference()).isEqualTo("EXT-1");
        assertThat(captured.status()).isEqualTo(PaymentStatus.SUCCESS);
    }
}
