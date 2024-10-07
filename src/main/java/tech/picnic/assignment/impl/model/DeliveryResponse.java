package tech.picnic.assignment.impl.model;

import com.fasterxml.jackson.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({"delivery_id", "delivery_time", "delivery_status", "orders", "total_amount"})
public record DeliveryResponse(@JsonProperty("delivery_id") String deliveryId,
                               @JsonProperty("delivery_time") Instant deliveryTime,
                               List<OrderResponse> orders) implements Comparable<DeliveryResponse> {
    @JsonProperty("total_amount")
    public BigDecimal getTotalAmount() {
        return orders
                .stream()
                .filter(OrderResponse::isDelivered)
                .map(OrderResponse::amount)
                .reduce(BigDecimal::add)
                .orElse(null);
    }

    @JsonProperty("delivery_status")
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
    public DeliveryStatus getDeliveryStatus() {
        final boolean anyOrderDelivered = orders
                .stream()
                .anyMatch(OrderResponse::isDelivered);
        return anyOrderDelivered ? DeliveryStatus.DELIVERED : DeliveryStatus.CANCELLED;
    }

    @JsonProperty("orders")
    public List<OrderResponse> getOrders() {
        return orders
                .stream()
                .sorted()
                .toList();
    }

    @Override
    public int compareTo(DeliveryResponse o) {
        final int deliveryTimeComparison = this.deliveryTime.compareTo(o.deliveryTime);
        if (deliveryTimeComparison != 0) {
            return deliveryTimeComparison;
        }
        return this.deliveryId.compareTo(o.deliveryId);
    }
}
