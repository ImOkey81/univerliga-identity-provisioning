package com.univerliga.identityprovisioning.messaging;

import com.univerliga.identityprovisioning.config.AppProperties;
import com.univerliga.identityprovisioning.domain.EventType;
import com.univerliga.identityprovisioning.dto.MockEventRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RabbitEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final AppProperties properties;

    public void publish(MockEventRequest event) {
        rabbitTemplate.convertAndSend(
            properties.getProvisioning().getBroker().getExchange(),
            routingKey(event.type()),
            event
        );
    }

    private String routingKey(EventType type) {
        return switch (type) {
            case PersonCreated -> properties.getProvisioning().getBroker().getRoutingCreated();
            case PersonUpdated -> properties.getProvisioning().getBroker().getRoutingUpdated();
            case PersonDeactivated -> properties.getProvisioning().getBroker().getRoutingDeactivated();
        };
    }
}
