package org.chaz.backoffice.facilities.service;

import org.chaz.membershipbackoffice.memberinstitution.dto.DataItem;
import org.chaz.membershipbackoffice.memberinstitution.dto.GPSDataItem;
import org.chaz.membershipbackoffice.memberinstitution.repository.MemberInstitutionRepository;
import org.chaz.membershipbackoffice.paging.PagingAndSortingHelper;
import org.membershipcommons.dto.GeneralStatsDto;
import org.membershipcommons.entity.members.MemberInstitution;
import org.membershipcommons.enums.MemberType;
import org.membershipcommons.utils.MembershipMgtError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

@Service
@Transactional
public class MemberInstitutionService {

    public static final int MEMBERS_PER_PAGE = 20;
    protected DateFormat dateFormat;
    @Autowired
    private MemberInstitutionRepository memberInstitutionRepository;

    public List<MemberInstitution> listAll() {
        return (List<MemberInstitution>) memberInstitutionRepository.findAll(Sort.by("name").ascending());
    }

    public List<MemberInstitution> findMemberInstitutionsByLevelOfCareIdAndEnabled(Integer id, boolean enabled) {
        return memberInstitutionRepository.findMemberInstitutionsByLevelOfCareIdAndEnabled(id, enabled);
    }

    public void listByPage(int pageNum, PagingAndSortingHelper helper) throws MembershipMgtError {
        helper.listEntities(pageNum, MEMBERS_PER_PAGE, memberInstitutionRepository);
    }

    @Transactional(rollbackOn = {Exception.class})
    public MemberInstitution save(MemberInstitution memberInstitution) {
        if (memberInstitution == null) {
            return null;
        }

        return memberInstitutionRepository.save(memberInstitution);
    }

    public boolean isEmailUnique(Integer id, String email) {
        if (email == null) {
            return false;
        }
        MemberInstitution institution = memberInstitutionRepository.getMemberInstitutionByEmail(email);

        if (institution == null) return true;
        boolean isCreatingNew = (id == null);

        if (isCreatingNew) {
            if (institution != null) return false;
        } else {
            if (institution.getId() != id) {
                return false;
            }
        }

        return true;
    }

    public boolean isNameUnique(Integer id, String email) {
        if (email == null) {
            return false;
        }
        MemberInstitution institution = memberInstitutionRepository.getMemberInstitutionByName(email);

        if (institution == null) return true;
        boolean isCreatingNew = (id == null);

        if (isCreatingNew) {
            if (institution != null) return false;
        } else {
            if (institution.getId() != id) {
                return false;
            }
        }

        return true;
    }

    public MemberInstitution getMemberInstitutionById(Integer id) throws MembershipMgtError {
        if (id == null) {
            throw new MembershipMgtError("Null parameter id");
        }

        try {
            return memberInstitutionRepository.findById(id).get();
        } catch (NoSuchElementException exception) {
            throw new MembershipMgtError("Could not find any member");
        }
    }

    @Transactional(rollbackOn = {Exception.class})
    public void delete(Integer id) throws MembershipMgtError {
        if (id == null) {
            throw new MembershipMgtError("Null parameter id");
        }

        Long countById = memberInstitutionRepository.countById(id);
        if (countById == null || countById == 0) {
            throw new MembershipMgtError("Could not find any member");
        }

        MemberInstitution memberInstitution = getMemberInstitutionById(id);
        if (memberInstitution.getId() == null) throw new MembershipMgtError("Could not find any driver");
        memberInstitution.setDeleted(true);
        save(memberInstitution);
        // userRepository.deleteById(id);
    }

    @Transactional(rollbackOn = {Exception.class})
    public void updateMemberInstitutionStatus(Integer id, boolean enabled) throws MembershipMgtError {
        if (id == null) {
            throw new MembershipMgtError("Null parameter id");
        }

        Long countById = memberInstitutionRepository.countById(id);
        if (countById == null || countById == 0) {
            throw new MembershipMgtError("Could not find any member");
        }

        memberInstitutionRepository.updateEnableStatus(id, enabled);
    }

    public MemberInstitution getMemberInstitutionByEmail(String email) throws MembershipMgtError {
        if (email == null) {
            throw new MembershipMgtError("Null parameter email");
        }

        try {
            return memberInstitutionRepository.getMemberInstitutionByEmail(email);
        } catch (NoSuchElementException exception) {
            throw new MembershipMgtError("Could not find any user");
        }
    }

    public boolean isMemberUnique(Integer id, Integer provinceId, String memberName, String memberType) {
        if (provinceId == null || memberName == null || memberType == null) {
            return false;
        }
        MemberInstitution institution = memberInstitutionRepository.getMemberInstitutionByIdAndProvinceAndNameAndMemberType(id, provinceId, memberName, MemberType.valueOf(memberType));

        if (institution == null) return true;
        boolean isCreatingNew = (id == null);

        if (isCreatingNew) {
            return false;
        } else {
            return Objects.equals(institution.getId(), id);
        }
    }

    public List<DataItem> getMemberTypesDataEnabled(boolean enabled) {
        List<MemberInstitution> memberInstitutions = memberInstitutionRepository.findMemberTypesEnabled(enabled);

        List<DataItem> dataItemList = new ArrayList<>();

        for (MemberInstitution detail : memberInstitutions) {
            String identifier = detail.getMemberType().defaultDescription();

            DataItem dataItem = new DataItem(identifier);

            int itemIndex = dataItemList.indexOf(dataItem);
            if (itemIndex >= 0) {
                dataItem = dataItemList.get(itemIndex);
                dataItem.increaseItemCount(1);
            } else {
                dataItemList.add(new DataItem(identifier, 1));
            }
        }

        return dataItemList;
    }

    public Long countInstitutions() {
        return memberInstitutionRepository.count();
    }

    public Long getTotalCatchmentMembers(boolean enabled) {
        return memberInstitutionRepository.getTotalCatchmentMembers(enabled);
    }

    public List<GPSDataItem> getMemberGPSDataEnabled(boolean enabled) {
        List<MemberInstitution> memberInstitutions = memberInstitutionRepository.findMemberTypesEnabled(enabled);

        List<GPSDataItem> dataItemList = new ArrayList<>();

        for (MemberInstitution detail : memberInstitutions) {
            String title = detail.getName();
            float lng = detail.getLongitude();
            float lat = detail.getLatitude();

            dataItemList.add(new GPSDataItem(title, lng, lat));
        }

        return dataItemList;
    }

    public Page<MemberInstitution> listForMemberByPage(int pageNum, String sortField, String sortDir, String keyword) {
        Sort sort = Sort.by(sortField);
        sort = sortDir.equals("asc") ? sort.ascending() : sort.descending();

        Pageable pageable = PageRequest.of(pageNum - 1, 200, sort);

        if (keyword != null) {
            return memberInstitutionRepository.findAll(keyword, pageable);
        }
        return memberInstitutionRepository.findAll(pageable);
    }

    public List<GeneralStatsDto> getMembersCountByProvince(Integer countryId, boolean enabled) {
        return memberInstitutionRepository.getMembersCountByProvince(countryId, enabled);
    }

    public List<GeneralStatsDto> getMembersCountByDenomination(Integer countryId, boolean enabled) {
        return memberInstitutionRepository.getMembersCountByDenomination(countryId, enabled);
    }

    public List<GeneralStatsDto> getMembersCountByLevelOfCare(Integer countryId, boolean enabled) {
        return memberInstitutionRepository.getMembersCountByLevelOfCare(countryId, enabled);
    }

}
