package org.chaz.portal.events.repository;

import org.chaz.portal.paging.SearchRepository;
import org.commons.entity.event.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface EventsRepository extends SearchRepository<Event, Integer> {
    @Query("select e from Event e where e.enabled = true order by e.name desc")
    public List<Event> findAllEnabledEvents();

    @Query("select e from Event e where concat(e.name, ' ', e.details) like %?1%")
    public Page<Event> findAll(String keyword, Pageable pageable);

    @Query("select e from Event e where e.enabled = true and e.name = :name")
    Event getEventByName(String name);

}
