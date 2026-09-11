package com.hosannasolutions.solisla.whatsapp.providedService;

import com.hosannasolutions.solisla.order.Order;
import com.hosannasolutions.solisla.order.OrderItem;
import com.hosannasolutions.solisla.order.PaymentMethod;
import com.hosannasolutions.solisla.order.repository.OrderItemRepository;
import com.hosannasolutions.solisla.whatsapp.dto.WhatsAppSendResult;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Calls the WhatsApp Business Cloud API directly (Meta Graph API). The message is always rendered
 * here from a freshly-loaded {@link Order} — never trust a client-generated WhatsApp message
 * (design doc §21).
 */
@Service
public class WhatsAppServiceImpl implements WhatsAppService {

    private static final String GRAPH_API_BASE_URL = "https://graph.facebook.com/v20.0";

    private final OrderItemRepository orderItemRepository;
    private final RestClient restClient;
    private final String phoneNumberId;
    private final String accessToken;
    private final String businessRecipientNumber;

    public WhatsAppServiceImpl(OrderItemRepository orderItemRepository,
                                @Value("${sol-isla.whatsapp.phone-number-id}") String phoneNumberId,
                                @Value("${sol-isla.whatsapp.access-token}") String accessToken,
                                @Value("${sol-isla.whatsapp.business-recipient-number}") String businessRecipientNumber) {
        this.orderItemRepository = orderItemRepository;
        this.phoneNumberId = phoneNumberId;
        this.accessToken = accessToken;
        this.businessRecipientNumber = businessRecipientNumber;
        this.restClient = RestClient.builder().baseUrl(GRAPH_API_BASE_URL).build();
    }

    @Override
    public boolean isConfigured() {
        return isSet(phoneNumberId) && isSet(accessToken) && isSet(businessRecipientNumber);
    }

    @Override
    public WhatsAppSendResult sendOrderCreatedMessage(Order order) {
        if (!isConfigured()) {
            return WhatsAppSendResult.failure("WhatsApp is not configured (sol-isla.whatsapp.* is empty)");
        }
        String message = renderOrderCreatedMessage(order);
        try {
            SendMessageResponse response = restClient.post()
                    .uri("/{phoneNumberId}/messages", phoneNumberId)
                    .header("Authorization", "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new SendMessageRequest(businessRecipientNumber, new TextBody(message)))
                    .retrieve()
                    .body(SendMessageResponse.class);

            if (response == null || response.messages() == null || response.messages().isEmpty()) {
                return WhatsAppSendResult.failure("WhatsApp API returned no message id");
            }
            return WhatsAppSendResult.success(response.messages().get(0).id());
        } catch (RestClientException e) {
            return WhatsAppSendResult.failure(e.getMessage());
        }
    }

    /** Design doc §21's template, adapted to show the order's own currency rather than assuming
     *  "$" — this store's currency is admin-configured per product, not hardcoded. */
    String renderOrderCreatedMessage(Order order) {
        List<OrderItem> items = orderItemRepository.findByOrderIdOrderById(order.getId());
        StringBuilder sb = new StringBuilder();
        sb.append("New order: ").append(order.getOrderNumber()).append("\n\n");

        sb.append("Customer:\n").append(order.getCustomerName()).append('\n').append(order.getCustomerPhone()).append("\n\n");

        sb.append("Delivery:\n").append(order.getDeliveryAddress()).append('\n').append(order.getDeliveryCity());
        if (isSet(order.getDeliveryPostalCode())) {
            sb.append(' ').append(order.getDeliveryPostalCode());
        }
        sb.append('\n');
        if (isSet(order.getDeliveryNotes())) {
            sb.append("Notes: ").append(order.getDeliveryNotes()).append('\n');
        }
        sb.append('\n');

        sb.append("Items:\n");
        for (OrderItem item : items) {
            sb.append(item.getQuantity()).append(" x ").append(item.getProductNameSnapshot())
                    .append("   ").append(money(item.getLineTotal(), order.getCurrency())).append('\n');
        }
        sb.append('\n');

        sb.append("Subtotal:  ").append(money(order.getSubtotal(), order.getCurrency())).append('\n');
        if (order.getDiscountTotal().signum() > 0) {
            sb.append("Discount:  -").append(money(order.getDiscountTotal(), order.getCurrency())).append('\n');
        }
        sb.append("Delivery:  ").append(money(order.getDeliveryFee(), order.getCurrency())).append('\n');
        sb.append("Total:  ").append(money(order.getGrandTotal(), order.getCurrency())).append("\n\n");

        sb.append("Payment:\n").append(order.getPaymentMethod() == PaymentMethod.CARD ? "Card" : "Cash on Delivery").append("\n\n");

        sb.append("Order status:\nPending confirmation");

        return sb.toString();
    }

    private static String money(BigDecimal amount, String currency) {
        return amount.setScale(2, RoundingMode.HALF_UP) + " " + currency;
    }

    private static boolean isSet(String value) {
        return value != null && !value.isBlank();
    }

    private record SendMessageRequest(String messaging_product, String to, String type, TextBody text) {
        SendMessageRequest(String to, TextBody text) {
            this("whatsapp", to, "text", text);
        }
    }

    private record TextBody(String body) {
    }

    private record SendMessageResponse(List<MessageId> messages) {
    }

    private record MessageId(String id) {
    }
}
