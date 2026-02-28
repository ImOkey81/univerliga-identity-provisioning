package com.univerliga.identityprovisioning.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.univerliga.identityprovisioning.config.AppProperties;
import com.univerliga.identityprovisioning.dto.MockEventRequest;
import com.univerliga.identityprovisioning.domain.ProcessedEventStatus;
import com.univerliga.identityprovisioning.service.EventProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.provisioning.mode", havingValue = "broker")
public class RabbitEventConsumer {

    private static final long[] BACKOFF_MS = {1000L, 2000L, 5000L, 10000L, 30000L};

    private final EventProcessingService eventProcessingService;
    private final AppProperties appProperties;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = "${app.provisioning.broker.queue}", containerFactory = "manualAckListenerFactory")
    public void consume(Message message, Channel channel) throws Exception {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            MockEventRequest event = objectMapper.readValue(message.getBody(), MockEventRequest.class);
            MDC.put("eventId", event.eventId().toString());
            ProcessedEventStatus result = withRetries(event);
            if (result == ProcessedEventStatus.IGNORED) {
                log.info("Event {} ignored as already processed", event.eventId());
            }
            channel.basicAck(deliveryTag, false);
        } catch (Exception ex) {
            log.error("Event failed after retries, sending to DLQ", ex);
            rabbitTemplate.convertAndSend("", appProperties.getProvisioning().getBroker().getDlq(), message.getBody());
            channel.basicAck(deliveryTag, false);
        } finally {
            MDC.remove("eventId");
        }
    }

    private ProcessedEventStatus withRetries(MockEventRequest event) {
        int maxRetries = Math.max(1, Math.min(appProperties.getProvisioning().getBroker().getMaxRetries(), BACKOFF_MS.length));
        RuntimeException last = null;

        for (int i = 0; i < maxRetries; i++) {
            try {
                return eventProcessingService.process(event);
            } catch (RuntimeException ex) {
                last = ex;
                sleep(BACKOFF_MS[i]);
            }
        }

        throw last == null ? new IllegalStateException("Unknown processing error") : last;
    }

    private void sleep(long timeoutMs) {
        try {
            Thread.sleep(timeoutMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Retry interrupted", e);
        }
    }
}
