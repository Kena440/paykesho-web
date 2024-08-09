package org.paykesho.backoffice.events.service;

import org.commons.entity.announcement.Announcement;
import org.commons.utils.PaykeshoError;
import org.paykesho.backoffice.events.repository.EventsRepository;
import org.paykesho.backoffice.paging.PagingAndSortingHelper;
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
     * @param announcement
     */
    public Announcement save(Announcement announcement) {
        if (announcement == null) {
            return null;
        }

        return eventsRepository.save(announcement);
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
        Announcement announcement = eventsRepository.getEventByName(name);

        if (announcement == null) return true;
        boolean isCreatingNew = (id == null);

        if (isCreatingNew) {
            if (announcement != null) return false;
        } else {
            if (announcement.getId() != id) {
                return false;
            }
        }

        return true;
    }

    public void listByPage(int pageNum, PagingAndSortingHelper helper) throws PaykeshoError {
        helper.listEntities(pageNum, EVENTS_PER_PAGE, eventsRepository);
    }

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

    public void delete(Integer id) throws PaykeshoError {
        if (id == null) {
            throw new PaykeshoError("Null parameter id");
        }

        Long countById = eventsRepository.countById(id);
        if (countById == null || countById == 0) {
            throw new PaykeshoError("Could not find any Event");
        }

        eventsRepository.deleteById(id);
    }

    public void updateEventStatus(Integer id, boolean enabled) throws PaykeshoError {
        if (id == null) {
            throw new PaykeshoError("Null parameter id");
        }

        Long countById = eventsRepository.countById(id);
        if (countById == null || countById == 0) {
            throw new PaykeshoError("Could not find any Event");
        }

        eventsRepository.updateEnableStatus(id, enabled);
    }

    public List<Announcement> getUpcomingEvents(int i) {
        if (i > 0) {
            return eventsRepository.findUpcomingEvents(i);
        } else {
            return eventsRepository.findUpcomingEvents(30);
        }
    }
}
