package org.chaz.backoffice.events.repository;

import org.chaz.backoffice.paging.SearchRepository;
import org.commons.entity.event.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EventsRepository extends SearchRepository<Event, Integer> {

    @Query("select e from Event e where e.name = :name")
    public Event getEventByName(@Param("name") String name);

    public Long countById(Integer id);

    @Query("select e from Event e where concat(e.name, ' ', e.details) like %?1%")
    public Page<Event> findAll(String keyword, Pageable pageable);

    @Query("update Event e set e.enabled = ?2 where e.id = ?1")
    @Modifying
    public void updateEnableStatus(Integer id, boolean enabled);

    //@Query(value = "SELECT e FROM Event e WHERE DATEDIFF(e.eventDate, CURRENT_DATE) <= :days")
    @Query("SELECT e FROM Event e WHERE e.eventDate > CURRENT_DATE AND DATEDIFF(e.eventDate, CURRENT_DATE) <= :days")
    public List<Event> findUpcomingEvents(@Param("days") int days);
}
