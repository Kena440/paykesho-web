package org.chaz.backoffice.events.service;

import org.chaz.backoffice.events.repository.*;
import org.chaz.backoffice.paging.PagingAndSortingHelper;
import org.commons.entity.event.*;
import org.commons.entity.facilities.Facility;
import org.commons.utils.AssessmentToolError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional
public class EventsService {

    private static final int EVENTS_PER_PAGE = 20;
    @Autowired
    private EventsRepository eventsRepository;

    /**
     * +
     * This method saves event submitted from the event
     *
     * @param event
     */
    public Event save(Event event) {
        if (event == null) {
            return null;
        }

        return eventsRepository.save(event);
    }

    /**
     * +
     * This method checks if the code submitted is unique
     *
     * @param id
     * @param name
     * @return
     */
    public boolean isEventUnique(Integer id, String name) {
        if (name == null) {
            return false;
        }
        Event event = eventsRepository.getEventByName(name);

        if (event == null) return true;
        boolean isCreatingNew = (id == null);

        if (isCreatingNew) {
            if (event != null) return false;
        } else {
            if (event.getId() != id) {
                return false;
            }
        }

        return true;
    }

    public void listByPage(int pageNum, PagingAndSortingHelper helper) throws AssessmentToolError {
        helper.listEntities(pageNum, EVENTS_PER_PAGE, eventsRepository);
    }

    public Event getEventById(Integer id) throws AssessmentToolError {
        if (id == null) {
            throw new AssessmentToolError("Null parameter id");
        }

        try {
            return eventsRepository.findById(id).get();
        } catch (NoSuchElementException exception) {
            throw new AssessmentToolError("Could not find any Event");
        }
    }

    public void delete(Integer id) throws AssessmentToolError {
        if (id == null) {
            throw new AssessmentToolError("Null parameter id");
        }

        Long countById = eventsRepository.countById(id);
        if (countById == null || countById == 0) {
            throw new AssessmentToolError("Could not find any Event");
        }

        eventsRepository.deleteById(id);
    }

    public void updateEventStatus(Integer id, boolean enabled) throws AssessmentToolError {
        if (id == null) {
            throw new AssessmentToolError("Null parameter id");
        }

        Long countById = eventsRepository.countById(id);
        if (countById == null || countById == 0) {
            throw new AssessmentToolError("Could not find any Event");
        }

        eventsRepository.updateEnableStatus(id, enabled);
    }

    public List<Event> getUpcomingEvents(int i) {
        if (i > 0) {
            return eventsRepository.findUpcomingEvents(i);
        } else {
            return eventsRepository.findUpcomingEvents(30);
        }
    }
}
