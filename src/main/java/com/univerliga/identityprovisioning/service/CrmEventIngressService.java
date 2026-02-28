package com.univerliga.identityprovisioning.service;

import com.univerliga.identityprovisioning.config.AppProperties;
import com.univerliga.identityprovisioning.domain.ProcessedEventStatus;
import com.univerliga.identityprovisioning.dto.MockEventRequest;
import com.univerliga.identityprovisioning.messaging.RabbitEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CrmEventIngressService {

    private final AppProperties properties;
    private final EventProcessingService eventProcessingService;
    private final RabbitEventPublisher rabbitEventPublisher;

    public ProcessedEventStatus handleEvent(MockEventRequest event) {
        if ("broker".equalsIgnoreCase(properties.getProvisioning().getMode())) {
            rabbitEventPublisher.publish(event);
            return ProcessedEventStatus.PENDING;
        }
        return eventProcessingService.process(event);
    }
}
