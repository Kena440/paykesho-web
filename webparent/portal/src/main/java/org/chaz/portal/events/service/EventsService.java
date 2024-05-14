package org.chaz.portal.events.service;

import org.chaz.portal.events.repository.EventsRepository;
import org.chaz.portal.paging.PagingAndSortingHelper;
import org.commons.entity.event.Event;
import org.commons.utils.AssessmentToolError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;
import java.util.NoSuchElementException;

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
    public List<Event> listAll() {
        return (List<Event>) eventsRepository.findAll(Sort.by("name").ascending());
    }

    public void listByPage(int pageNum, PagingAndSortingHelper helper) throws AssessmentToolError {
        helper.listEntities(pageNum, EVENTS_PER_PAGE, eventsRepository);
    }

    /**
     * +
     * This method lists all Stores in the system by pagination
     *
     * @return
     */
    public Page<Event> listByPage(int pageNumber, String sortField, String sortDir, String keyword) {
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
     * @throws AssessmentToolError
     */
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


    /**
     * +
     * This method returns a user based on the email
     *
     * @param name
     * @return
     * @throws AssessmentToolError
     */
    public Event getEventByName(String name) throws AssessmentToolError {
        if (name == null) {
            throw new AssessmentToolError("Null parameter storeName");
        }

        try {
            return eventsRepository.getEventByName(name);
        } catch (NoSuchElementException exception) {
            throw new AssessmentToolError("Could not find any Event");
        }
    }

}
