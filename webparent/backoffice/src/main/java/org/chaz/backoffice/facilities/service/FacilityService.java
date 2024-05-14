package org.chaz.backoffice.facilities.service;

import org.chaz.backoffice.facilities.dto.DataItem;
import org.chaz.backoffice.facilities.dto.GPSDataItem;
import org.chaz.backoffice.facilities.repository.FacilityRepository;
import org.chaz.backoffice.paging.PagingAndSortingHelper;
import org.chaz.backoffice.settings.service.SettingService;
import org.commons.dto.GeneralStatsDto;
import org.commons.entity.facilities.Facility;
import org.commons.entity.facilities.LevelOfCare;
import org.commons.utils.AssessmentToolError;
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
public class FacilityService {

    public static final int MEMBERS_PER_PAGE = 20;
    protected DateFormat dateFormat;
    @Autowired
    private FacilityRepository facilityRepository;
    @Autowired
    private SettingService settingService;

    public List<Facility> listAll() {
        return (List<Facility>) facilityRepository.findAll(Sort.by("name").ascending());
    }

    public List<Facility> findFacilitysByLevelOfCareIdAndEnabled(Integer id, boolean enabled) {
        return facilityRepository.findFacilitysByLevelOfCareIdAndEnabled(id, enabled);
    }

    public void listByPage(int pageNum, PagingAndSortingHelper helper) throws AssessmentToolError {
        helper.listEntities(pageNum, MEMBERS_PER_PAGE, facilityRepository);
    }

    @Transactional(rollbackOn = {Exception.class})
    public Facility save(Facility memberInstitution) {
        if (memberInstitution == null) {
            return null;
        }

        return facilityRepository.save(memberInstitution);
    }

    public boolean isEmailUnique(Integer id, String email) {
        if (email == null) {
            return false;
        }
        Facility institution = facilityRepository.getFacilityByEmail(email);

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
        Facility institution = facilityRepository.getFacilityByName(email);

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

    public Facility getFacilityById(Integer id) throws AssessmentToolError {
        if (id == null) {
            throw new AssessmentToolError("Null parameter id");
        }

        try {
            return facilityRepository.findById(id).get();
        } catch (NoSuchElementException exception) {
            throw new AssessmentToolError("Could not find any member");
        }
    }

    @Transactional(rollbackOn = {Exception.class})
    public void delete(Integer id) throws AssessmentToolError {
        if (id == null) {
            throw new AssessmentToolError("Null parameter id");
        }

        Long countById = facilityRepository.countById(id);
        if (countById == null || countById == 0) {
            throw new AssessmentToolError("Could not find any member");
        }

        Facility memberInstitution = getFacilityById(id);
        if (memberInstitution.getId() == null) throw new AssessmentToolError("Could not find any facility");
        memberInstitution.setDeleted(true);
        save(memberInstitution);
        // userRepository.deleteById(id);
    }

    @Transactional(rollbackOn = {Exception.class})
    public void updateFacilityStatus(Integer id, boolean enabled) throws AssessmentToolError {
        if (id == null) {
            throw new AssessmentToolError("Null parameter id");
        }

        Long countById = facilityRepository.countById(id);
        if (countById == null || countById == 0) {
            throw new AssessmentToolError("Could not find any member");
        }

        facilityRepository.updateEnableStatus(id, enabled);
    }

    public Facility getFacilityByEmail(String email) throws AssessmentToolError {
        if (email == null) {
            throw new AssessmentToolError("Null parameter email");
        }

        try {
            return facilityRepository.getFacilityByEmail(email);
        } catch (NoSuchElementException exception) {
            throw new AssessmentToolError("Could not find any user");
        }
    }

    public boolean isFacilityUnique(Integer id, Integer provinceId, String facilityName, Integer levelOfCareId) {
        if (provinceId == null || facilityName == null || levelOfCareId == null) {
            return false;
        }
        Facility institution = facilityRepository.getFacilityByIdAndProvinceAndNameAndLevelOfCare(id, provinceId, facilityName, levelOfCareId);

        if (institution == null) return true;
        boolean isCreatingNew = (id == null);

        if (isCreatingNew) {
            return false;
        } else {
            return Objects.equals(institution.getId(), id);
        }
    }

    public List<DataItem> getFacilityTypesDataEnabled(boolean enabled) {
        List<Facility> memberInstitutions = facilityRepository.findFacilityTypesEnabled(enabled);

        List<DataItem> dataItemList = new ArrayList<>();

        for (Facility detail : memberInstitutions) {
            LevelOfCare levelOfCare = settingService.findLevelOfCareById(detail.getLevelOfCare().getId());
            String identifier = levelOfCare.getName();

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
        return facilityRepository.count();
    }

    public List<GPSDataItem> getFacilityGPSDataEnabled(boolean enabled) {
        List<Facility> memberInstitutions = facilityRepository.findFacilityTypesEnabled(enabled);

        List<GPSDataItem> dataItemList = new ArrayList<>();

        for (Facility detail : memberInstitutions) {
            String title = detail.getName();
            float lng = detail.getLongitude();
            float lat = detail.getLatitude();

            dataItemList.add(new GPSDataItem(title, lng, lat));
        }

        return dataItemList;
    }

    public Page<Facility> listForFacilityByPage(int pageNum, String sortField, String sortDir, String keyword) {
        Sort sort = Sort.by(sortField);
        sort = sortDir.equals("asc") ? sort.ascending() : sort.descending();

        Pageable pageable = PageRequest.of(pageNum - 1, 200, sort);

        if (keyword != null) {
            return facilityRepository.findAll(keyword, pageable);
        }
        return facilityRepository.findAll(pageable);
    }

    public List<GeneralStatsDto> getFacilitysCountByProvince(Integer countryId, boolean enabled) {
        return facilityRepository.getFacilitysCountByProvince(countryId, enabled);
    }

    public List<GeneralStatsDto> getFacilitysCountByLevelOfCare(Integer countryId, boolean enabled) {
        return facilityRepository.getFacilitysCountByLevelOfCare(countryId, enabled);
    }

}
