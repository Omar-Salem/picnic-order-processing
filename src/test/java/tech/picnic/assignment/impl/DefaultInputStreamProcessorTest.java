package tech.picnic.assignment.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import tech.picnic.assignment.impl.model.*;

import java.io.*;
import java.math.BigDecimal;
import java.time.Duration;

class DefaultInputStreamProcessorTest {
    private final DefaultInputStreamProcessor target = new DefaultInputStreamProcessor();

    @ParameterizedTest
    @ValueSource(strings = {"happy-path-input.json-stream",
            "happy-path-input-with-empty-lines.json-stream"})
    void processing_stream_is_limited_by_size(String filePath) throws IOException {
        try (InputStream source = getClass().getResourceAsStream(filePath)) {
            //Arrange
            final int maxOrders = 1;
            final Duration maxTime = Duration.ofSeconds(30);

            //Act
            final Flux<OrderRequest> orderRequestFlux = target.convertStream(source, maxOrders, maxTime);

            //Assert
            StepVerifier.create(orderRequestFlux)
                    .expectNext(new OrderRequest("1234567890", OrderStatus.DELIVERED, new DeliveryRequest("a", "2022-05-20T11:50:48Z"), BigDecimal.TEN))
                    .expectComplete()
                    .verify();
        }
    }

    @Test
    void processing_stream_is_limited_by_time() throws IOException {
        //Arrange
        final Flux<String> fluxInterval = Flux
                .interval(Duration.ofSeconds(1))
                .map(i -> i % 2 == 0 ? "\n" : "{\"order_id\":\"1234567890\",\"order_status\":\"delivered\",\"delivery\":{\"delivery_id\":\"d923jd29j91d1gh6\",\"delivery_time\":\"2022-05-20T11:50:48Z\"},\"amount\": 6477}")
                .take(5);


        final InputStream source = convertFluxToStream(fluxInterval);

        final int maxOrders = 100;
        final Duration maxTime = Duration.ofSeconds(3);

        //Act
        final Flux<OrderRequest> orderRequestFlux = target.convertStream(source, maxOrders, maxTime);

        //Assert
        final OrderRequest orderRequest = new OrderRequest("1234567890", OrderStatus.DELIVERED, new DeliveryRequest("a", "2022-05-20T11:50:48Z"), BigDecimal.TEN);
        StepVerifier.create(orderRequestFlux)
                .expectNext(orderRequest)
                .expectComplete()
                .verify();
    }

    @Test
    void processing_stream_ignores_empty_heart_beat_lines() throws IOException {
        try (InputStream source = getClass().getResourceAsStream("happy-path-input-with-empty-lines.json-stream")) {
            //Arrange
            final int maxOrders = 100;
            final Duration maxTime = Duration.ofSeconds(30);

            //Act
            final Flux<OrderRequest> orderRequestFlux = target.convertStream(source, maxOrders, maxTime);

            //Assert
            StepVerifier.create(orderRequestFlux)
                    .expectNextCount(3)
                    .verifyComplete();
        }
    }

    @Test
    void processing_stream_ignores_faulty_entries() throws IOException {
        //Arrange
        final Flux<String> fluxInterval = Flux
                .just("{\"order_id\":\"1234567890\",\"order_status\":\"delivered\",\"delivery\":{\"delivery_id\":\"d923jd29j91d1gh6\",\"delivery_time\":\"2022-05-20T11:50:48Z\"},\"amount\": 6477}",
                        "$#^$");


        final InputStream source = convertFluxToStream(fluxInterval);

        final int maxOrders = 100;
        final Duration maxTime = Duration.ofSeconds(30);

        //Act
        final Flux<OrderRequest> orderRequestFlux = target.convertStream(source, maxOrders, maxTime);

        //Assert
        StepVerifier.create(orderRequestFlux)
                .expectNextCount(1)
                .verifyComplete();
    }

    private InputStream convertFluxToStream(Flux<String> fluxInterval) throws IOException {
        PipedOutputStream outputStream = new PipedOutputStream();
        PipedInputStream source = new PipedInputStream(outputStream);
        fluxInterval.subscribe(value -> {
            try {
                // Write emitted values to the OutputStream
                outputStream.write((value + "\n").getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, throwable -> {
            // Handle errors
            throwable.printStackTrace();
        }, () -> {
            try {
                // Close the output stream when Flux completes
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        return source;
    }
}