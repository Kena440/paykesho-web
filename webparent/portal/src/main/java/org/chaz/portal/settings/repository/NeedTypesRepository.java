package org.chaz.membershipsite.settings.repository;

import org.membershipcommons.entity.settings.NeedType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

public interface NeedTypesRepository extends PagingAndSortingRepository<NeedType, Integer> {
    public List<NeedType> findAllByOrderByNameDesc();

    @Query("select c from NeedType c where c.parent.id is NULL")
    public List<NeedType> findRootNeedTypes(Sort sort);

    @Query("select c from NeedType c where c.parent.id is NULL")
    public Page<NeedType> findRootNeedTypes(Pageable pageable);

    @Query("select c from NeedType c where c.name like %?1%")
    public Page<NeedType> search(String keyword, Pageable pageable);

    public NeedType findByName(String name);

    public NeedType findByAlias(String alias);

    public Long countById(Integer id);

    @Query("update NeedType s set s.enabled = ?2 where s.id = ?1")
    @Modifying
    public void updateEnableStatus(Integer id, boolean enabled);
}
