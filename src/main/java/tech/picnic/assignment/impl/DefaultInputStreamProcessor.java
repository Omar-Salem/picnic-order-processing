package tech.picnic.assignment.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import reactor.core.publisher.Flux;
import tech.picnic.assignment.api.InputStreamProcessor;
import tech.picnic.assignment.impl.exceptions.InvalidFormatException;
import tech.picnic.assignment.impl.model.OrderRequest;

import java.io.*;
import java.time.Duration;
import java.util.logging.Logger;

import static tech.picnic.assignment.impl.ObjectMapperFactory.createObjectMapper;

public class DefaultInputStreamProcessor implements InputStreamProcessor {
    private final ObjectMapper objectMapper = createObjectMapper();
    private final Logger log = Logger.getLogger(DefaultInputStreamProcessor.class.getName());

    @Override
    public Flux<OrderRequest> convertStream(InputStream source, int maxOrders, Duration maxTime) {
        return Flux
                .fromStream(new BufferedReader(new InputStreamReader(source)).lines())
                .filter(line -> !Strings.isNullOrEmpty(line))
                .map(line -> {
                    try {
                        return objectMapper.readValue(line, OrderRequest.class);
                    } catch (JsonProcessingException e) {
                        throw new InvalidFormatException(e);
                    }
                })
                .onErrorContinue((ex, o) -> log.warning(ex.toString()))
                .take(maxOrders)
                .take(maxTime);
    }
}
