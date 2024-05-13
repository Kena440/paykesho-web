package org.chaz.membershipsite.events.service;

import org.chaz.membershipsite.events.repository.EventsAccomodationOptionsRepository;
import org.chaz.membershipsite.events.repository.EventsAttendeesRepository;
import org.chaz.membershipsite.events.repository.EventsRepository;
import org.chaz.membershipsite.paging.PagingAndSortingHelper;
import org.membershipcommons.entity.event.Event;
import org.membershipcommons.entity.event.EventAccomodationOption;
import org.membershipcommons.entity.event.EventAttendeeRegistration;
import org.membershipcommons.entity.members.MemberInstitution;
import org.membershipcommons.utils.MembershipMgtError;
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
    @Autowired
    private EventsAttendeesRepository eventsAttendeesRepository;
    @Autowired
    private EventsAccomodationOptionsRepository eventsAccomodationOptionsRepository;

    /**
     * +
     * This method lists all Campuses in the system
     *
     * @return
     */
    public List<Event> listAll() {
        return (List<Event>) eventsRepository.findAll(Sort.by("name").ascending());
    }

    public void listByPage(int pageNum, PagingAndSortingHelper helper) throws MembershipMgtError {
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
     * @throws MembershipMgtError
     */
    public Event getEventById(Integer id) throws MembershipMgtError {
        if (id == null) {
            throw new MembershipMgtError("Null parameter id");
        }

        try {
            return eventsRepository.findById(id).get();
        } catch (NoSuchElementException exception) {
            throw new MembershipMgtError("Could not find any Event");
        }
    }


    /**
     * +
     * This method returns a user based on the email
     *
     * @param name
     * @return
     * @throws MembershipMgtError
     */
    public Event getEventByName(String name) throws MembershipMgtError {
        if (name == null) {
            throw new MembershipMgtError("Null parameter storeName");
        }

        try {
            return eventsRepository.getEventByName(name);
        } catch (NoSuchElementException exception) {
            throw new MembershipMgtError("Could not find any Event");
        }
    }

    public List<EventAttendeeRegistration> getEventAttendeesByMemberInstitution(MemberInstitution memberInstitution, Event event) throws MembershipMgtError {
        if (memberInstitution == null) {
            throw new MembershipMgtError("Null parameter member Institution");
        }

        return eventsAttendeesRepository.findByMemberAndEvent(memberInstitution.getId(), event.getId());
    }

    public EventAttendeeRegistration saveAttendee(EventAttendeeRegistration eventAttendeeRegistration) throws MembershipMgtError {
        if (eventAttendeeRegistration == null) {
            throw new MembershipMgtError("Null parameter eventAttendeeRegistration");
        }

        return eventsAttendeesRepository.save(eventAttendeeRegistration);
    }

    public List<EventAccomodationOption> getEventAccomodationOptions() {
        return eventsAccomodationOptionsRepository.getEventAccomodationOptions();
    }

    public EventAccomodationOption getEventAccomodationOptionById(Integer accomodationOption) {
        return eventsAccomodationOptionsRepository.findById(accomodationOption).get();
    }
}
