package com.univerliga.identityprovisioning.service;

import com.univerliga.identityprovisioning.config.AppProperties;
import com.univerliga.identityprovisioning.domain.EventType;
import com.univerliga.identityprovisioning.domain.ProcessedEvent;
import com.univerliga.identityprovisioning.domain.ProcessedEventStatus;
import com.univerliga.identityprovisioning.dto.CrmPayload;
import com.univerliga.identityprovisioning.dto.MockEventRequest;
import com.univerliga.identityprovisioning.repository.ProcessedEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventProcessingServiceTest {

    @Mock
    private ProcessedEventRepository processedEventRepository;
    @Mock
    private ProvisioningService provisioningService;

    private EventProcessingService eventProcessingService;

    @BeforeEach
    void setUp() {
        AppProperties properties = new AppProperties();
        properties.getProvisioning().getBroker().setMaxFailCount(10);
        eventProcessingService = new EventProcessingService(processedEventRepository, provisioningService, properties);
    }

    @Test
    void process_shouldIgnoreAlreadyProcessedEvent() {
        UUID eventId = UUID.randomUUID();
        MockEventRequest event = event(eventId);
        ProcessedEvent existing = new ProcessedEvent();
        existing.setEventId(eventId);
        existing.setType(EventType.PersonCreated);
        existing.setStatus(ProcessedEventStatus.PROCESSED);

        when(processedEventRepository.findById(eventId)).thenReturn(Optional.of(existing));

        ProcessedEventStatus status = eventProcessingService.process(event);

        assertThat(status).isEqualTo(ProcessedEventStatus.IGNORED);
        verify(provisioningService, never()).upsertFromEvent(any(), any(), any(), any(), any(), any());
    }

    @Test
    void process_shouldMarkProcessed() {
        when(processedEventRepository.findById(any())).thenReturn(Optional.empty());
        when(processedEventRepository.save(any(ProcessedEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        ProcessedEventStatus status = eventProcessingService.process(event(UUID.randomUUID()));

        assertThat(status).isEqualTo(ProcessedEventStatus.PROCESSED);
        verify(provisioningService).upsertFromEvent(any(), any(), any(), any(), any(), any());
    }

    private MockEventRequest event(UUID eventId) {
        return new MockEventRequest(
            eventId,
            EventType.PersonCreated,
            OffsetDateTime.now(),
            "crm-service",
            new CrmPayload("p_1", "user1", "user1@example.com", "User 1", Set.of("ROLE_EMPLOYEE"), true)
        );
    }
}
