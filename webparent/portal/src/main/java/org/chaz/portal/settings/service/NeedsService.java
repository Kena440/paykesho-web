package org.chaz.membershipsite.settings.service;

import org.chaz.membershipsite.settings.repository.NeedTypesRepository;
import org.membershipcommons.entity.settings.NeedType;
import org.membershipcommons.utils.MembershipMgtError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.*;

@Service
@Transactional
public class NeedsService {
    public static final int ROOT_NEEDS_PER_PAGE = 1;
    @Autowired
    private NeedTypesRepository needTypesRepository;

    /**
     * +
     * This method lists all NeedTypes in the system
     *
     * @return
     */
    public List<NeedType> listAll() {
        return (List<NeedType>) needTypesRepository.findAll(Sort.by("name").descending());
    }

    /**
     * +
     * This method category store submitted from the category
     *
     * @param category
     */
    public NeedType save(NeedType category) {
        if (category == null) {
            return null;
        }

        NeedType parent = category.getParent();
        if (parent != null) {
            String allParentIds = parent.getAllParentIds() == null ? "-" : parent.getAllParentIds();
            allParentIds += String.valueOf(parent.getId()) + "-";
            category.setAllParentIds(allParentIds);
        }
        return needTypesRepository.save(category);
    }

    public NeedType get(Integer id) throws MembershipMgtError {
        try {
            return needTypesRepository.findById(id).get();
        } catch (NoSuchElementException ex) {
            throw new MembershipMgtError("no category");
        }
    }


    private Set<NeedType> sortSubNeedTypes(Set<NeedType> children) {
        return sortSubNeedTypes(children, "asc");
    }

    private SortedSet<NeedType> sortSubNeedTypes(Set<NeedType> children, String sortDir) {
        SortedSet<NeedType> sortedChildrenSet = new TreeSet<>(new Comparator<NeedType>() {
            @Override
            public int compare(NeedType o1, NeedType o2) {
                if (sortDir.equals("asc")) {
                    return o1.getName().compareTo(o2.getName());
                } else {
                    return o2.getName().compareTo(o1.getName());
                }
            }
        });

        sortedChildrenSet.addAll(children);
        return sortedChildrenSet;
    }

    /**
     * +
     * This method updates the status of a category i.e. enabled or disabled
     *
     * @param id
     * @param enabled
     * @throws MembershipMgtError
     */
    public void updateNeedTypeStatus(Integer id, boolean enabled) throws MembershipMgtError {
        if (id == null) {
            throw new MembershipMgtError("Null parameter id");
        }

        Long countById = needTypesRepository.countById(id);
        if (countById == null || countById == 0) {
            throw new MembershipMgtError("Could not find any category");
        }

        needTypesRepository.updateEnableStatus(id, enabled);
    }

    /**
     * +
     * This method deletes a Store based on ID produced
     *
     * @param id
     * @throws MembershipMgtError
     */
    public void delete(Integer id) throws MembershipMgtError {
        if (id == null) {
            throw new MembershipMgtError("Null parameter id");
        }

        Long countById = needTypesRepository.countById(id);
        if (countById == null || countById == 0) {
            throw new MembershipMgtError("Could not find any NeedType");
        }

        needTypesRepository.deleteById(id);
    }
}
