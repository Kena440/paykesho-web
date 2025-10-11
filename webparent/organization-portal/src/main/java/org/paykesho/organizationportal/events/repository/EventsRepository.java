package org.paykesho.organizationportal.events.repository;

import org.commons.entity.announcement.Announcement;
import org.paykesho.organizationportal.paging.SearchRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EventsRepository extends SearchRepository<Announcement, Integer> {
    @Query("select e from Announcement e where e.enabled = true order by e.name desc")
    public List<Announcement> findAllEnabledEvents();

    @Query("select e from Announcement e where concat(e.name, ' ', e.details) like %?1%")
    public Page<Announcement> findAll(String keyword, Pageable pageable);

    @Query("select e from Announcement e where e.enabled = true and e.name = :name")
    Announcement getEventByName(String name);

    @Query("SELECT e FROM Announcement e WHERE e.eventDate > CURRENT_DATE AND DATEDIFF(e.eventDate, CURRENT_DATE) <= :days")
    public List<Announcement> findUpcomingEvents(@Param("days") int days);

    // UUID-based methods
    Optional<Announcement> findByUuid(String uuid);
    
    @Query("SELECT COUNT(a) > 0 FROM Announcement a WHERE a.uuid = ?1")
    boolean existsByUuid(String uuid);

}
