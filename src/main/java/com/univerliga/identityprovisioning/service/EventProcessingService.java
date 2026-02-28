package com.univerliga.identityprovisioning.service;

import com.univerliga.identityprovisioning.domain.ProcessedEvent;
import com.univerliga.identityprovisioning.domain.ProcessedEventStatus;
import com.univerliga.identityprovisioning.dto.MockEventRequest;
import com.univerliga.identityprovisioning.dto.ProcessedEventDto;
import com.univerliga.identityprovisioning.exception.NotFoundException;
import com.univerliga.identityprovisioning.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventProcessingService {

    private final ProcessedEventRepository processedEventRepository;
    private final ProvisioningService provisioningService;

    @Transactional
    public ProcessedEventStatus process(MockEventRequest event) {
        Optional<ProcessedEvent> existing = processedEventRepository.findById(event.eventId());
        if (existing.isPresent() && existing.get().getStatus() == ProcessedEventStatus.PROCESSED) {
            return ProcessedEventStatus.IGNORED;
        }

        ProcessedEvent record = existing.orElseGet(ProcessedEvent::new);
        record.setEventId(event.eventId());
        record.setType(event.type());

        try {
            switch (event.type()) {
                case PersonCreated, PersonUpdated -> provisioningService.upsertFromEvent(
                    event.payload().personId(),
                    event.payload().username(),
                    event.payload().email(),
                    event.payload().displayName(),
                    event.payload().roles(),
                    event.payload().enabled()
                );
                case PersonDeactivated -> provisioningService.toggle(event.payload().personId(), false);
            }
            record.setStatus(ProcessedEventStatus.PROCESSED);
            record.setError(null);
        } catch (Exception ex) {
            record.setStatus(ProcessedEventStatus.FAILED);
            record.setError(ex.getMessage());
            provisioningService.markFailed(event.payload().personId(), ex.getMessage());
            throw ex;
        } finally {
            record.setAttemptCount(record.getAttemptCount() + 1);
            processedEventRepository.save(record);
        }

        return ProcessedEventStatus.PROCESSED;
    }

    @Transactional(readOnly = true)
    public ProcessedEventDto get(UUID eventId) {
        ProcessedEvent event = processedEventRepository.findById(eventId)
            .orElseThrow(() -> new NotFoundException("Processed event not found: " + eventId));

        return new ProcessedEventDto(
            event.getEventId().toString(),
            event.getType().name(),
            event.getStatus().name(),
            event.getProcessedAt(),
            event.getError()
        );
    }
}
