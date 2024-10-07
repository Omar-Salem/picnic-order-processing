package tech.picnic.assignment.impl.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum DeliveryStatus {
    @JsonProperty("delivered") DELIVERED,
    @JsonProperty("cancelled") CANCELLED
}
