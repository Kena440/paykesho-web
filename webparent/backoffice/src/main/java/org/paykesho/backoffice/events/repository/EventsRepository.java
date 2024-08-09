package org.paykesho.backoffice.events.repository;

import org.commons.entity.announcement.Announcement;
import org.paykesho.backoffice.paging.SearchRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EventsRepository extends SearchRepository<Announcement, Integer> {

    @Query("select e from Announcement e where e.name = :name")
    public Announcement getEventByName(@Param("name") String name);

    public Long countById(Integer id);

    @Query("select e from Announcement e where concat(e.name, ' ', e.details) like %?1%")
    public Page<Announcement> findAll(String keyword, Pageable pageable);

    @Query("update Announcement e set e.enabled = ?2 where e.id = ?1")
    @Modifying
    public void updateEnableStatus(Integer id, boolean enabled);

    //@Query(value = "SELECT e FROM Event e WHERE DATEDIFF(e.eventDate, CURRENT_DATE) <= :days")
    @Query("SELECT e FROM Announcement e WHERE e.eventDate > CURRENT_DATE AND DATEDIFF(e.eventDate, CURRENT_DATE) <= :days")
    public List<Announcement> findUpcomingEvents(@Param("days") int days);
}
