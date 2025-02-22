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
        return (List<Announcement>) eventsRepository.findAll(Sort.by("name").ascending());
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
        if (i > 0) {
            return eventsRepository.findUpcomingEvents(i);
        } else {
            return eventsRepository.findUpcomingEvents(30);
        }
    }

}
