package tech.picnic.assignment.impl.model;

import com.fasterxml.jackson.annotation.*;

import java.math.BigDecimal;

public record OrderResponse(@JsonProperty("order_id") String orderId,
                            BigDecimal amount,
                            @JsonIgnore OrderStatus orderStatus) implements Comparable<OrderResponse> {
    @Override
    public int compareTo(OrderResponse o) {
        return o.orderId.compareTo(this.orderId);
    }

    @JsonIgnore
    public boolean isDelivered() {
        return OrderStatus.DELIVERED.equals(orderStatus());
    }
}
