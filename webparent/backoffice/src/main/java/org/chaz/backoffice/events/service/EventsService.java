package org.chaz.backoffice.events.service;

import org.chaz.backoffice.events.repository.*;
import org.chaz.backoffice.paging.PagingAndSortingHelper;
import org.membershipcommons.entity.event.*;
import org.membershipcommons.entity.members.MemberInstitution;
import org.membershipcommons.utils.MembershipMgtError;
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
    @Autowired
    private EventsAttendeesRepository eventsAttendeesRepository;
    @Autowired
    private EventsVendorsRepository eventsVendorsRepository;
    @Autowired
    private EventsVendorPackagesRepository eventsVendorPackagesRepository;
    @Autowired
    private EventsAccomodationOptionsRepository eventsAccomodationOptionsRepository;
    @Autowired
    private EventsAttendeesAttendanceRepository eventsAttendeesAttendanceRepository;

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

    public void listByPage(int pageNum, PagingAndSortingHelper helper) throws MembershipMgtError {
        helper.listEntities(pageNum, EVENTS_PER_PAGE, eventsRepository);
    }

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

    public void delete(Integer id) throws MembershipMgtError {
        if (id == null) {
            throw new MembershipMgtError("Null parameter id");
        }

        Long countById = eventsRepository.countById(id);
        if (countById == null || countById == 0) {
            throw new MembershipMgtError("Could not find any Event");
        }

        eventsRepository.deleteById(id);
    }

    public void updateEventStatus(Integer id, boolean enabled) throws MembershipMgtError {
        if (id == null) {
            throw new MembershipMgtError("Null parameter id");
        }

        Long countById = eventsRepository.countById(id);
        if (countById == null || countById == 0) {
            throw new MembershipMgtError("Could not find any Event");
        }

        eventsRepository.updateEnableStatus(id, enabled);
    }

    public List<EventAttendeeRegistration> getEventAttendees(Event event) throws MembershipMgtError {
        if (event == null) {
            throw new MembershipMgtError("Null parameter event");
        }

        return eventsAttendeesRepository.findByEvent(event.getId());
    }

    /*public Page<EventAttendeeRegistration> getEventAttendeesByPage(int pageNum, String sortField, String sortDir, Event event) {
        Sort sort = Sort.by(sortField);
        sort = sortDir.equals("asc") ? sort.ascending() : sort.descending();

        Pageable pageable = PageRequest.of(pageNum - 1, 200, sort);

        if (event != null) {
            return eventsAttendeesRepository.findAll(event.getId(), pageable);
        }
        return eventsAttendeesRepository.findAll(pageable);
    }*/

    public List<EventAttendeeRegistration> getEventAttendeesByMemberInstitution(MemberInstitution memberInstitution, Event event) throws MembershipMgtError {
        if (memberInstitution == null) {
            throw new MembershipMgtError("Null parameter member Institution");
        }

        return eventsAttendeesRepository.findByEvent(memberInstitution.getId(), event.getId());
    }

    public EventAttendeeRegistration saveAttendee(EventAttendeeRegistration eventAttendeeRegistration) throws MembershipMgtError {
        if (eventAttendeeRegistration == null) {
            throw new MembershipMgtError("Null parameter eventAttendeeRegistration");
        }

        return eventsAttendeesRepository.save(eventAttendeeRegistration);
    }

    public EventAttendeeRegistration getEventAttendeeById(Integer id) throws MembershipMgtError {
        if (id == null) {
            throw new MembershipMgtError("Null parameter event");
        }

        return eventsAttendeesRepository.findById(id).get();
    }

    public EventVendorRegistration saveVendor(EventVendorRegistration eventVendorRegistration) throws MembershipMgtError {
        if (eventVendorRegistration == null) {
            throw new MembershipMgtError("Null parameter event");
        }

        return eventsVendorsRepository.save(eventVendorRegistration);
    }

    public EventVendorRegistration getEventVendorRegistrationById(Integer id) throws MembershipMgtError {
        if (id == null) {
            throw new MembershipMgtError("Null parameter event");
        }

        return eventsVendorsRepository.findById(id).get();
    }

    public List<EventVendorPackage> getVendorPackages() {
        return eventsVendorPackagesRepository.getEventVendorPackages();
    }

    public List<EventVendorRegistration> getEventVendors(Event event) throws MembershipMgtError {
        if (event == null) {
            throw new MembershipMgtError("Null parameter event");
        }

        return eventsVendorsRepository.findByEvent(event.getId());
    }

    public List<EventAccomodationOption> getEventAccomodationOptions() {
        return eventsAccomodationOptionsRepository.getEventAccomodationOptions();
    }

    public EventAccomodationOption getEventAccomodationOptionById(Integer accomodationOption) {
        return eventsAccomodationOptionsRepository.findById(accomodationOption).get();
    }

    public EventVendorPackage getEventVendorPackageById(Integer eventVendorPackage) {
        return eventsVendorPackagesRepository.findById(eventVendorPackage).get();
    }

    public void deleteAttendee(EventAttendeeRegistration eventAttendeeRegistration) throws MembershipMgtError {
        if (eventAttendeeRegistration == null) {
            throw new MembershipMgtError("Null parameter id");
        }

        Long countById = eventsAttendeesRepository.countById(eventAttendeeRegistration.getId());
        if (countById == null || countById == 0) {
            throw new MembershipMgtError("Could not find any Event");
        }

        eventsAttendeesRepository.deleteById(eventAttendeeRegistration.getId());
    }

    public void deleteVendor(EventVendorRegistration eventVendorRegistration) throws MembershipMgtError {
        if (eventVendorRegistration == null) {
            throw new MembershipMgtError("Null parameter id");
        }

        Long countById = eventsVendorsRepository.countById(eventVendorRegistration.getId());
        if (countById == null || countById == 0) {
            throw new MembershipMgtError("Could not find any Event");
        }

        eventsVendorsRepository.deleteById(eventVendorRegistration.getId());
    }

    public EventAttendeeRegistration getEventAttendeeByEventRegId(String barcodeData) throws MembershipMgtError {
        if (barcodeData == null) {
            throw new MembershipMgtError("Null parameter event");
        }

        return eventsAttendeesRepository.findByEventRegId(barcodeData);
    }

    public EventAttendeeAttendance saveAttendeeAttendance(EventAttendeeAttendance eventAttendeeRegistration) throws MembershipMgtError {
        if (eventAttendeeRegistration == null) {
            throw new MembershipMgtError("Null parameter eventAttendeeRegistration");
        }

        return eventsAttendeesAttendanceRepository.save(eventAttendeeRegistration);
    }

    public List<EventAttendeeAttendance> getEventAttendeeAttendance(Event event) throws MembershipMgtError {
        if (event == null) {
            throw new MembershipMgtError("Null parameter event");
        }

        return eventsAttendeesAttendanceRepository.findByEvent(event.getId());
    }

    public EventAttendeeAttendance getEventAttendeeAttendanceById(Integer id) throws MembershipMgtError {
        if (id == null) {
            throw new MembershipMgtError("Null parameter event");
        }

        return eventsAttendeesAttendanceRepository.findById(id).get();
    }

    public void deleteEventAttendeeAttendance(EventAttendeeAttendance eventAttendeeAttendance) throws MembershipMgtError {
        if (eventAttendeeAttendance == null) {
            throw new MembershipMgtError("Null parameter id");
        }

        Long countById = eventsAttendeesAttendanceRepository.countById(eventAttendeeAttendance.getId());
        if (countById == null || countById == 0) {
            throw new MembershipMgtError("Could not find any Event");
        }

        eventsAttendeesAttendanceRepository.deleteById(eventAttendeeAttendance.getId());
    }

    public List<Event> getUpcomingEvents(int i) {
        if (i > 0) {
            return eventsRepository.findUpcomingEvents(i);
        } else {
            return eventsRepository.findUpcomingEvents(30);
        }
    }
}
