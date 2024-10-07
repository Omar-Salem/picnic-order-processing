package tech.picnic.assignment.api;

import reactor.core.publisher.Flux;
import tech.picnic.assignment.impl.model.OrderRequest;

import java.io.InputStream;
import java.time.Duration;

public interface InputStreamProcessor {

    Flux<OrderRequest> convertStream(InputStream source, int maxOrders, Duration maxTime);
}
