package org.paykesho.clientportal.events.service;

import lombok.extern.slf4j.Slf4j;
import org.commons.entity.announcement.Announcement;
import org.commons.utils.PaykeshoError;
import org.paykesho.clientportal.events.repository.EventsRepository;
import org.paykesho.clientportal.paging.PagingAndSortingHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Slf4j
@Service
@Transactional
public class EventsService {

    private static final int EVENTS_PER_PAGE = 10;
    @Autowired
    private EventsRepository eventsRepository;

    /**
     * +
     * This method lists all Campuses in the system
     *
     * @return
     */
    public List<Announcement> listAll() {
        try {
            log.debug("Retrieving all events");
            List<Announcement> events = (List<Announcement>) eventsRepository.findAll(Sort.by("name").ascending());
            log.debug("Successfully retrieved {} events", events.size());
            return events;
        } catch (Exception ex) {
            log.error("Error retrieving all events: {}", ex.getMessage(), ex);
            throw new RuntimeException("Failed to retrieve all events", ex);
        }
    }

    public void listByPage(int pageNum, PagingAndSortingHelper helper) throws PaykeshoError {
        helper.listEntities(pageNum, EVENTS_PER_PAGE, eventsRepository);
    }

    /**
     * +
     * This method lists all Stores in the system by pagination
     *
     * @return
     */
    public Page<Announcement> listByPage(int pageNumber, String sortField, String sortDir, String keyword) {
        if (pageNumber == 0) {
            return null;
        }
        Sort sort = Sort.by(sortField);
        sort = sortDir.equals("asc") ? sort.ascending() : sort.descending();
        Pageable pageable = PageRequest.of(pageNumber - 1, EVENTS_PER_PAGE, sort);

        if (keyword != null) {
            return eventsRepository.findAll(keyword, pageable);
        }
        return eventsRepository.findAll(pageable);
    }


    /**
     * +
     * This method returns a campus based on the ID
     *
     * @param id
     * @return
     * @throws PaykeshoError
     */
    public Announcement getEventById(Integer id) throws PaykeshoError {
        if (id == null) {
            throw new PaykeshoError("Null parameter id");
        }

        try {
            return eventsRepository.findById(id).get();
        } catch (NoSuchElementException exception) {
            throw new PaykeshoError("Could not find any Event");
        }
    }


    // UUID-based method
    public Announcement getEventByUuid(String uuid) throws PaykeshoError {
        if (uuid == null || uuid.trim().isEmpty()) {
            throw new PaykeshoError("Invalid UUID parameter");
        }

        try {
            UUID.fromString(uuid);
        } catch (IllegalArgumentException e) {
            throw new PaykeshoError("Invalid UUID format");
        }

        Announcement announcement = eventsRepository.findByUuid(uuid)
                .orElseThrow(() -> new PaykeshoError("Could not find event with UUID: " + uuid));

        // Ensure UUID is set (in case it was null in database)
        if (announcement.getUuid() == null || announcement.getUuid().trim().isEmpty()) {
            log.warn("Event {} has null or empty UUID, generating new one", announcement.getId());
            announcement.ensureUuid();
            eventsRepository.save(announcement);
        }

        return announcement;
    }

    /**
     * +
     * This method returns a user based on the email
     *
     * @param name
     * @return
     * @throws PaykeshoError
     */
    public Announcement getEventByName(String name) throws PaykeshoError {
        if (name == null) {
            throw new PaykeshoError("Null parameter storeName");
        }

        try {
            return eventsRepository.getEventByName(name);
        } catch (NoSuchElementException exception) {
            throw new PaykeshoError("Could not find any Event");
        }
    }

    public List<Announcement> getUpcomingEvents(int i) {
        try {
            log.debug("Retrieving upcoming events with limit: {}", i);
            List<Announcement> events;
            if (i > 0) {
                events = eventsRepository.findUpcomingEvents(i);
            } else {
                events = eventsRepository.findUpcomingEvents(30);
            }
            log.debug("Successfully retrieved {} upcoming events", events.size());
            return events;
        } catch (Exception ex) {
            log.error("Error retrieving upcoming events with limit {}: {}", i, ex.getMessage(), ex);
            // Return empty list on error to prevent dashboard failures
            return List.of();
        }
    }

}
